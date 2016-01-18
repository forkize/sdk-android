package com.forkize.sdk;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.forkize.sdk.localstorage.EventStorage;
import com.forkize.sdk.localstorage.IForkizeStorage;
import com.forkize.sdk.localstorage.UserProfileStorage;

public class StorageFactory implements IForkizeStorage {

    private static StorageFactory instance;

    private Context context;
    private IForkizeStorage eventStorage;
    private IForkizeStorage userProfileStorage;

    private SQLiteDatabase database;
    private ForkizeSQLiteOpenHelper dbHelper;

    private StorageFactory() {
    }

    protected static StorageFactory getInstance() {
        if (instance == null) {
            instance = new StorageFactory();
        }

        return instance;
    }

    protected void setContext(Context context) {
        this.context = context;
    }

    protected IForkizeStorage getStorage(String storageType) {
        // ** dbHelper is shared for all storage types
        if (database == null) {
            if (dbHelper == null) {
                String dbName = ForkizeConfig.getInstance().getDbName();
                dbHelper = new ForkizeSQLiteOpenHelper(context, dbName, null, 1);
                long maxSQLiteDBSize = ForkizeConfig.getInstance().getMaxSQLiteDBSize();
                this.database = dbHelper.getWritableDatabase();
                this.database.setMaximumSize(maxSQLiteDBSize);
            }
        }

        // ** eventStorage and userProfileStorage are singleton
        switch (storageType) {
            case "EVENT":
                if (eventStorage == null) {
                    eventStorage = new EventStorage(database);
                }
                return eventStorage;
            case "USER":
                if (userProfileStorage == null) {
                    userProfileStorage = new UserProfileStorage(database);
                }
                return userProfileStorage;
            default:
                return null;
        }
    }

    private class ForkizeSQLiteOpenHelper extends SQLiteOpenHelper {

        public ForkizeSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // FZ::TODO when it's called ?
            db.execSQL("CREATE TABLE " + MAIN_TABLE + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_UID + " TEXT NOT NULL, " + COLUMN_TID + " TEXT NOT NULL, " + COLUMN_ALIAS_ID + " TEXT, " + COLUMN_UP + " TEXT, " + COLUMN_UP_CHANGELOG + " TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + MAIN_TABLE);
            onCreate(db);
        }
    }
}
