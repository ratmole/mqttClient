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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Locale;


public class MQTTService extends Service implements MqttCallback
{

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
	public int nCount	= 1;
	private static final int 		MQTT_KEEP_ALIVE = 120000;
	private static final String		MQTT_KEEP_ALIVE_TOPIC_FORMAT = "/users/%s/keepalive";
	private static final byte[] 	MQTT_KEEP_ALIVE_MESSAGE = { 0 };
	private static final int		MQTT_KEEP_ALIVE_QOS = MQTT_QOS_2;

	public boolean isbPortOpen = false;
	private static final boolean 	MQTT_CLEAN_SESSION = false;

	private String 	MQTT_URL_FORMAT;

	private static final String 	ACTION_START 	= DEBUG_TAG + ".START";
	private static final String 	ACTION_STOP		= DEBUG_TAG + ".STOP";
	private static final String 	ACTION_KEEPALIVE= DEBUG_TAG + ".KEEPALIVE";
	private static final String 	ACTION_RECONNECT= DEBUG_TAG + ".RECONNECT";
	private static final String 	ACTION_FORCE_RECONNECT= DEBUG_TAG + ".FORCE_RECONNECT";
	private static final String 	ACTION_SANITY= DEBUG_TAG + ".SANITY";


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

	public static void actionKeepalive(Context ctx) {
		Intent i = new Intent(ctx,MQTTService.class);
		i.setAction(ACTION_KEEPALIVE);
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

		datasource = new MessagesDataSource(this);
    	datasource.open();

		String vHostname, vTopic, vUsername, vPassword, vPort;
		final SharedPreferences sharedPref = this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

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

		mDeviceId = String.format(DEVICE_ID_FORMAT, Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

		HandlerThread thread = new HandlerThread(MQTT_THREAD_NAME);
		thread.start();

		mConnHandler = new Handler(thread.getLooper());
		mDataStore = new MqttDefaultFilePersistence(getCacheDir().getAbsolutePath());

		mOpts = new MqttConnectOptions();
		mOpts.setCleanSession(MQTT_CLEAN_SESSION);
		mOpts.setPassword(PASSWORD.toCharArray());
		mOpts.setUserName(USERNAME);
		// Do not set keep alive interval on mOpts we keep track of it with alarm's

		mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
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

		if (intent != null) {
			action = intent.getAction();
		}
				//Log.i(DEBUG_TAG, " "+action);

			if (action == null) {
				Log.i(DEBUG_TAG, "Starting service with no action\n Probably from a crash");
				start();
			} else {
				if (action.equals(ACTION_START)) {
					Log.i(DEBUG_TAG, "Received ACTION_START");
					start();
				} else if (action.equals(ACTION_STOP)) {
					stop();
				} else if (action.equals(ACTION_KEEPALIVE)) {
					keepAlive();
				} else if (action.equals(ACTION_RECONNECT)) {
					if (isNetworkAvailable()) {
						reconnectIfNecessary();
					}
				} else if (action.equals(ACTION_FORCE_RECONNECT)) {
					if (isNetworkAvailable()) {
						forceReconnect();
					}
				}
				else if (action.equals(ACTION_SANITY)) {
					if (isNetworkAvailable() && !isConnected()) {
						forceReconnect();
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
			Log.i(DEBUG_TAG, "Attempt to start while already started");
			return;
		}

		if(hasScheduledKeepAlives()) {
			stopKeepAlives();
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
			Log.i(DEBUG_TAG, "Attemtpign to stop connection that isn't running");
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

					stopKeepAlives();
					sanityTimerStop();
					statusIcon(false);
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
							mClient.subscribe(topic, 2);
						}
					}

					mClient.setCallback(MQTTService.this);

					mStarted = true; // Service is now connected
					statusIcon(true);
					Log.i(DEBUG_TAG, "Successfully connected and subscribed starting keep alives");

					startKeepAlives();
					isReconnecting = false;

				} catch (Exception e) {
					e.printStackTrace();
					forceReconnect();
				}
				sanityTimerStart();

			}
		});
	}

	/**
	 * Schedules keep alives via a PendingIntent
	 * in the Alarm Manager
	 */
	private void startKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, MQTTService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + MQTT_KEEP_ALIVE, MQTT_KEEP_ALIVE, pi);
	}

	/**
	 * Cancels the Pending Intent
	 * in the alarm manager
	 */
	private void stopKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, MQTTService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i , 0);
		mAlarmManager.cancel(pi);
	}

	private void sanityTimerStart() {
		Intent i = new Intent();
		i.setClass(this, MQTTService.class);
		i.setAction(ACTION_SANITY);
		PendingIntent pi = PendingIntent.getService(this, 1, i, 0);
		mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + MQTT_KEEP_ALIVE, MQTT_KEEP_ALIVE, pi);
	}

	private void sanityTimerStop() {
		Intent i = new Intent();
		i.setClass(this, MQTTService.class);
		PendingIntent pi = PendingIntent.getService(this, 1, i , 0);
		mAlarmManager.cancel(pi);
	}

	/**
	 * Publishes a KeepALive to the topic
	 * in the broker
	 */
	private synchronized void keepAlive() {
		boolean kFailed = false;

		if(isConnected()) {
			try {
				sendKeepAlive();
				return;

			} catch(MqttConnectivityException ex) {
				kFailed = true;
			} catch(MqttPersistenceException ex) {
				kFailed = true;
			} catch(MqttException ex) {
				kFailed = true;
			} catch (Exception ex) {
				kFailed = true;
			}
				if (kFailed) {
					forceReconnect();
				}
		}
	}

	/**
	 * Checkes the current connectivity
	 * and reconnects if it is required.
	 */
	private synchronized void reconnectIfNecessary() {
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
			Log.i(DEBUG_TAG,"Mismatch between what we think is connected and what is connected");
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
			statusIcon(false);
			}
	};

	private synchronized MqttDeliveryToken sendKeepAlive()
			throws MqttConnectivityException, MqttPersistenceException, MqttException, Exception {

		if(!isConnected())
			throw new MqttConnectivityException();

		if(mKeepAliveTopic == null) {
			mKeepAliveTopic = mClient.getTopic(String.format(Locale.US, MQTT_KEEP_ALIVE_TOPIC_FORMAT, mDeviceId));
		}

		Log.i(DEBUG_TAG, "Sending Keepalive to " + MQTT_BROKER);

		MqttMessage message = new MqttMessage(MQTT_KEEP_ALIVE_MESSAGE);
		message.setQos(MQTT_KEEP_ALIVE_QOS);

		return mKeepAliveTopic.publish(message);
	}

	/**
	 * Query's the AlarmManager to check if there is
	 * a keep alive currently scheduled
	 * @return true if there is currently one scheduled false otherwise
	 */
	private synchronized boolean hasScheduledKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, MQTTService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_NO_CREATE);

		return (pi != null) ? true : false;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * Connectivity Lost from broker
	 */
	@Override
	public void connectionLost(Throwable arg0) {
		stopKeepAlives();

		mClient = null;
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

		Intent notifyIntent = null;
		String type = "text";

		if (topic.toLowerCase().contains("pic")) {
			type = "pic";
		}


		Message messageDB = datasource.createMessage(type,topic,message.toString(),"0");
		final List<Message> values = datasource.getAllMessages();


		Notification.Builder n = new Notification.Builder(this)
				.setVibrate(new long[]{500, 1000})
				.setSmallIcon(R.drawable.m2mgreen)
				.setLights(0xff00ff00, 100, 100)
				.setContentTitle("You have " + values.size() + " unread msg's");

		notifyIntent = new Intent(this, MyListActivity.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

		PendingIntent notifyPendingIntent =
				PendingIntent.getActivity(
						this,
						nCount,
						notifyIntent,
						PendingIntent.FLAG_UPDATE_CURRENT
				);

		n.setContentIntent(notifyPendingIntent);
		n.setAutoCancel(true);

		NotificationManager notificationManager =(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(1, n.build());


		nCount++;

	}
	public void statusIcon(boolean status){

		Intent intent = new Intent(this, MQTTService.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

		if (status) {
			intent.setAction(ACTION_STOP);
			PendingIntent actionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			n = new Notification.Builder(this)
					.setContentTitle("MQTT Active")
					.setContentIntent(pIntent)
					.addAction(android.R.drawable.presence_offline, "Disconnect", actionPendingIntent)
					.setSmallIcon(R.drawable.m2mgreen)
					.setAutoCancel(false).build();
		} else {
			intent.setAction(ACTION_START);
			PendingIntent actionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

			if (isReconnecting){
				n = new Notification.Builder(this)
						.setContentTitle("MQTT Auto Reconnecting...")
						.setContentIntent(pIntent)
						.setSmallIcon(R.drawable.m2mgrey)
						.setAutoCancel(false).build();
			} else {

				n = new Notification.Builder(this)
						.setContentTitle("MQTT Inactive")
						.setContentIntent(pIntent)
						.setSmallIcon(R.drawable.m2mgrey)
						.addAction(android.R.drawable.presence_online, "Connect", actionPendingIntent)
						.setAutoCancel(false).build();
			}
		}

		n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(-1, n);

		}

	private void forceReconnect(){

		if (isConnected()){
			return;
		}

		isReconnecting = true;
		Log.i(DEBUG_TAG, "connection lost, Reconnecting (forceReconnect)");

		stopKeepAlives();
		mClient = null;
		mStarted = false;
		statusIcon(false);
		if (MQTT_BROKER.length() > 0) {
			new Connection().execute();
			while (!isbPortOpen){
				try {
					Log.i(DEBUG_TAG, "Server Unreachable!!! Sleeping for " + (MQTT_KEEP_ALIVE / 4)/1000 + " seconds");
					Thread.sleep(MQTT_KEEP_ALIVE / 4);
					new Connection().execute();
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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

	private class Connection extends AsyncTask {

		@Override
		protected Object doInBackground(Object... arg0) {
			if (isPortOpen(MQTT_BROKER, Integer.parseInt(MQTT_PORT), 3000)){
				isbPortOpen = true;
			} else {
				isbPortOpen = false;
			}
			return null;
		}

		public  boolean isPortOpen(final String ip, final int port, final int timeout) {
			try {
				Socket socket = new Socket();
				socket.connect(new InetSocketAddress(ip, port), timeout);
				socket.close();
				isbPortOpen = true;
				return true;
			}

			catch(ConnectException ce){
				ce.printStackTrace();
				isbPortOpen = false;
				return false;
			}

			catch (Exception ex) {
				ex.printStackTrace();
				isbPortOpen = false;
				return false;
			}
		}

	}
}
