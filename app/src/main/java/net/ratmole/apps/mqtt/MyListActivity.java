package net.ratmole.apps.mqtt;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

public class MyListActivity extends ListActivity {
    public static final String DEBUG_TAG = "MqttService";

    List<Message> values = null;

    private MessagesDataSource datasource;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        datasource = new MessagesDataSource(this);
        datasource.open();

        values = datasource.getAllMessages();

        if (values.isEmpty())
            return;

        final MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(this, values);
        setListAdapter(adapter);

        final Button clearAll = (Button) findViewById(R.id.clearAll);

        clearAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                datasource.clearMessages();
                values.clear();
                adapter.notifyDataSetChanged();
                finish();

            }
        });

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
        ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
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
