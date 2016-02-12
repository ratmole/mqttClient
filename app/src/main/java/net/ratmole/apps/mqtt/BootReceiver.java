package net.ratmole.apps.mqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class BootReceiver extends BroadcastReceiver {

    private static String PREFS = "mqtt-prefs";
    private static String sHOSTNAME = "-Hostname";

    public void onReceive(Context context, Intent intent) {

        final SharedPreferences sharedPref = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String vHostname;

        vHostname = sharedPref.getString(sHOSTNAME, "");

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            if (vHostname.length() > 0) {
                Intent serviceLauncher = new Intent(context, MQTTService.class);
                context.startService(serviceLauncher);
            }
        }
    }
}