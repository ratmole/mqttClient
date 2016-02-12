package net.ratmole.apps.mqtt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;

public class MyPicActivity extends Activity {
    public static final String 		DEBUG_TAG = "MqttService";

    private MessagesDataSource datasource;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pic);

        datasource = new MessagesDataSource(this);
        datasource.open();

        Intent intent = getIntent();
        String data = intent.getStringExtra("data");

        ImageView mqttData;
        mqttData = (ImageView) findViewById(R.id.mqttMessage);


        byte[] decodedString = Base64.decode(data.toString(), Base64.DEFAULT);
        final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        mqttData.setImageBitmap(decodedByte);
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
