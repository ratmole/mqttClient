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
    private String[] IDColumn = {
            MySQLiteHelper.COLUMN_ID,
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
        //System.out.println("Message with id: " + id +", status: "+Status);

    }

    public void clearMessages() {
       // System.out.println("Messages Cleared!!!");
        database.delete(MySQLiteHelper.TABLE_MESSAGES, null, null);
    }

    public int countUnreadMessages(String type) {

        Cursor cursor = null;

        if (type.matches("all")) {
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                    allColumns, unRead, unReadArgs, null, null, null);
        } else  {
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                    allColumns, unRead + " AND " + MySQLiteHelper.COLUMN_TYPE + "=?" , new String[] { "0",type }, null, null, null);
        }

        int cnt = cursor.getCount();
        cursor.close();
        return cnt;
    }

    public int countAllMessages() {

        Cursor cursor = null;
        cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                new String[] {MySQLiteHelper.COLUMN_ID}, null, null, null, null, null);

        int cnt = cursor.getCount();
        cursor.close();
        return cnt;
    }


    public List<ID> getMessageIDS(String Status, int type) {

        List<ID> ids = new ArrayList<ID>();
        Cursor cursor = null;
        try{

        if (type == 0 ) {
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                    IDColumn, unRead, new String[] { Status }, null, null, null, null);
        } else if (type == 1){
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                    IDColumn, unRead +" AND "+ MySQLiteHelper.COLUMN_TYPE + "=?", new String[] { Status,"text" }, null, null, null, null);
        } else if (type == 2){
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                    IDColumn, unRead +" AND "+ MySQLiteHelper.COLUMN_TYPE + "=?", new String[] { Status,"pic" }, null, null, null, null);
        }

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ID ID = cursorToID(cursor);
                ids.add(ID);
                cursor.moveToNext();
            }
            // make sure to close the cursor
            cursor.close();

            return ids;

    } catch (CursorIndexOutOfBoundsException e){
        return null;
    }

    }
    public List<Message> getAllMessages(String Status,int type) {

        List<Message> messages = new ArrayList<Message>();
        Cursor cursor = null;

        if (type == 0 ) {
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                    allColumns, unRead, new String[] { Status }, null, null, null, "15");
        } else if (type == 1){
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                    allColumns, unRead +" AND "+ MySQLiteHelper.COLUMN_TYPE + "=?", new String[] { Status,"text" }, null, null, null, "15");
        } else if (type == 2){
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                    allColumns, unRead +" AND "+ MySQLiteHelper.COLUMN_TYPE + "=?", new String[] { Status,"pic" }, null, null, null, "15");
        }

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

    public List<Message> getNextMessages(String Status, String range, String type) {

        List<Message> messages = new ArrayList<Message>();
        Cursor cursor = null;

        if (type.equals("all") ) {
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                    allColumns, unRead, new String[] {Status}, null, null, null, range);
        } else {
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES,
                    allColumns, unRead + " AND " + MySQLiteHelper.COLUMN_TYPE + "=?", new String[]{Status, type}, null, null, null, range);
        }

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

        public List<Message> getMessage(String id, String type) {
            List<Message> messages = new ArrayList<Message>();
            Cursor cursor = null;

        if (type.equals("all") ) {
                 cursor = database.query(MySQLiteHelper.TABLE_MESSAGES, allColumns, MySQLiteHelper.COLUMN_ID + "=?",
                         new String[]{String.valueOf(id)}, null, null, null, null);
             } else{
            cursor = database.query(MySQLiteHelper.TABLE_MESSAGES, allColumns, MySQLiteHelper.COLUMN_ID + "=?" + " AND " + MySQLiteHelper.COLUMN_TYPE + "=?",
                    new String[]{String.valueOf(id), String.valueOf(type)}, null, null, null, null);
        }

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

    private ID cursorToID(Cursor cursor) {
        ID ID = new ID();
        ID.setId(cursor.getString(0));

        return ID;
    }
}
