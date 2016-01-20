package net.ratmole.apps.mqtt;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageActivity extends Activity {


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        Bundle extras = getIntent().getExtras();

        byte[] decodedString = null;
        ImageView mImg = null;
        String image = null;

        if (extras != null) {
            image = extras.getString("Image");

            mImg = (ImageView) findViewById(R.id.imageV);

            decodedString = Base64.decode(image.toString(), Base64.DEFAULT);
            final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            mImg.setImageBitmap(decodedByte);

            final Button saveB = (Button) findViewById(R.id.saveImage);

            if (isExternalStorageWritable()){
                saveB.setActivated(true);
            } else {
                saveB.setActivated(false);
            }


            saveB.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    OutputStream fOut = null;

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
                    String currentDateandTime = sdf.format(new Date());

                    File folder = new File(Environment.getExternalStorageDirectory().toString()+"/mqttClient");
                    folder.mkdirs();

                    String extStorageDirectory = folder.toString();
                    File file = new File(extStorageDirectory, currentDateandTime+".png");

                    try {
                        fOut = new FileOutputStream(file);
                        decodedByte.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                        fOut.flush();
                        fOut.close();
                        MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
                        saveB.setText("Image Saved!");
                        saveB.setEnabled(false);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }



}
