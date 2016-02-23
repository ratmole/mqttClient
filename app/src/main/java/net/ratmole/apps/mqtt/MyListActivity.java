package net.ratmole.apps.mqtt;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

public class MyListActivity extends ListActivity {
    public static final String DEBUG_TAG = "MqttService";

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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        datasource = new MessagesDataSource(this);
        datasource.open();

        values = datasource.getAllMessages();

        if (values.isEmpty())
            return;

        adapter = new MySimpleArrayAdapter(this, values);
        setListAdapter(adapter);

   }

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
        datasource.open();
        super.onResume();
    }

    @Override
    protected void onPause() {
        datasource.close();
        super.onPause();
    }



}
