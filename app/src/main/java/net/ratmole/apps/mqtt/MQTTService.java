package net.ratmole.apps.mqtt;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.util.Locale;


public class MQTTService extends Service implements MqttCallback
{

	private static boolean logDebug = false;


	private static String PREFS = "mqtt-prefs";
	private static String sHOSTNAME = "-Hostname";
	private static String sTOPIC = "-Topic";
	private static String sUSERNAME = "-Username";
	private static String sPASSWORD = "-Password";
	private static String sPORT = "-Port";

	private String 	MQTT_BROKER;
	private String 	MQTT_PORT;
	private String 	USERNAME;
	private String 	PASSWORD;
	private String 	TOPIC;


	public static final String 		DEBUG_TAG = "MqttService";
	private static final String		MQTT_THREAD_NAME = "MqttService[" + DEBUG_TAG + "]";

	public static final int			MQTT_QOS_0 = 0; // QOS Level 0 ( Delivery Once no confirmation )
	public static final int 		MQTT_QOS_1 = 1; // QOS Level 1 ( Delevery at least Once with confirmation )
	public static final int			MQTT_QOS_2 = 2; // QOS Level 2 ( Delivery only once with confirmation with handshake )
	private static final int 		MQTT_KEEP_ALIVE = 300;
	private static final String		MQTT_KEEP_ALIVE_TOPIC_FORMAT = "/users/%s/keepalive";
	private static final byte[] 	MQTT_KEEP_ALIVE_MESSAGE = { 0 };
	private static final int		MQTT_KEEP_ALIVE_QOS = MQTT_QOS_2;

	public boolean isbPortOpen = false;
	private static final boolean 	MQTT_CLEAN_SESSION = false;

	private String 	MQTT_URL_FORMAT;

	private static final String 	ACTION_START 	= DEBUG_TAG + ".START";
	private static final String 	ACTION_STOP		= DEBUG_TAG + ".STOP";
	private static final String 	ACTION_RECONNECT= DEBUG_TAG + ".RECONNECT";
	private static final String 	ACTION_FORCE_RECONNECT= DEBUG_TAG + ".FORCE_RECONNECT";
	private static final String 	ACTION_SANITY= DEBUG_TAG + ".SANITY";
	private static final String 	ACTION_SETTINGS_UPDATE= DEBUG_TAG + ".SANITY";

	private static final String 	DEVICE_ID_FORMAT = "andr_%s"; // Device ID Format, add any prefix you'd like
	// Note: There is a 23 character limit you will get
	// An NPE if you go over that limit
	private boolean mStarted,isReconnecting = false;
	private String mDeviceId;
	private Handler mConnHandler;

	private MqttDefaultFilePersistence mDataStore;
	private MemoryPersistence mMemStore;
	private MqttConnectOptions mOpts;
	private MqttTopic mKeepAliveTopic;

	private MqttClient mClient;
	private MessagesDataSource datasource;

	private AlarmManager mAlarmManager;
	private ConnectivityManager mConnectivityManager;
	Notification n = null;
	public boolean status = false;

	public static void actionStart(Context ctx) {
		Intent i = new Intent(ctx,MQTTService.class);
		i.setAction(ACTION_START);
		ctx.startService(i);
	}

	public static void actionStop(Context ctx) {
		Intent i = new Intent(ctx,MQTTService.class);
		i.setAction(ACTION_STOP);
		ctx.startService(i);
	}

