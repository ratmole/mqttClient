package net.ratmole.apps.mqtt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class MessagesDataSource {

    // Database fields
    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;
    private String[] allColumns = {
            MySQLiteHelper.COLUMN_ID,
            MySQLiteHelper.COLUMN_TYPE,
            MySQLiteHelper.COLUMN_TOPIC,
            MySQLiteHelper.COLUMN_MESSAGE,
            MySQLiteHelper.COLUMN_STATUS

    };
    private String unRead = MySQLiteHelper.COLUMN_STATUS + "=?" ;
    private String[] unReadArgs = {"0"};

    public MessagesDataSource(Context context) {
        dbHelper = new MySQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public Message createMessage(String type, String topic, String message, String status) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_TYPE, type);
        values.put(MySQLiteHelper.COLUMN_TOPIC, topic);
        values.put(MySQLiteHelper.COLUMN_MESSAGE, message);
        values.put(MySQLiteHelper.COLUMN_STATUS, status);
        long insertId = database.insert(MySQLiteHelper.TABLE_MESSAGES, null, values);
        Cursor cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        Message newMessage = cursorToMessage(cursor);
        cursor.close();
        return newMessage;
    }

    public void updateMessage(String id, int Status) {

        ContentValues values=new ContentValues();
        values.put(MySQLiteHelper.COLUMN_STATUS, Status);
        database.update(MySQLiteHelper.TABLE_MESSAGES, values, MySQLiteHelper.COLUMN_ID + " = " + id, null);
        System.out.println("Message with id: " + id +", status: "+Status);

    }

    public void deleteMessage(String id) {
        //String id = message.getId();
        System.out.println("Message deleted with id: " + id);
        database.delete(MySQLiteHelper.TABLE_MESSAGES, MySQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }
    public void clearMessages() {
        System.out.println("Messages Cleared!!!");
        database.delete(MySQLiteHelper.TABLE_MESSAGES, null, null);
    }
    public int countUnreadMessages() {

        Cursor cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                allColumns, unRead, unReadArgs, null, null, null);

        int cnt = cursor.getCount();
        cursor.close();
        return cnt;
    }

    public List<Message> getAllMessages(String[] unReadArgs) {

        List<Message> messages = new ArrayList<Message>();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                allColumns, unRead, unReadArgs, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Message message = cursorToMessage(cursor);
            messages.add(message);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return messages;
    }

    public String getMessage(String id, String type) {
        try {
            Cursor cursor = database.query(MySQLiteHelper.TABLE_MESSAGES, new String[] { MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_TYPE,
                MySQLiteHelper.COLUMN_MESSAGE }, MySQLiteHelper.COLUMN_ID + "=?" +" AND " + MySQLiteHelper.COLUMN_TYPE + "=?" ,
                new String[] { String.valueOf(id),String.valueOf(type) }, null, null, null, null);

        if (cursor != null)
            cursor.moveToFirst();

        return cursor.getString(2);

        } catch (CursorIndexOutOfBoundsException e){
            return null;
        }

        }



    private Message cursorToMessage(Cursor cursor) {
        Message message = new Message();
        message.setId(cursor.getString(0));
        message.setType(cursor.getString(1));
        message.setTopic(cursor.getString(2));
        message.setMessage(cursor.getString(3));
        message.setStatus(cursor.getString(4));
        return message;
    }
}
