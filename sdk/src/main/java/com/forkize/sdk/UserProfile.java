package com.forkize.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.forkize.sdk.localstorage.UserProfileStorage;
import com.forkize.sdk.messaging.ForkizeNotification;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserProfile {

    // FZ::TODO negotiate with backend
    private static final String USER_ID = "user_id";
    private static final String INCREMENT = "increment";
    private static final String SET = "set";
    private static final String SET_ONCE = "set_once";
    private static final String UNSET = "unset";
    private static final String APPEND = "append";
    private static final String PREPEND = "prepend";

    private String userId, tableName = "dump";
    private boolean isMale, isFemale;
    private int age, aliased;

    private long profileVersion;
    private JSONObject userInfo;
    private JSONObject changeLog;
    private Context context;
    private AtomicBoolean initialised;

    private ForkizeNotification notification;
    private UserProfileStorage localStorage;

    private static UserProfile instance;

    protected UserProfile() {
        isMale = isFemale = false;
        this.userInfo = new JSONObject();
        this.changeLog = new JSONObject();
        this.initialised = new AtomicBoolean(false);
        this.notification = new ForkizeNotification();
    }

    protected static UserProfile getInstance() {
        if (instance == null) {
            instance = new UserProfile();
        }

        return instance;
    }

    private void _init() {
        this.localStorage = (UserProfileStorage) StorageFactory.getInstance().getStorage("USER");
        this.initialised.set(true);
    }

    public void showMessage(){
        this.notification.showNotification(userInfo);
    }

    public void loadMessages(){
        this.notification.loadNotifications();
    }

    public void addMessage(String message){
        this.notification.addNotification(message);
    }

    public void saveMessages(){
        this.notification.saveNotifications();
    }

    protected void processEvent(JSONObject object){
        this.notification.processEvent(object);
    }

    public JSONObject getCampaignHashArray(){
        return this.notification.getNotificationHashArray();
    }

    public JSONObject getTemplateHashArray(){
        return this.notification.getTemplateHashArray();
    }

    public void setActivity(Activity activity){
        this.notification.setActivity(activity);
    }

    public void releaseActivity(){
        this.notification.releaseActivity();
    }

    public long getProfileVersion() {
        return this.profileVersion;
    }

    public void setProfileVersion(long profileVersion) {
        if (profileVersion > 0) {
            this.profileVersion = profileVersion;
        }
    }

    public int getAliased() {
        return this.aliased;
    }

    public void setAliased(int val) {
        this.aliased = val;
    }

    public void alias(String oldUserId, String newUserId) {
        if (!this.initialised.get()) {
            _init();
        }

        if (ForkizeHelper.isNullOrEmpty(newUserId)) {
            Log.e("Forkize SDK", "New user Id is null");
            return;
        }

        if (oldUserId.equals(newUserId)) {
            Log.e("Forkize SDK", "Current and alias user ids are same!");
            return;
        }

        try {
            this.localStorage.alias(oldUserId, newUserId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.aliased = 1;
        // FZ::TODO update userInfo in shared prefs
    }

    public void updateProfile(Map<String, Object> map) {
        for (Object o : map.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            this.set((String) entry.getKey(), entry.getValue());
        }
    }

    public void setProfile(String profile, boolean override) {
        if (!override) {
            try {
                this.userInfo = new JSONObject(profile);
            } catch (JSONException e) {
                Log.e("Forkize SDK", "Given profile is not a JSONObject");
            }
        } else {
            // TODO::::::::
        }
    }

    private Object _parse(Object value) {
        if (value instanceof Map)
            return new JSONObject((Map) value);
        JSONArray array = new JSONArray();
        if (value instanceof Collection) {
            for (Object object : (Collection) value) {
                if (object instanceof Collection)
                    array.put(_parse(object));
                else
                    array.put(object);
            }
        }
        return (array.length() == 0) ? value : array;
    }

    private JSONArray _getWithoutChosen(JSONArray array, int index) {
        JSONArray newArray = null;

        try {
            newArray = new JSONArray();
            for (int i = 0; i < array.length(); ++i) {
                if (i != index) {
                    newArray.put(array.get(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return newArray;
    }

    private void _processValue(String actionKey, String key) {
        if (this.changeLog.has(actionKey)) {
            try {

                if (actionKey.equals(UNSET)) {
                    JSONArray array = this.changeLog.getJSONArray(UNSET);
                    for (int i = 0; i < array.length(); ++i) {
                        String string = array.getString(i);
                        if (string.equals(key)) {
                            this.changeLog.put(actionKey, _getWithoutChosen(array, i));
                            break;
                        }
                    }
                } else {
                    this.changeLog.getJSONObject(actionKey).remove(key);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean _containsInList(String actionKey, String key) {
        if (this.changeLog.has(actionKey)) {
            try {
                if (actionKey.equals(UNSET)) {
                    JSONArray array = this.changeLog.getJSONArray(UNSET);
                    for (int i = 0; i < array.length(); ++i) {
                        String string = array.getString(i);
                        if (string.equals(key)) {
                            return true;
                        }
                    }
                } else {
                    return this.changeLog.getJSONObject(actionKey).has(key);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private Object _validate(Object value) {
        if (value instanceof Map || value instanceof Collection || value instanceof JSONArray) {
            return null;
        }
        return value;
    }

    public void setOnce(String key, Object value) {
        value = _validate(value);

        if (ForkizeHelper.isKeyValid(key) && value != null) {
            if (!this.userInfo.has(key)) {
                _set(key, value, true);
            }
        }
    }

    public void set(String key, Object value) {
        value = _validate(value);

        if (ForkizeHelper.isKeyValid(key) && value != null) {

            try {
                this.userInfo.put(key, value);
                _set(key, value, false);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void setBatch(Map<String, Object> map) {
        for (Object o : map.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            this.set((String) entry.getKey(), entry.getValue());
        }
    }

    private void _set(String key, Object value, boolean once) {
        if (once) {

            try {
                if (this.changeLog.has(SET_ONCE)) {
                    if (this.changeLog.getJSONObject(SET_ONCE).has(key))
                        return;
                } else {
                    JSONObject object = new JSONObject();
                    this.changeLog.put(SET_ONCE, object);
                }

                if (this.changeLog.has(SET)) {
                    if (this.changeLog.getJSONObject(SET).has(key))
                        return;
                }

                if (this.changeLog.has(INCREMENT)) {
                    if (this.changeLog.getJSONObject(INCREMENT).has(key))
                        return;
                }

                if (this.changeLog.has(APPEND)) {
                    if (this.changeLog.getJSONObject(APPEND).has(key))
                        return;
                }

                if (this.changeLog.has(PREPEND)) {
                    if (this.changeLog.getJSONObject(PREPEND).has(key))
                        return;
                }

                this.changeLog.getJSONObject(SET_ONCE).put(key, value);

                _processValue(UNSET, key);
            } catch (JSONException e) {
                e.printStackTrace();
            }


        } else {
            try {
                if (!this.changeLog.has(SET)) {
                    JSONObject object = new JSONObject();
                    this.changeLog.put(SET, object);
                }

                this.changeLog.getJSONObject(SET).put(key, value);

                _processValue(SET_ONCE, key);
                _processValue(INCREMENT, key);
                _processValue(APPEND, key);
                _processValue(PREPEND, key);
                _processValue(UNSET, key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void unset(String key) {
        if (ForkizeHelper.isKeyValid(key)) {
            try {
                this.userInfo.remove(key);

                if (!this.changeLog.has(UNSET)) {
                    JSONArray array = new JSONArray();
                    this.changeLog.put(UNSET, array);
                }

                this.changeLog.getJSONArray(UNSET).put(key);

                _processValue(SET, key);
                _processValue(INCREMENT, key);
                _processValue(APPEND, key);
                _processValue(PREPEND, key);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void unsetBatch(JSONArray array) {
        for (int i = 0; i < array.length(); ++i) {
            this.unset(array.optString(i));
        }
    }

    public void increment(String key, Double value) {
        if (ForkizeHelper.isKeyValid(key)) {
            Double d;

            try {
                d = this.userInfo.optDouble(key, 0);
                d += value;
                this.userInfo.put(key, d);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                if (!this.changeLog.has(INCREMENT)) {

                    JSONObject object = new JSONObject();
                    object.put(key, value);

                    this.changeLog.put(INCREMENT, object);
                    return;
                }

                d = this.changeLog.getJSONObject(INCREMENT).optDouble(key, 0);
                d += value;

                this.changeLog.getJSONObject(INCREMENT).put(key, d);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void increment(String key, Integer value) {
        if (ForkizeHelper.isKeyValid(key)) {
            Integer d;

            try {
                d = this.userInfo.optInt(key, 0);
                d += value;
                this.userInfo.put(key, d);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                if (!this.changeLog.has(INCREMENT)) {

                    JSONObject object = new JSONObject();
                    object.put(key, value);

                    this.changeLog.put(INCREMENT, object);
                    return;
                }

                d = this.changeLog.getJSONObject(INCREMENT).optInt(key, 0);
                d += value;

                this.changeLog.getJSONObject(INCREMENT).put(key, d);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void _inc(String key, Object value) {
        if (_containsInList(UNSET, key)) {
            Log.e("Forkize SDK", "Error incrementing the value of unset key");
            return;
        }
        if (_containsInList(APPEND, key) || _containsInList(PREPEND, key)) {
            Log.e("Forkize SDK", "Error incrementing the value of list");
            return;
        }

        if (value instanceof Integer)
            this.increment(key, (Integer) value);
        else if (value instanceof Double)
            this.increment(key, (Double) value);
    }

    public void incrementBatch(Map<String, Object> map) {
        for (Object o : map.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            this._inc((String) entry.getKey(), entry.getValue());
        }
    }

    // FZ::TODO test it !

    public void append(String key, Object value) {
        if (ForkizeHelper.isKeyValid(key)) {

            try {
                JSONArray array = null;
                if (value instanceof JSONArray) {
                    array = (JSONArray) value;
                }
                value = _validate(value);

                JSONArray mArray = this.userInfo.optJSONArray(key);
                if (mArray == null) {
                    mArray = new JSONArray();
                }

                if (array != null) {
                    for (int i = 0; i < array.length(); ++i) {
                        Object object = _validate(array.get(i));
                        if (object != null) {
                            mArray.put(object);
                        }
                    }
                    this.userInfo.put(key, mArray);
                    _pend(APPEND, key, array);
                }

                if (value != null) {
                    mArray.put(value);
                    this.userInfo.put(key, mArray);
                    _pend(APPEND, key, value);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void prepend(String key, Object value) {
        if (ForkizeHelper.isKeyValid(key)) {

            try {
                JSONArray array = null;
                if (value instanceof JSONArray) {
                    array = (JSONArray) value;
                }
                value = _validate(value);

                JSONArray mArray = this.userInfo.optJSONArray(key);
                if (mArray == null) {
                    mArray = new JSONArray();
                }

                if (array != null) {
                    JSONArray tArray = new JSONArray();
                    for (int i = array.length() - 1; i >= 0; --i) {
                        tArray.put(array.get(i));
                    }

                    for (int i = 0; i < mArray.length(); ++i) {
                        tArray.put(mArray.get(i));
                    }
                    this.userInfo.put(key, tArray);
                    _pend(PREPEND, key, array);
                }

                if (value != null) {
                    JSONArray tArray = new JSONArray();
                    tArray.put(value);
                    for (int i = 0; i < mArray.length(); ++i)
                        tArray.put(mArray.get(i));
                    this.userInfo.put(key, tArray);
                    _pend(PREPEND, key, value);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void _pend(String action, String key, Object value) {
        Log.e("Forkize SDK", "Pend called with action:  " + action);
        try {
            if (!this.changeLog.has(action)) {
                JSONObject object = new JSONObject();
                object.put(key, value);
                this.changeLog.put(action, object);
                return;
            }

            Log.e("Forkize SDK", value.toString());
            if (value instanceof JSONArray) {
                for (int i = 0; i < ((JSONArray) value).length(); ++i) {
                    Log.e("Forkize SDK", "APPENDING:::" + action + " --- " + ((JSONArray) value).get(i));
                    this.changeLog.getJSONObject(action).getJSONArray(key).put(((JSONArray) value).get(i));
                }
            } else {
                if (this.changeLog.getJSONObject(action).has(key)) {
                    this.changeLog.getJSONObject(action).getJSONArray(key).put(value);
                } else {
                    JSONArray array = new JSONArray();
                    array.put(value);
                    this.changeLog.getJSONObject(action).put(key, array);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setAge(int age) {
        if (age > 0 && age < 100) {
            this.age = age;
            this.set("age", age);
        } else
            Log.e("Forkize SDK", "Entered wrong value for age");
    }

    public int getAge() {
        try {
            return this.userInfo.getInt("age");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void isMale(boolean b) {
        if (b) {
            isMale = true;
            isFemale = false;
            this.set("$gender", "male");
        }
    }

    public void isFemale(boolean b) {
        if (b) {
            isMale = false;
            isFemale = true;
            this.set("$gender", "female");
        }
    }

    public String getGender() {
        try {
            return this.userInfo.getString("$gender");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "undefined";
    }

    public void setProfileForSession(String profile) {
        try {
            this.userInfo = new JSONObject(profile);
        } catch (JSONException e) {
            Log.e("Forkize SDK", "User Info that you've provided could not convert into JSONObject");
        }
    }

    public void saveUserProfile() {
        SharedPreferences settings = this.context.getSharedPreferences("forkize_prefs", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(this.userId, this.userInfo.toString());
        editor.apply();

        Log.i("Forkize SDK", "User profile saved successfully");
    }

    public void identify(Activity context, String uid) {
        if (!this.initialised.get()) {
            _init();
        }

        this.notification.setActivity(context);
        this.context = context;
        this.aliased = 0;
        String oldId = this.userId;

        String tmpUserId = (ForkizeHelper.isNullOrEmpty(uid)) ? _generateUserID() : uid;
        _checkUserId(tmpUserId);

        SharedPreferences settings = context.getSharedPreferences("forkize_prefs", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(USER_ID, tmpUserId);

        if (!ForkizeHelper.isNullOrEmpty(oldId) && this.userInfo.length() > 0) {
            editor.putString(oldId, this.userInfo.toString());
            editor.putLong(oldId + "upv", this.profileVersion);
        }
        editor.apply();

        this.profileVersion = settings.getLong(tmpUserId + "upv", 0);

        Log.e("Forkize SDK", "Profile version from disk " + String.valueOf(profileVersion));
        this.userId = tmpUserId;

        // TODO::REVIEW
        ForkizeEventManager.getInstance().flushCacheToDatabase();
        try {

            this.tableName = this.localStorage.changeUserId();

            if (!ForkizeHelper.isNullOrEmpty(oldId)) {
                this.localStorage.writeUserProfileInDB(oldId, this.userInfo.toString());
                this.localStorage.writeUserProfileChangeLogInDB(oldId, this.changeLog.toString());
            }

            String userProfileString = this.localStorage.getUserProfileFromDB(this.userId);
            if (!ForkizeHelper.isNullOrEmpty(userProfileString)) {
                this.userInfo = new JSONObject(userProfileString);
            } else {
                this.userInfo = new JSONObject();
            }

            String changeLogString = this.localStorage.getUserProfileChangeLogFromDB(this.userId);
            if (!ForkizeHelper.isNullOrEmpty(changeLogString)) {
                this.changeLog = new JSONObject(changeLogString);
            } else {
                this.changeLog = new JSONObject();
            }

            Log.w("Forkize SDK", this.userInfo.toString());
            Log.w("Forkize SDK", this.changeLog.toString());

        } catch (JSONException e) {
            Log.e("Forkize SDK", "User change log is not converted to JSONObject");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkAliasState() {
        if (!this.initialised.get()) {
            return false;
        }

        try {
            Cursor cursor = this.localStorage.getAliasedUser(this.userId);
            if (cursor == null) {
                Log.e("Forkize SDK", "User with given Id have not been discovered");
                return false;
            } else {
                cursor.moveToFirst();

                if (cursor.isLast() && !ForkizeHelper.isNullOrEmpty(cursor.getString(1))) {
                    this.aliased = 1;
                    return true;
                }
                this.aliased = 2;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateAlias() {
        try {
            this.localStorage.exchangeIds(this.userId);
            this.aliased = 2;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Cursor getAliasCursor() {
        Cursor cursor = null;
        try {
            cursor = this.localStorage.getAliasedUser(this.userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cursor;
    }

    public String getChangeLog() {
        return this.changeLog.toString();
    }

    public void dropChangeLog() {
        this.changeLog = new JSONObject();
    }

    public void printChangeLog() {
        Log.e("Forkize SDK", this.changeLog.toString());
        Log.e("Forkize SDK", this.userInfo.toString());
    }

    public void flushToDatabase() {
        if (this.localStorage != null) {
            try {
                this.localStorage.writeUserProfileInDB(this.userId, this.userInfo.toString());
                this.localStorage.writeUserProfileChangeLogInDB(this.userId, this.changeLog.toString());

                Log.e("Forkize SDK", "Saving Version to disk " + String.valueOf(profileVersion));
                SharedPreferences settings = context.getSharedPreferences("forkize_prefs", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putLong(this.userId + "upv", this.profileVersion);
                editor.apply();
            } catch (Exception e) {
                Log.e("Forkize SDK", "Something went wrong putting UserInfo to database");
            }
        }
    }

    public JSONObject getUserInfo() {
        if (this.userInfo == null) {
            this.userInfo = new JSONObject();

            if (this.age > 0 && this.age < 100)
                try {
                    this.userInfo.put("age", this.age);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            if (!ForkizeHelper.isNullOrEmpty(this.getGender()))
                try {
                    this.userInfo.put("gender", this.getGender());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }
        return this.userInfo;
    }

    public String getUserId(Activity context) {

        if (ForkizeHelper.isNullOrEmpty(this.userId)) {

            SharedPreferences preferences = context.getSharedPreferences("forkize_prefs", 0);
            String prefUserId = preferences.getString(USER_ID, null);

            if (ForkizeHelper.isNullOrEmpty(prefUserId)) {
                identify(context, null);
            } else {
                this.userId = prefUserId;
            }
        }
        return this.userId;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getTableName() {
        return this.tableName;
    }

    protected boolean isNewInstall(Context context) {
        boolean isNewInstall = false;

        SharedPreferences preferences = context.getSharedPreferences("forkize_prefs", 0);
        try {
            String timeRaw = preferences.getString("forkize.install.time", null);
            if (ForkizeHelper.isNullOrEmpty(timeRaw)) {
                long time = (new Date()).getTime();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("forkize.install.time", String.valueOf(time)); // FZ::TODO check
                editor.apply();
                isNewInstall = true;
            }
        } catch (Exception e) {
            Log.e("Forkize SDK", "Could not get or set Install time ", e);
        }

        return isNewInstall;
    }

    private void _checkUserId(String userId) throws IllegalArgumentException {
        if ((userId != null) && (userId.matches("^.*\\..*@\\w+$"))) {
            Log.w("Forkize SDK", "Please double-check your user id. It seems to be Object.toString(): " + userId);
        }
    }

    // FZ::TODO Maybe we'll use some hash function like murmur or cHash
    private String _generateUserID() {
        return UUID.randomUUID().toString();
    }
}