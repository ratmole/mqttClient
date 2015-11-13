package net.ratmole.apps.mqtt;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetWatcher extends BroadcastReceiver {

    public static final String DEBUG_TAG = "MqttService"; // Debug TAG
    private static final String ACTION_FORCE_RECONNECT = DEBUG_TAG + ".FORCE_RECONNECT"; // Action to reconnect
    private static String PREFS = "mqtt-prefs";
    private static String sHOSTNAME = "-Hostname";

    @Override
    public void onReceive(Context context, Intent intent) {

        final SharedPreferences sharedPref = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String vHostname;

        vHostname = sharedPref.getString(sHOSTNAME, "");

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            final NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                if (vHostname.length() > 0) {
                    while (!isAvailable(vHostname)) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    intent = new Intent(context, MQTTService.class);
                    intent.setAction(ACTION_FORCE_RECONNECT);
                    context.startService(intent);
                }
            }
        }
    }

    public Boolean isAvailable(String vHostname) {
        try {
            Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 " + vHostname);
            int returnVal = p1.waitFor();
            boolean reachable = (returnVal == 0);

            if (reachable) {
               return reachable;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

