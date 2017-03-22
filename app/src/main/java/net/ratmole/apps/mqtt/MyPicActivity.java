package net.ratmole.apps.mqtt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class MyPicActivity extends Activity {
    public static final String 		DEBUG_TAG = "MqttService";

    private MessagesDataSource datasource;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ImageView VgestureView = new ImageView(this);
        ImageView VgestureViewTop = new ImageView(this);

        datasource = new MessagesDataSource(this);
        datasource.open();

        Intent intent = getIntent();

        String data = datasource.getMessage(intent.getStringExtra("id"));

        if (intent.getStringExtra("Rid") != null){
            if (datasource.getMessage(intent.getStringExtra("Rid")) != null){
                data = datasource.getMessage(intent.getStringExtra("Rid"));
            }
        }


            byte[] decodedString = Base64.decode(data, Base64.DEFAULT);
            final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            ZoomImageView img = new ZoomImageView(this);
            img.setImageBitmap(decodedByte);
            img.setMaxZoom(4f);

            setContentView(img);

            FrameLayout.LayoutParams lpTop = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, 300);
            lpTop.gravity = Gravity.TOP;
            VgestureViewTop.setBackgroundColor(Color.DKGRAY);
            this.addContentView(VgestureViewTop, lpTop);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.FILL_PARENT, 300);
            lp.gravity = Gravity.BOTTOM;
            VgestureView.setBackgroundColor(Color.DKGRAY);
            this.addContentView(VgestureView, lp);

            VgestureView.setOnTouchListener(new OnSwipeTouchListener(this) {
                String ID = getIntent().getStringExtra("id");
                int counter = 0;

                @Override
                public void onSwipeLeft() {
                    counter++;
                    int vID = Integer.parseInt(ID.toString()) + counter;
                    getIntent().putExtra("Rid", String.valueOf(vID)); //Optional parameters
                    recreate();
                }

                public void onSwipeRight() {
                    counter--;
                    int vID = Integer.parseInt(ID.toString()) + counter;
                    getIntent().putExtra("Rid", String.valueOf(vID)); //Optional parameters
                    recreate();
                }
            });
        VgestureViewTop.setOnTouchListener(new OnSwipeTouchListener(this) {
            String ID = getIntent().getStringExtra("id");
            int counter = 0;

            @Override
            public void onSwipeLeft() {
                counter++;
                int vID = Integer.parseInt(ID.toString()) + counter;
                getIntent().putExtra("Rid", String.valueOf(vID)); //Optional parameters
                recreate();
            }

            public void onSwipeRight() {
                counter--;
                int vID = Integer.parseInt(ID.toString()) + counter;
                getIntent().putExtra("Rid", String.valueOf(vID)); //Optional parameters
                recreate();
            }
        });

            datasource.close();
    }

   @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }


}
