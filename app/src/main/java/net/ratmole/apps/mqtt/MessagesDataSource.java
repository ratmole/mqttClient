package net.ratmole.apps.mqtt;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

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


    public void deleteMessage(Message message) {
        String id = message.getId();
        System.out.println("Message deleted with id: " + id);
        database.delete(MySQLiteHelper.TABLE_MESSAGES, MySQLiteHelper.COLUMN_ID
                + " = " + id, null);
    }
    public void clearMessages() {
        System.out.println("Messages Cleared!!!");
        database.delete(MySQLiteHelper.TABLE_MESSAGES, null, null);
    }

    public List<Message> getAllMessages() {
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
