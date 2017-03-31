package net.ratmole.apps.mqtt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;


public class MyPicActivity extends Activity {
    public static final String DEBUG_TAG = "MqttService";
    public static List<ID> ids = null;

    public int counter = 0;
    public int EXcounter = 0;
    List<Message> IDS = null;

    private MessagesDataSource datasource;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_pic);

        final Intent intent = getIntent();
        String ID = intent.getStringExtra("id");
        boolean isHidden = intent.getBooleanExtra("isHidden",false);

        TouchImageView image = (TouchImageView) findViewById(R.id.imageV);
        image.setZoom(1);

        datasource = new MessagesDataSource(this);
        datasource.open();

        List<Message> values = null;
        values = datasource.getMessage(ID, "pic");
        String data = values.get(0).getMessage();

        if (isHidden) {
           ids = datasource.getMessageIDS("1", 2);
        } else {
           ids = datasource.getMessageIDS("0", 2);
        }

        for(int i=0;i<ids.size();++i){
            if (ids.get(i).getId().equals(ID)) {
         //       System.out.println("Substring found in:"+i);
                counter = i;
            }
        }

        byte[] decodedString = null;
        Bitmap decodedByte = null;

        try {
            decodedString = Base64.decode(data, Base64.DEFAULT);
            decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            image.setImageBitmap(decodedByte);

        } catch (IllegalArgumentException e) {
            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.bug_error);
            image.setImageBitmap(bitmap);
        }

        datasource.updateMessage(ID, 1);
        datasource.close();
        informService();

        image.setOnTouchListener(new OnSwipeTouchListener(this) {


            @Override
            public void onSwipeLeft() {
                counter++;
                try {

                    showPic(String.valueOf(ids.get(counter).getId()));
                }catch (ArrayIndexOutOfBoundsException e){
                    counter--;
                    Toast.makeText(getApplicationContext(), "No pictures left, swipe right...", Toast.LENGTH_SHORT).show();
                }catch (IndexOutOfBoundsException e) {
                    counter--;
                    Toast.makeText(getApplicationContext(), "No pictures left, swipe right...", Toast.LENGTH_SHORT).show();
                    }
            }

            public void onSwipeRight() {
                counter--;
                try {
                    showPic(String.valueOf(ids.get(counter).getId()));
                }catch (ArrayIndexOutOfBoundsException e){
                    counter++;
                    Toast.makeText(getApplicationContext(), "Already at first image, swipe left...", Toast.LENGTH_SHORT).show();
                }catch (IndexOutOfBoundsException e) {
                    counter++;
                    Toast.makeText(getApplicationContext(), "Already at first image, swipe left...", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void showPic(String Pid) {
        ImageView image = (ImageView) findViewById(R.id.imageV);

        datasource = new MessagesDataSource(getApplicationContext());
        datasource.open();

        List<Message> values = null;
        values = datasource.getMessage(Pid, "pic");
        String data = values.get(0).getMessage();

//        String data = datasource.getMessage(Pid, "pic");
        datasource.updateMessage(Pid, 1);
        datasource.close();
        informService();
        byte[] decodedString = null;
        Bitmap decodedByte = null;

        try {
            decodedString = Base64.decode(data, Base64.DEFAULT);
            decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            image.setImageBitmap(decodedByte);
            EXcounter = 0;


        } catch (IllegalArgumentException e) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bug_error);
            image.setImageBitmap(bitmap);
        }
        catch (NullPointerException e) {
                Toast.makeText(getApplicationContext(), "No pictures left.Exiting...", Toast.LENGTH_SHORT).show();
                finish();
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

    private void sendLocationBroadcast(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void informService() {
     //   Log.d("MQTT", "Informing Service from Activity");
        Intent intent = new Intent("informService");
        sendLocationBroadcast(intent);
    }


}
