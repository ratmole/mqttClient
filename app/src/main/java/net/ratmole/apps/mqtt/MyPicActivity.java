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
import android.widget.Toast;

public class MyPicActivity extends Activity {
    public static final String 		DEBUG_TAG = "MqttService";
    public int counter = 0;

    private MessagesDataSource datasource;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ImageView VgestureView = new ImageView(this);
        ImageView VgestureViewTop = new ImageView(this);

        datasource = new MessagesDataSource(this);
        datasource.open();

        final Intent intent = getIntent();


        String data = datasource.getMessage(intent.getStringExtra("id"), "pic");

        if (intent.hasExtra("Rid")){
            if (datasource.getMessage(intent.getStringExtra("Rid"), "pic") != null){
                data = datasource.getMessage(intent.getStringExtra("Rid"), "pic");
            } else {
                if (Integer.parseInt(intent.getStringExtra("id")) <= Integer.parseInt(intent.getStringExtra("Rid"))){
                    Toast.makeText(getApplicationContext(), "No more pictures left, exiting.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "Already at the first picture, swipe the other way!!!", Toast.LENGTH_SHORT).show();
                    counter = 0;
                    int vID = Integer.parseInt(intent.getStringExtra("id").toString()) + counter;
                    getIntent().putExtra("Rid", String.valueOf(vID)); //Optional parameters
                   }

            }
        }

            byte[] decodedString = null;
            Bitmap decodedByte = null;
            ZoomImageView img = new ZoomImageView(this);


            try {
                decodedString = Base64.decode(data, Base64.DEFAULT);
                decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                img.setImageBitmap(decodedByte);

            }catch (IllegalArgumentException e){
                Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bug_error);
                img.setImageBitmap(bitmap);
                }

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

                @Override
                public void onSwipeLeft() {

                    Intent intentIn = getIntent();
                    String ID = null;

                    if (intent.hasExtra("Rid")){
                        ID = intent.getStringExtra("Rid");
                    } else if (intent.hasExtra("id")){
                        ID = intent.getStringExtra("id");
                    }

                        counter++;

                    int vID = Integer.parseInt(ID.toString()) + counter;
                    getIntent().putExtra("Rid", String.valueOf(vID)); //Optional parameters
                    recreate();
                }

                public void onSwipeRight() {

                    Intent intentIn = getIntent();
                    String ID = null;

                    if (intent.hasExtra("Rid")){
                        ID = intent.getStringExtra("Rid");
                    } else if (intent.hasExtra("id")){
                        ID = intent.getStringExtra("id");
                    }

                        counter--;

                    int vID = Integer.parseInt(ID.toString()) + counter;
                    getIntent().putExtra("Rid", String.valueOf(vID)); //Optional parameters
                    recreate();
                }
            });
        VgestureViewTop.setOnTouchListener(new OnSwipeTouchListener(this) {

            @Override
            public void onSwipeLeft() {

                Intent intentIn = getIntent();
                String ID = null;

                if (intent.hasExtra("Rid")){
                    ID = intent.getStringExtra("Rid");
                } else if (intent.hasExtra("id")){
                    ID = intent.getStringExtra("id");
                }

                    counter++;

                int vID = Integer.parseInt(ID.toString()) + counter;
                intentIn.putExtra("Rid", String.valueOf(vID)); //Optional parameters
                recreate();
            }

            public void onSwipeRight() {

                Intent intentIn = getIntent();
                String ID = null;

                if (intent.hasExtra("Rid")){
                    ID = intent.getStringExtra("Rid");
                } else if (intent.hasExtra("id")){
                    ID = intent.getStringExtra("id");
                }


                    counter--;

                int vID = Integer.parseInt(ID.toString()) + counter;
                intentIn.putExtra("Rid", String.valueOf(vID)); //Optional parameters
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