	/**
	 * Initalizes the DeviceId and most instance variables
	 * Including the Connection Handler, Datastore, Alarm Manager
	 * and ConnectivityManager.
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		String vHostname, vTopic, vUsername, vPassword, vPort;
		final SharedPreferences sharedPref = this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

		mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

		mDeviceId = String.format(DEVICE_ID_FORMAT, Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("informService"));


		vHostname	= sharedPref.getString(sHOSTNAME, "");
		vTopic		= sharedPref.getString(sTOPIC,"");
		vUsername	= sharedPref.getString(sUSERNAME, "");
		vPassword	= sharedPref.getString(sPASSWORD, "");
		vPort		= sharedPref.getString(sPORT, "");

		MQTT_BROKER = vHostname; // Broker URL or IP Address
		MQTT_PORT = vPort;				// Broker Port
		USERNAME = vUsername;
		PASSWORD = vPassword;
		TOPIC = vTopic;

		if (MQTT_PORT.equals("1883")){
			MQTT_URL_FORMAT = "tcp://%s:%s";
		} else {
			MQTT_URL_FORMAT = "ssl://%s:%s";
		}

		try {
			ProviderInstaller.installIfNeeded(getApplicationContext());
		} catch (GooglePlayServicesRepairableException e) {
			e.printStackTrace();
		} catch (GooglePlayServicesNotAvailableException e) {
			e.printStackTrace();
		}


		HandlerThread thread = new HandlerThread(MQTT_THREAD_NAME);
		thread.start();

		mConnHandler = new Handler(thread.getLooper());
		mDataStore = new MqttDefaultFilePersistence(getCacheDir().getAbsolutePath());

		mOpts = new MqttConnectOptions();
		mOpts.setCleanSession(MQTT_CLEAN_SESSION);
		mOpts.setPassword(PASSWORD.toCharArray());
		mOpts.setUserName(USERNAME);
		mOpts.setKeepAliveInterval(MQTT_KEEP_ALIVE);

		start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sanityTimerStop();
	}

	/**
	 * Service onStartCommand
	 * Handles the action passed via the Intent
	 *
	 * @return START_REDELIVER_INTENT
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		String action = null;
		int flag = -99;

		if (intent != null) {
			action = intent.getAction();
			if (action != null) {
				flag = intent.getFlags();
				if (logDebug) Log.i(DEBUG_TAG, "Action: " + action);
				if (logDebug) Log.i(DEBUG_TAG, "Flag: " + flag);

				if (action.equals(ACTION_START)) {
					if (logDebug) Log.i(DEBUG_TAG, "Received ACTION_START");
					start();
				}

				if (action.equals(ACTION_STOP)) {
					stop();
				}

				if (action.equals(ACTION_RECONNECT)) {
					if (isNetworkAvailable()) {
						reconnectIfNecessary();
					}
				}
				if (action.equals(ACTION_FORCE_RECONNECT)) {
					if (isNetworkAvailable()) {
						forceReconnect();
					}
				}

				//if (action.equals(ACTION_SANITY) && (intent.getFlags() != 4)) {
				if (action.equals(ACTION_SANITY)) {

					if (logDebug) Log.i(DEBUG_TAG, "Received ACTION SANITY");
					if (isNetworkAvailable() && !isConnected()) {
						if (logDebug) Log.i(DEBUG_TAG, "Received ACTION SANITY FORCE");
						forceReconnect();
					}
				}

				if (action.equals(ACTION_SETTINGS_UPDATE) && (intent.getFlags() != 4)) {
					if (logDebug) Log.i(DEBUG_TAG, "Received ACTION_SETTINGS_UPDATE");
					String vHostname, vTopic, vUsername, vPassword, vPort;
					final SharedPreferences sharedPref = this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

					vHostname = sharedPref.getString(sHOSTNAME, "");
					vTopic = sharedPref.getString(sTOPIC, "");
					vUsername = sharedPref.getString(sUSERNAME, "");
					vPassword = sharedPref.getString(sPASSWORD, "");
					vPort = sharedPref.getString(sPORT, "");

					MQTT_BROKER = vHostname; // Broker URL or IP Address
					MQTT_PORT = vPort;       // Broker Port
					USERNAME = vUsername;
					PASSWORD = vPassword;
					TOPIC = vTopic;

					if (MQTT_PORT.equals("1883")) {
						MQTT_URL_FORMAT = "tcp://%s:%s";
					} else {
						MQTT_URL_FORMAT = "ssl://%s:%s";
					}
					stop();
					start();
				}
			}
		}

		return Service.START_STICKY;
	}

	/**
	 * Attempts connect to the Mqtt Broker
	 * and listen for Connectivity changes
	 * via ConnectivityManager.CONNECTVITIY_ACTION BroadcastReceiver
	 */
	private synchronized void start() {

		if(mStarted) {
			if (logDebug) Log.i(DEBUG_TAG, "Attempt to start while already started");
			return;
		}

		connect();
		registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	/**
	 * Attempts to stop the Mqtt client
	 * as well as halting all keep alive messages queued
	 * in the alarm manager
	 */
	private synchronized void stop() {
		if(!mStarted) {
			if (logDebug) Log.i(DEBUG_TAG, "Attemtpign to stop connection that isn't running");
			return;
		}

		if(mClient != null) {
			mConnHandler.post(new Runnable() {
				@Override
				public void run() {
					try {
						mClient.disconnect();
					} catch (MqttException ex) {
						ex.printStackTrace();
					}
					mClient = null;
					mStarted = false;

					sanityTimerStop();
					status = false;
					statusIcon(status);
				}
			});
		}
		unregisterReceiver(mConnectivityReceiver);
	}

	/**
	 * Connects to the broker with the appropriate datastore
	 */
	private synchronized void connect() {
		String url = String.format(Locale.US, MQTT_URL_FORMAT, MQTT_BROKER, MQTT_PORT);
		Log.i(DEBUG_TAG, "Connecting with URL: " + url);
		try {
			if(mDataStore != null) {
				Log.i(DEBUG_TAG,"Connecting with DataStore");
				mClient = new MqttClient(url,mDeviceId,mDataStore);
			} else {
				Log.i(DEBUG_TAG,"Connecting with MemStore");
				mClient = new MqttClient(url,mDeviceId,mMemStore);
			}
		} catch(MqttException e) {
			sanityTimerStart();
			e.printStackTrace();
		}

		mConnHandler.post(new Runnable() {
			@Override

			public void run() {
				try {

					if (mClient != null && mClient.isConnected())
						return;

					mClient.connect(mOpts);

					String[] topics = TOPIC.split(",");

					for (String topic : topics) {

						if (topic != null) {
                            if (!mClient.isConnected()){
                                mClient.connect(mOpts);
                            }
							mClient.subscribe(topic, MQTT_KEEP_ALIVE_QOS);
						}
					}

					mClient.setCallback(MQTTService.this);

					mStarted = true; // Service is now connected
					status = true;
					statusIcon(status);
					Log.i(DEBUG_TAG, "Successfully connected and subscribed");
					sanityTimerStop();
					isReconnecting = false;

				} catch (Exception e) {
					sanityTimerStart();
					e.printStackTrace();
					//forceReconnect();
				}


			}
		});
	}


	private void sanityTimerStart() {
		Intent i = new Intent();
		i.setClass(this, MQTTService.class);
		i.setAction(ACTION_SANITY);
		PendingIntent pi = PendingIntent.getService(this, 1, i, PendingIntent.FLAG_CANCEL_CURRENT);
		mAlarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + (MQTT_KEEP_ALIVE*1000)/5, (MQTT_KEEP_ALIVE*1000)/5, pi);
	}

