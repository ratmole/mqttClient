package net.ratmole.apps.mqtt;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_MESSAGES   = "messages";
    public static final String COLUMN_ID        = "_id";
    public static final String COLUMN_TYPE     = "type";
    public static final String COLUMN_TOPIC     = "topic";
    public static final String COLUMN_MESSAGE   = "message";
    public static final String COLUMN_STATUS   = "status";

    private static final String DATABASE_NAME = "mqttClient.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_MESSAGES + "(" + COLUMN_ID + " integer primary key autoincrement, "
            + COLUMN_TYPE + " text not null, "
            + COLUMN_TOPIC + " text not null, "
            + COLUMN_MESSAGE + " text not null, "
            + COLUMN_STATUS + " text not null );";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        Log.w(MySQLiteHelper.class.getName(), DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

}
