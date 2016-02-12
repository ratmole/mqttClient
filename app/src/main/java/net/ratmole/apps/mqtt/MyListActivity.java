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


    private MessagesDataSource datasource;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        datasource = new MessagesDataSource(this);
        datasource.open();

        final List<Message> values = datasource.getAllMessages();

        if (values.isEmpty())
            return;

        final ArrayAdapter<Message> Mainadapter = new ArrayAdapter<Message>(this, android.R.layout.simple_list_item_1, values);
        setListAdapter(Mainadapter);

        final Button clearAll = (Button) findViewById(R.id.clearAll);

        clearAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                datasource.clearMessages();
                values.clear();
                Mainadapter.notifyDataSetChanged();
                finish();

            }
        });



        final ListView lv = getListView();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {

                String value = (String) adapter.getItemAtPosition(position).toString();
                Log.i(DEBUG_TAG, "" + values.get(position).getId());
                datasource.deleteMessage(values.get(position));

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

                values.remove(position);
                Mainadapter.notifyDataSetChanged();


            }
        });
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
