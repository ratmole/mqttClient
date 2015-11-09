package net.ratmole.apps.mqtt;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetWatcher extends BroadcastReceiver {

    public static final String 		DEBUG_TAG = "MqttService"; // Debug TAG

    private static final String 	ACTION_START 	= DEBUG_TAG + ".START"; // Action to start
    private static final String 	ACTION_STOP		= DEBUG_TAG + ".STOP"; // Action to stop
    private static final String 	ACTION_FORCE_RECONNECT = DEBUG_TAG + ".FORCE_RECONNECT"; // Action to reconnect

    private static String PREFS = "mqtt-prefs";
    private static String sHOSTNAME = "-Hostname";

    private String 	MQTT_BROKER;
    private String 	MQTT_PORT;
    private String 	USERNAME;
    private String 	PASSWORD;
    private String TOPIC;

    @Override
    public void onReceive(Context context, Intent intent) {

        final SharedPreferences sharedPref = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String vHostname;

        vHostname	= sharedPref.getString(sHOSTNAME, "");

        //here, check that the network connection is available. If yes, start your service. If not, stop your service.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null) {
            if (info.isConnected()) {

                if (vHostname.length() >	0) {

                    while (!isAvailable(vHostname)){
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    //start service
                    intent = new Intent(context, MQTTService.class);
                    intent.setAction(ACTION_FORCE_RECONNECT);
                    context.startService(intent);
                }


            }

        }
    }
    public Boolean isAvailable(String vHostname) {
        try {

            Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 "+vHostname);
            int returnVal = p1.waitFor();
            boolean reachable = (returnVal==0);
            if(reachable){
                System.out.println("Internet access");
                return reachable;
            }
            else{
                System.out.println("No Internet access");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
        return false;
    }
}
