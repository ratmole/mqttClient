package net.ratmole.apps.mqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

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
                    intent = new Intent(context, MQTTService.class);
                    intent.setAction(ACTION_FORCE_RECONNECT);
                    context.startService(intent);
                }
            }
        }
    }

}

