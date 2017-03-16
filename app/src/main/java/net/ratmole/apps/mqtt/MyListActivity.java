package net.ratmole.apps.mqtt;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

public class MyListActivity extends ListActivity {
    public static final String DEBUG_TAG = "MqttService";
    private static String PREFS = "mqtt-prefs";

    private static String HOSTNAME = "-Hostname";
    private static String TOPIC = "-Topic";
    private static String USERNAME = "-Username";
    private static String PASSWORD = "-Password";
    private static String PORT = "-Port";

    List<Message> values = null;
    private MySimpleArrayAdapter adapter;

    private MessagesDataSource datasource;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.clearDB:
                datasource = new MessagesDataSource(this);
                datasource.open();
                datasource.clearMessages();
                values.clear();

                if (adapter != null)
                    adapter.notifyDataSetChanged();

                return true;
            case R.id.settings:
                Intent launchNewIntent = new Intent(this,SettingsActivity.class);
                startActivityForResult(launchNewIntent, 0);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final SharedPreferences sharedPref = this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);


        String sHostname, sTopic, sUsername, sPassword, sPort;

        sHostname	= sharedPref.getString(HOSTNAME, "");
        sTopic		= sharedPref.getString(TOPIC,"");
        sUsername	= sharedPref.getString(USERNAME, "");
        sPassword	= sharedPref.getString(PASSWORD, "");
        sPort		= sharedPref.getString(PORT, "");
        if (	sHostname.length() > 0	&& sTopic.length() 	> 0	&& sPort.length() > 0){
            final Intent intent = new Intent(getApplicationContext(), MQTTService.class);
            startService(intent);
        } else {
            Toast.makeText(getApplicationContext(), "Please Fill Hostname, Topic and Port", Toast.LENGTH_LONG).show();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("speedExceeded"));

        setContentView(R.layout.activity_list);

        datasource = new MessagesDataSource(this);
        datasource.open();

        values = datasource.getAllMessages();

        if (values.isEmpty())
            return;

        adapter = new MySimpleArrayAdapter(this, values);
        setListAdapter(adapter);

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (adapter != null){
                adapter.clear();
                datasource = new MessagesDataSource(context);
                datasource.open();
                values = datasource.getAllMessages();
                adapter = new MySimpleArrayAdapter(context, values);
                setListAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        datasource.open();

        Intent myIntent;

        switch (values.get(position).getType()) {
            case "text":
                myIntent = new Intent(MyListActivity.this, MyTextActivity.class);
                myIntent.putExtra("data", values.get(position).getMessage()); //Optional parameters
                MyListActivity.this.startActivity(myIntent);
                break;
            case "pic":
                myIntent = new Intent(MyListActivity.this, MyPicActivity.class);
                myIntent.putExtra("data", values.get(position).getMessage()); //Optional parameters
                MyListActivity.this.startActivity(myIntent);
                break;
        }


        datasource.deleteMessage(values.get(position));
        values.remove(position);
        if (adapter != null)
            adapter.notifyDataSetChanged();

    }


    @Override
    protected void onResume() {
        super.onResume();
        datasource.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        datasource.close();
    }

}