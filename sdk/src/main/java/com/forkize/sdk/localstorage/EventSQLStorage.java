package com.forkize.sdk.localstorage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.forkize.sdk.ForkizeInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventSQLStorage implements IForkizeStorage {

    private String CURRENT_TABLE = "dump";

    private SQLiteDatabase database;
    private AtomicBoolean connectionOpen;

    protected EventSQLStorage(SQLiteDatabase database) {
        this.database = database;
        this.connectionOpen = new AtomicBoolean(true);
    }

    public boolean write(String[] data) throws Exception {
        if (this.connectionOpen.get()) {
            CURRENT_TABLE = ForkizeInstance.getInstance().getUserProfile().getTableName();
            if (!CURRENT_TABLE.equals("dump")) {

                try {
                    for (String aData : data) {
                        ContentValues values = new ContentValues();
                        values.put(COLUMN_EVENT, aData);
                        if (this.database.insertOrThrow(CURRENT_TABLE, null, values) == -1) {
                            Log.e("Forkize SDK", "Error adding event to database");
                            return false;
                        } else {
                            Log.i("Forkize SDK", "Event added to database");
                        }
                    }
                } catch (Exception e) {
                    Log.e("Forkize SDK", "Exception thrown writing database", e);
                } finally {
                    Log.i("Forkize SDK", "End writing to database");
                }

                return true;
            }
        }
        return false;
    }

    public String[] read(int quantity) throws Exception {
        List<String> events = new ArrayList<>();
        if (this.connectionOpen.get()) {
            CURRENT_TABLE = ForkizeInstance.getInstance().getUserProfile().getTableName();
            if (!CURRENT_TABLE.equals("dump")) {
                Cursor cursor = null;
                try {
                    cursor = this.database.query(CURRENT_TABLE, null, null, null, null, null, COLUMN_ID, String.valueOf(quantity));

                    Log.i("Forkize SDK", String.valueOf(cursor.getCount()) + " events got by cursor");

                    cursor.moveToFirst();

                    while (!cursor.isAfterLast()) {
                        events.add(cursor.getString(cursor.getColumnIndex(COLUMN_EVENT)));
                        cursor.moveToNext();
                    }
                } catch (Exception e) {
                    Log.e("Forkize SDK", "Error occurred getting events from SQLiteDatabase", e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }

        int len = events.size();
        String[] data = new String[len];

        for (int i = 0; i < len; i++) {
            data[i] = events.get(i);
        }
        return data;
    }

    public void flush(int eventCount) {
        if (this.connectionOpen.get()) {
            CURRENT_TABLE = ForkizeInstance.getInstance().getUserProfile().getTableName();
            if (!CURRENT_TABLE.equals("dump")) {
                Cursor cursor = null;
                try {
                    cursor = this.database.query(CURRENT_TABLE, null, null, null, null, null, COLUMN_ID, null);
                    cursor.moveToFirst();

                    while (!cursor.isAfterLast() && eventCount > 0) {
                        int id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));

                        this.database.delete(CURRENT_TABLE, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
                        cursor.moveToNext();
                        eventCount--;
                    }
                } catch (Exception e) {
                    Log.e("Forkize SDK", "Error occurred flushing events from SQLiteDatabase", e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    public void close() {
        this.database.close();
        this.connectionOpen.set(false);
    }
}