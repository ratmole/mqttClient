package net.ratmole.apps.mqtt;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ImageActivity extends Activity {

    ImageView image;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String image = extras.getString("Image");

            ImageView mImg;
            mImg = (ImageView) findViewById(R.id.imageV);

            byte[] decodedString = Base64.decode(image.toString(), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            mImg.setImageBitmap(decodedByte);
        }
    }


}
