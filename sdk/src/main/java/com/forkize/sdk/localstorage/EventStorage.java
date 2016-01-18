package com.forkize.sdk.localstorage;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class EventStorage implements IForkizeStorage {

    private EventMemoryStorage cache;
    private EventSQLStorage sqlStorage;
    private final Object eventLock;

    public EventStorage(SQLiteDatabase database) {
        this.eventLock = new Object();
        this.cache = new EventMemoryStorage();
        this.sqlStorage = new EventSQLStorage(database);
    }

    public void addEvent(String event) throws Exception {
        synchronized (this.eventLock) {
            if (!this.cache.write(event)) {
                this.sqlStorage.write(this.cache.read());
                this.cache.clear();
                this.cache.write(event);
            }
        }
    }

    public String[] getEvents(int eventCount) {

        synchronized (this.eventLock) {
            try {
                return this.sqlStorage.read(eventCount);
            } catch (Exception e) {
                Log.e("Forkize SDK", "Exception thrown getting events", e);
            }
        }
        return null;
    }

    public void removeEvents(int eventCount) {
        synchronized (this.eventLock) {
            try {
                this.sqlStorage.flush(eventCount);
            } catch (Exception e) {
                Log.e("Forkize SDK", "Exception thrown removing events", e);
            }
        }
    }

    public void flushCacheToDatabase() {
        try {
            this.sqlStorage.write(this.cache.read());
            this.cache.clear();
        } catch (Exception e) {
            Log.e("Forkize SDK", "Exception thrown flushing data to database");
        }
    }

    public void close() {
        this.cache.clear();
        if (this.sqlStorage != null) {
            this.sqlStorage.close();
        }
    }
}