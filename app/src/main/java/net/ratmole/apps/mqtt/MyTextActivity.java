package net.ratmole.apps.mqtt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;


public class MyTextActivity extends Activity {
    public static final String DEBUG_TAG = "MqttService";
    public static List<ID> ids = null;
    public int counter = 0;
    private MessagesDataSource datasource;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_text);

        datasource = new MessagesDataSource(this);
        datasource.open();

        Intent intent = getIntent();

        String ID = intent.getStringExtra("id");

        List<Message> values = null;
        values = datasource.getMessage(ID, "text");
        String data = values.get(0).getMessage();

        boolean isHidden = intent.getBooleanExtra("isHidden",false);

        if (isHidden) {
            ids = datasource.getMessageIDS("1", 1);
        } else {
            ids = datasource.getMessageIDS("0", 1);
        }

        for(int i=0;i<ids.size();++i){
            if (ids.get(i).getId().equals(ID)) {
                //       System.out.println("Substring found in:"+i);
                counter = i;
            }
        }

        datasource.updateMessage(ID,1);
        datasource.close();

        TextView mqttData;
        mqttData = (TextView) findViewById(R.id.mqttMessage);
        mqttData.setText(data);
        informService();

        mqttData.setOnTouchListener(new OnSwipeTouchListener(this) {

            @Override
            public void onSwipeLeft() {
                counter++;
                try {
                    showText(String.valueOf(ids.get(counter).getId()));
                }catch (ArrayIndexOutOfBoundsException e){
                    counter--;
                    Toast.makeText(getApplicationContext(), "No text left, swipe right...", Toast.LENGTH_SHORT).show();
                }catch (IndexOutOfBoundsException e) {
                    counter--;
                    Toast.makeText(getApplicationContext(), "No text left, swipe right...", Toast.LENGTH_SHORT).show();
                    }
            }

            public void onSwipeRight() {
                counter--;
                try {
                    showText(String.valueOf(ids.get(counter).getId()));
                }catch (ArrayIndexOutOfBoundsException e){
                    counter++;
                    Toast.makeText(getApplicationContext(), "Already at first text, swipe left...", Toast.LENGTH_SHORT).show();
                }catch (IndexOutOfBoundsException e) {
                    counter++;
                    Toast.makeText(getApplicationContext(), "Already at first text, swipe left...", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void showText(String Pid) {
        TextView mqttData;
        mqttData = (TextView) findViewById(R.id.mqttMessage);

        datasource = new MessagesDataSource(getApplicationContext());
        datasource.open();

        List<Message> values = null;
        values = datasource.getMessage(Pid, "text");
        String data = values.get(0).getMessage();

        datasource.updateMessage(Pid, 1);
        datasource.close();
        informService();
        mqttData.setText(data);

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
        //Log.d("MQTT", "Informing Service from Text Activity");
        Intent intent = new Intent("informService");
        sendLocationBroadcast(intent);
    }


}
