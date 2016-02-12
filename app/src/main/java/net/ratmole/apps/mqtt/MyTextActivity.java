package net.ratmole.apps.mqtt;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;


import java.util.List;

public class MyTextActivity extends Activity {
    public static final String 		DEBUG_TAG = "MqttService";

    private MessagesDataSource datasource;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);

        datasource = new MessagesDataSource(this);
        datasource.open();

        Intent intent = getIntent();
        String data = intent.getStringExtra("data");

        TextView mqttData;
        mqttData = (TextView) findViewById(R.id.mqttMessage);
        mqttData.setText(data);
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
