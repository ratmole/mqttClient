package net.ratmole.apps.mqtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    private static String PREFS = "mqtt-prefs";
    private static String sHOSTNAME = "-Hostname";

    public void onReceive(Context context, Intent intent) {

        final SharedPreferences sharedPref = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String vHostname;

        vHostname = sharedPref.getString(sHOSTNAME, "");

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            if (vHostname.length() > 0) {
                while (!isAvailable(vHostname)) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Intent serviceLauncher = new Intent(context, MQTTService.class);
                context.startService(serviceLauncher);
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