	private void sanityTimerStop() {
		Intent i = new Intent();
		i.setClass(this, MQTTService.class);
        i.setAction(ACTION_SANITY);
        PendingIntent pi = PendingIntent.getService(this, 1, i , PendingIntent.FLAG_CANCEL_CURRENT);
        //mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (MQTT_KEEP_ALIVE*10), (MQTT_KEEP_ALIVE*10), pi);
        mAlarmManager.cancel(pi);

	}

	/**
	 * Checkes the current connectivity
	 * and reconnects if it is required.
	 */
	private synchronized void reconnectIfNecessary() {
		status = false;
		statusIcon(false);
		if(mStarted && mClient == null) {
			connect();
		}
	}

	/**
	 * Query's the NetworkInfo via ConnectivityManager
	 * to return the current connected state
	 * @return boolean true if we are connected false otherwise
	 */
	private boolean isNetworkAvailable() {
		NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
		return (info == null) ? false : info.isConnected();
	}

	/**
	 * Verifies the client State with our local connected state
	 * @return true if its a match we are connected false if we aren't connected
	 */
	private boolean isConnected() {
		if(mStarted && mClient != null && !mClient.isConnected()) {
			if (logDebug) Log.i(DEBUG_TAG,"Mismatch between what we think is connected and what is connected");
		}

		if(mClient != null) {
			return (mStarted && mClient.isConnected()) ? true : false;
		}

		return false;
	}

