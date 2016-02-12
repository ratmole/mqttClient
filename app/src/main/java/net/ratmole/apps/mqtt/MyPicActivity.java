package net.ratmole.apps.mqtt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.graphics.Matrix;

public class MyPicActivity extends Activity {
    public static final String 		DEBUG_TAG = "MqttService";

    private ImageView mqttData;

    private MessagesDataSource datasource;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        datasource = new MessagesDataSource(this);
        datasource.open();

        Intent intent = getIntent();
        String data = intent.getStringExtra("data");

        byte[] decodedString = Base64.decode(data, Base64.DEFAULT);
        final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);


        TouchImageView img = new TouchImageView(this);
        img.setImageBitmap(decodedByte);
        img.setMaxZoom(4f);
        setContentView(img);
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
