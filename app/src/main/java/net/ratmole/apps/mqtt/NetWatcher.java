package net.ratmole.apps.mqtt;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetWatcher extends BroadcastReceiver {

    public static final String 		DEBUG_TAG = "MqttService"; // Debug TAG

    private static final String 	ACTION_START 	= DEBUG_TAG + ".START"; // Action to start
    private static final String 	ACTION_STOP		= DEBUG_TAG + ".STOP"; // Action to stop
    private static final String 	ACTION_FORCE_RECONNECT = DEBUG_TAG + ".FORCE_RECONNECT"; // Action to reconnect

    @Override
    public void onReceive(Context context, Intent intent) {
        //here, check that the network connection is available. If yes, start your service. If not, stop your service.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null) {
            if (info.isConnected()) {
                //start service
                intent = new Intent(context, MQTTService.class);
                intent.setAction(ACTION_FORCE_RECONNECT);
                context.startService(intent);
            }

        }
    }
}
