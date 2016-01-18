package com.forkize.sdk.localstorage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.forkize.sdk.ForkizeConfig;
import com.forkize.sdk.ForkizeInstance;

import java.util.concurrent.atomic.AtomicBoolean;

public class UserProfileStorage implements IForkizeStorage {
    private String CURRENT_TABLE = "dump";

    private SQLiteDatabase database;
    private AtomicBoolean connectionOpen;

    public UserProfileStorage(SQLiteDatabase database) {
        this.database = database;
        this.connectionOpen = new AtomicBoolean(true);
    }

    public synchronized String getUserProfileFromDB(String uid) throws Exception {
        if (this.connectionOpen.get()) {
            Cursor cursor = null;

            try {
                cursor = this.database.query(MAIN_TABLE, new String[]{COLUMN_UP}, COLUMN_UID + "== '" + uid + "'", null, null, null, null);
                cursor.moveToFirst();

                if (cursor.isLast()) {
                    return cursor.getString(cursor.getColumnIndex(COLUMN_UP));
                } else {
                    Log.e("Forkize SDK", "Something went wrong, You have more than one rows with given user id");
                }
            } catch (Exception e) {
                Log.e("Forkize SDK", "Error getting user profile info");
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return null;
    }

    public synchronized void writeUserProfileInDB(String uid, String userProfile) throws Exception {
        if (this.connectionOpen.get()) {
            Cursor cursor = null;
            try {
                cursor = this.database.query(MAIN_TABLE, null, COLUMN_UID + "== '" + uid + "'", null, null, null, null);
                if (!cursor.moveToFirst())
                    return;

                if (cursor.isLast()) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_UID, uid);
                    values.put(COLUMN_TID, cursor.getString(cursor.getColumnIndex(COLUMN_TID)));
                    values.put(COLUMN_ALIAS_ID, cursor.getString(cursor.getColumnIndex(COLUMN_ALIAS_ID)));
                    values.put(COLUMN_UP, userProfile);
                    values.put(COLUMN_UP_CHANGELOG, cursor.getString(cursor.getColumnIndex(COLUMN_UP_CHANGELOG)));

                    if (this.database.insertOrThrow(MAIN_TABLE, null, values) != -1) {
                        this.database.delete(MAIN_TABLE, COLUMN_ID + " = ?", new String[]{cursor.getString(cursor.getColumnIndex(COLUMN_ID))});
                    } else {
                        Log.e("Forkize SDK", "Something went wrong putting row into database in writeUserProfileInDB");
                    }

                } else {
                    Log.e("Forkize SDK", "Something went wrong, You have more than one rows with given user id");
                }
            } catch (Exception e) {
                Log.e("Forkize SDK", "Error writing user profile in database");
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }

    public synchronized String getUserProfileChangeLogFromDB(String uid) throws Exception {
        if (this.connectionOpen.get()) {
            Cursor cursor = null;

            try {
                cursor = this.database.query(MAIN_TABLE, new String[]{COLUMN_UP_CHANGELOG}, COLUMN_UID + "== '" + uid + "'", null, null, null, null);
                cursor.moveToFirst();

                if (cursor.isLast()) {
                    return cursor.getString(cursor.getColumnIndex(COLUMN_UP_CHANGELOG));
                } else {
                    Log.e("Forkize SDK", "Something went wrong, You have more than one rows with given user id");
                }
            } catch (Exception e) {
                Log.e("Forkize SDK", "Error getting user profile change log");
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return null;
    }

    public synchronized void writeUserProfileChangeLogInDB(String uid, String userProfileChangeLog) throws Exception {
        if (this.connectionOpen.get()) {
            Cursor cursor = null;
            try {
                cursor = this.database.query(MAIN_TABLE, null, COLUMN_UID + "== '" + uid + "'", null, null, null, null);
                if (!cursor.moveToFirst())
                    return;

                if (cursor.isLast()) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_UID, uid);
                    values.put(COLUMN_TID, cursor.getString(cursor.getColumnIndex(COLUMN_TID)));
                    values.put(COLUMN_ALIAS_ID, cursor.getString(cursor.getColumnIndex(COLUMN_ALIAS_ID)));
                    values.put(COLUMN_UP, cursor.getString(cursor.getColumnIndex(COLUMN_UP)));
                    values.put(COLUMN_UP_CHANGELOG, userProfileChangeLog);

                    if (this.database.insertOrThrow(MAIN_TABLE, null, values) != -1) {
                        this.database.delete(MAIN_TABLE, COLUMN_ID + " = ?", new String[]{cursor.getString(cursor.getColumnIndex(COLUMN_ID))});
                    } else {
                        Log.e("Forkize SDK", "Something went wrong putting row into database in writeUserProfileChangeLogInDB");
                    }

                } else {
                    Log.e("Forkize SDK", "Something went wrong, You have more than one rows with given user id");
                }
            } catch (Exception e) {
                Log.e("Forkize SDK", "Error writing user profile change log in database");
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }


    public synchronized String changeUserId() throws Exception {
        String userID = ForkizeInstance.getInstance().getUserProfile().getUserId();
        if (this.connectionOpen.get()) {
            Cursor cursor = null;

            try {
                cursor = this.database.query(MAIN_TABLE, new String[]{COLUMN_TID}, COLUMN_UID + "== '" + userID + "'", null, null, null, null);
                cursor.moveToFirst();

                if (cursor.isLast()) {
                    CURRENT_TABLE = cursor.getString(cursor.getColumnIndex(COLUMN_TID));
                } else {
                    CURRENT_TABLE = "fz_user_" + String.valueOf(System.currentTimeMillis());
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_UID, userID);
                    values.put(COLUMN_TID, CURRENT_TABLE);

                    if (this.database.insertOrThrow(MAIN_TABLE, null, values) != -1)
                        Log.i("Forkize SDK", "New table id and name are successfully added to user_table!");

                    this.database.execSQL("CREATE TABLE " + CURRENT_TABLE + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_EVENT + " TEXT NOT NULL);");
                }
            } catch (Exception e) {
                Log.e("Forkize SDK", "Error identifying SQLite database user");
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return CURRENT_TABLE;
    }

    public synchronized void alias(String oldId, String newId) throws Exception {

        if (this.connectionOpen.get()) {
            Cursor cursor = null;

            try {
                cursor = this.database.query(MAIN_TABLE, null, COLUMN_UID + "== '" + oldId + "'", null, null, null, null);
                cursor.moveToFirst();

                if (cursor.isLast()) {
                    if (!newId.equals(cursor.getString(cursor.getColumnIndex(COLUMN_ALIAS_ID)))) {
                        ContentValues values = new ContentValues();
                        values.put(COLUMN_UID, cursor.getString(cursor.getColumnIndex(COLUMN_UID)));
                        values.put(COLUMN_TID, cursor.getString(cursor.getColumnIndex(COLUMN_TID)));
                        values.put(COLUMN_ALIAS_ID, newId);
                        values.put(COLUMN_UP, cursor.getString(cursor.getColumnIndex(COLUMN_UP)));
                        values.put(COLUMN_UP_CHANGELOG, cursor.getString(cursor.getColumnIndex(COLUMN_UP_CHANGELOG)));

                        this.database.delete(MAIN_TABLE, COLUMN_UID + " = ?", new String[]{oldId});
                        if (this.database.insertOrThrow(MAIN_TABLE, null, values) == -1)
                            Log.e("Forkize SDK", "New aliased id is not inserted into database");
                    } else {
                        Log.w("Forkize SDK", "Aliasing is queued, don't call alias multiple time if it possible");
                    }
                } else {
                    Log.e("Forkize SDK", "User Id to be aliased is incorrect " + oldId);
                }
            } catch (Exception e) {
                Log.e("Forkize SDK", "Error updating alias in database");
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }

    public synchronized Cursor getAliasedUser(String uid) throws Exception {
        if (this.connectionOpen.get()) {
            Cursor cursor;
            //FZ::TODO
            cursor = this.database.query(MAIN_TABLE, new String[]{COLUMN_UID, COLUMN_ALIAS_ID}, COLUMN_UID + " == '" + uid + "'", null, null, null, null);
            cursor.moveToFirst();

            if (cursor.isLast()) {
                return cursor;
            } else {
                Log.e("Forkize SDK", "Something went wrong getting aliased users id");
            }
        }
        return null;
    }

    public synchronized void exchangeIds(String uid) throws Exception {
        if (this.connectionOpen.get()) {
            Cursor cursor = null;

            try {
                cursor = this.database.query(MAIN_TABLE, null, COLUMN_UID + " == '" + uid + "'", null, null, null, null);
                cursor.moveToFirst();

                if (cursor.isLast()) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_UID, cursor.getString(cursor.getColumnIndex(COLUMN_ALIAS_ID)));
                    values.put(COLUMN_TID, cursor.getString(cursor.getColumnIndex(COLUMN_TID)));
                    values.put(COLUMN_UP, cursor.getString(cursor.getColumnIndex(COLUMN_UP)));
                    values.put(COLUMN_UP_CHANGELOG, cursor.getString(cursor.getColumnIndex(COLUMN_UP_CHANGELOG)));

                    if (this.database.insertOrThrow(MAIN_TABLE, null, values) == -1) {
                        Log.e("Forkize SDK", "User and alias id exchange failed");
                    } else {
                        this.database.delete(MAIN_TABLE, COLUMN_ID + " = ?", new String[]{cursor.getString(cursor.getColumnIndex(COLUMN_ID))});
                        // TODO
//                        ForkizeInstance.getInstance().getUserProfile().identify(ForkizeConfig.getInstance().getApplicationContext(), cursor.getString(cursor.getColumnIndex(COLUMN_ALIAS_ID)));
                        Log.i("Forkize SDK", "User and alias id exchanged successfully");
                    }
                } else {
                    Log.e("Forkize SDK", "Something went wrong getting aliased user id");
                }

            } catch (Exception e) {
                Log.e("Forkize SDK", "Error exchanging users", e);
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
    }
}