	/**
	 * Receiver that listens for connectivity chanes
	 * via ConnectivityManager
	 */
	private final BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			status = false;
			statusIcon(status);
			}
	};

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * Connectivity Lost from broker
	 */
	@Override
	public void connectionLost(Throwable arg0) {
		mClient = null;
		status = false;
		statusIcon(false);

		if(isNetworkAvailable()) {
			forceReconnect();
		}
	}

	/**
	 * Publish Message Completion
	 */
	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
	}

	/**
	 * Received Message from broker
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message)
			throws Exception {

		String type = "text";

		if (topic.toLowerCase().contains("pic")) {
			type = "pic";
		}

		datasource = new MessagesDataSource(this);
		datasource.open();
		Message messageDB = datasource.createMessage(type,topic,message.toString(),"0");
		datasource.close();
		status = true;
		statusIcon(status);
		informActivity(messageDB.getId());
		Light();

	}


	public void statusIcon(boolean status){
		datasource = new MessagesDataSource(this);
		datasource.open();
		int textCount = datasource.countUnreadMessages("text");
		int picCount = datasource.countUnreadMessages("pic");
		datasource.close();

		Intent intent = new Intent(this, MQTTService.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

		Intent ViewTextIntent = new Intent(this, MyListActivity.class);
		ViewTextIntent.putExtra("type", 1);
		ViewTextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		Intent ViewPicIntent = new Intent(this, MyListActivity.class);
		ViewPicIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ViewPicIntent.putExtra("type", 2);

		//PendingIntent actionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent actionViewTextMessages = PendingIntent.getActivity(this, 1, ViewTextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent actionViewPicMessages = PendingIntent.getActivity(this, 2, ViewPicIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (status) {
			intent.setAction(ACTION_STOP);
			PendingIntent actionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);


			n = new Notification.Builder(this)
							.setContentTitle("MQTT Active")
							.setContentIntent(pIntent)
							//.setVibrate(new long[]{500, 1000})
							//.setLights(Color.BLUE, 1000, 1000)
							.addAction(android.R.drawable.presence_offline, "Disconnect", actionPendingIntent)
							.addAction(android.R.drawable.presence_offline, picCount + " Pictures", actionViewPicMessages)
							.addAction(android.R.drawable.presence_offline, textCount + " Text", actionViewTextMessages)
							.setSmallIcon(R.drawable.m2mgreen)
							.setAutoCancel(false).build();


		} else {
			intent.setAction(ACTION_START);
			PendingIntent actionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			if (isReconnecting){
				n = new Notification.Builder(this)
						.setContentTitle("MQTT Auto Reconnecting...")
						.addAction(android.R.drawable.presence_online, "Connect", actionPendingIntent)
						.addAction(android.R.drawable.presence_offline, picCount + " Pictures", actionViewPicMessages)
						.addAction(android.R.drawable.presence_offline, textCount + " Text", actionViewTextMessages)
						.setLights(Color.RED, 1000, 1000)
						.setContentIntent(pIntent)
						.setSmallIcon(R.drawable.m2mgrey)
						.setAutoCancel(false).build();
			} else {

				n = new Notification.Builder(this)
						.setContentTitle("MQTT Inactive")
						.setLights(Color.RED, 1000, 1000)
						.setContentIntent(pIntent)
						.setSmallIcon(R.drawable.m2mgrey)
						.addAction(android.R.drawable.presence_online, "Connect", actionPendingIntent)
						.addAction(android.R.drawable.presence_offline, picCount + " Pictures", actionViewPicMessages)
						.addAction(android.R.drawable.presence_offline, textCount + " Text", actionViewTextMessages)
						.setAutoCancel(false).build();
			}
		}

		n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(-1, n);

		}

	private void forceReconnect(){

        if (datasource != null) {
            datasource.close();
        }

        sanityTimerStart();

		if (isConnected()){
			sanityTimerStop();
			return;
		}

		isReconnecting = true;
		Log.i(DEBUG_TAG, "connection lost, Reconnecting");

		mClient = null;
		mStarted = false;
		status = false;
		statusIcon(status);
		if (MQTT_BROKER.length() > 0) {
            if (isConnected()) {
                stop();
            }
			start();
		}
	}

	/**
	 * MqttConnectivityException Exception class
	 */
	private class MqttConnectivityException extends Exception {
		private static final long serialVersionUID = -1234567890123456780L;
	}


	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

		@Override

		public void onReceive(Context context, Intent intent) {
		//	Log.d("MQTT","Service Informed from Activity");
			statusIcon(status);
		}

	};

	private void sendLocationBroadcast(Intent intent) {
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void informActivity(String ID) {
		//Log.d("MQTT", "Informing Activity from Service");

		Intent intent = new Intent("informActivity");
		intent.putExtra("newMessageID", ID);
		sendLocationBroadcast(intent);

	}

	private void Light()
	{
		n = new Notification.Builder(this)
				.setVibrate(new long[]{500, 1000})
				.setLights(Color.BLUE, 1000, 1000)
				.build();

		n.flags |= Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_ONGOING_EVENT;

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(-11, n);
	}




}
