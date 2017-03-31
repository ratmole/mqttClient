package net.ratmole.apps.mqtt;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.List;
//public class MyListActivity extends ListActivity  {


public class MyListActivity extends ListActivity implements AbsListView.OnScrollListener {
    public static final String DEBUG_TAG = "MqttService";
    private static String PREFS = "mqtt-prefs";

    private static String HOSTNAME = "-Hostname";
    private static String TOPIC = "-Topic";
    private static String USERNAME = "-Username";
    private static String PASSWORD = "-Password";
    private static String PORT = "-Port";
    int mPrevTotalItemCount = 0;
    int Count = 0;
    boolean isHidden = false;

    List<Message> values = null;
    List<Message> valuesNew = null;

    private MySimpleArrayAdapter adapter;

    private MessagesDataSource datasource;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        if (adapter != null && ((firstVisibleItem + visibleItemCount) >= totalItemCount) && totalItemCount != mPrevTotalItemCount && totalItemCount > 0) {


            mPrevTotalItemCount = totalItemCount;


            int remaining = Count-totalItemCount;
            int range = 10;
            if (remaining < 10){
                range = remaining;
            }

            int type = getIntent().getIntExtra("type", 0);
            String cType = null;

            if (type == 0 ) {
                 cType = "all";
            } else if (type == 1){
                 cType = "text";
            } else if (type == 2){
                 cType = "pic";
            }

            datasource = new MessagesDataSource(this);
            datasource.open();

            if (range < 0)
                return;

            if (isHidden) {
                valuesNew = datasource.getNextMessages("1", String.valueOf(totalItemCount - 1) + "," + String.valueOf(range), cType);
            } else{
                valuesNew = datasource.getNextMessages("0", String.valueOf(totalItemCount - 1) + "," + String.valueOf(range), cType);
                }

                adapter.addAll(valuesNew);
                adapter.notifyDataSetChanged();
                getListView().setOnScrollListener(this);
                datasource.close();

        }
    }


    public void onScrollStateChanged(AbsListView v, int s) { }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.clearDB:
                isHidden = false;
                datasource = new MessagesDataSource(this);
                datasource.open();
                datasource.clearMessages();
                values.clear();

                if (adapter != null)
                    adapter.notifyDataSetChanged();

                datasource.close();
                informService();
                return true;

            case R.id.settings:
                isHidden = false;

                Intent launchNewIntent = new Intent(this,SettingsActivity.class);
                startActivityForResult(launchNewIntent, 0);

                return true;

            case R.id.showHidden:
                datasource = new MessagesDataSource(this);
                datasource.open();

                if (adapter != null){
                    isHidden = true;
                    adapter.clear();
                    values = datasource.getAllMessages("1", 0);
                    Count = datasource.countAllMessages();
                    adapter = new MySimpleArrayAdapter(getApplicationContext(), values);
                    setListAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    setListAdapter(adapter);
                    getListView().setOnScrollListener(this);
                }
                datasource.close();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPref = this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("informActivity"));

        String sHostname, sTopic, sPort;

        sHostname	= sharedPref.getString(HOSTNAME, "");
        sTopic		= sharedPref.getString(TOPIC,"");
        sPort		= sharedPref.getString(PORT, "");

        if (	sHostname.length() > 0	&& sTopic.length() 	> 0	&& sPort.length() > 0){
            final Intent intent = new Intent(getApplicationContext(), MQTTService.class);
            startService(intent);
        } else {
            Intent launchNewIntent = new Intent(this,SettingsActivity.class);
            startActivityForResult(launchNewIntent, 0);
        }


        int type = getIntent().getIntExtra("type", 0);
        datasource = new MessagesDataSource(this);
        datasource.open();

        String cType = null;

        if (type == 0 ) {
                Count = datasource.countUnreadMessages("all");
            } else if (type == 1){
                Count = datasource.countUnreadMessages("text");
            } else if (type == 2){
                Count = datasource.countUnreadMessages("pic");
            }

            setContentView(R.layout.activity_list);

            values = datasource.getAllMessages("0", type);
            datasource.close();


            adapter = new MySimpleArrayAdapter(this, values);
            setListAdapter(adapter);
            getListView().setOnScrollListener(this);

    }



    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent myIntent;
        Log.d("pos",""+position);
        switch (values.get(position).getType()) {
            case "text":
                myIntent = new Intent(MyListActivity.this, MyTextActivity.class);
                myIntent.putExtra("id", values.get(position).getId()); //Optional parameters
                myIntent.putExtra("isHidden", isHidden); //Optional parameters
                MyListActivity.this.startActivity(myIntent);
                finish();
                break;
            case "pic":
                myIntent = new Intent(MyListActivity.this, MyPicActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);;
                myIntent.putExtra("id", values.get(position).getId()); //Optional parameters
                myIntent.putExtra("isHidden", isHidden); //Optional parameters
                MyListActivity.this.startActivity(myIntent);
                finish();
                break;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override

        public void onReceive(Context context, Intent intent) {
          //  Log.d("MQTT","Activity Informed from Service");
            String newMessageID = intent.getStringExtra("newMessageID");

            datasource = new MessagesDataSource(context);
            datasource.open();

            if (adapter != null){
                List<Message> newMessagevalue = null;
                newMessagevalue = datasource.getMessage(newMessageID, "all");
                adapter.addAll(newMessagevalue);
                adapter.notifyDataSetChanged();
            }
            datasource.close();
        }

    };

    private void sendLocationBroadcast(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void informService() {
       // Log.d("MQTT", "Informing Service from Activity");
        Intent intent = new Intent("informService");
        sendLocationBroadcast(intent);
    }


}