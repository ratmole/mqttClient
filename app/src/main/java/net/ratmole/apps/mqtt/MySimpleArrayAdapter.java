package net.ratmole.apps.mqtt;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class MySimpleArrayAdapter extends ArrayAdapter<Message> {
    private final Context context;
    private final List<Message> values;

    public MySimpleArrayAdapter(Context context, List<Message> values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.label);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        ImageView picView = (ImageView) rowView.findViewById(R.id.pic);

        String s = values.get(position).getType();
        if (s.contains("pic")) {
            textView.setVisibility(View.GONE);
            imageView.setImageResource(R.drawable.pic);
            byte[] decodedString = Base64.decode(values.get(position).getMessage(), Base64.DEFAULT);
            final Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            picView.setImageBitmap(decodedByte);
        } else {
            textView.setText(values.get(position).getMessage());
            picView.setVisibility(View.GONE);
            imageView.setImageResource(R.drawable.txt);
        }

        return rowView;
    }
    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
    }
}
