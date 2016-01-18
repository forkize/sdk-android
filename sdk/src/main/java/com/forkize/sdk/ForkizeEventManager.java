package com.forkize.sdk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.forkize.sdk.localstorage.EventStorage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ForkizeEventManager {

    // FZ::TODO is ot okay to have public constants ?
    public static final String PREFIX = "$";

    public static final String EVENT_TYPE = "evn";
    public static final String EVENT_DATA = "evd";
    public static final String EVENT_SESSION_ID = "sid";
    public static final String EVENT_UTC_TIME = "ev_utc";
    public static final String EVENT_SESSION_TIME = "ev_moment";

    public static final String EVENT_DURATION = "ev_duration";
    public static final String EVENT_STATE = "ev_state";
    public static final String EVENT_STATE_TIME = "ev_state_moment";

    public static final String APP_VERSION = "app_version";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String CONNECTION_TYPE = "connection";
    public static final String BATTERY_LEVEL = "battery";

    public static final String SESSION_START = "fz.session.start";
    public static final String SESSION_END = "fz.session.end";
    public static final String SESSION_LENGTH = "session_length";

    public static final String STATE_PREVIOUS = "$state_prev";
    public static final String STATE_NEXT = "$state_next";
    public static final String STATE_DURATION = "$state_duration";
    public static final String STATE_ADVANCE = "$state_advance";

    private EventStorage localStorage;

    private ExecutorService localStorageExecutor;

    private ConnectivityManager connectivityManager;
    private Map<String, Long> scheduledEvents;
    private Map<String, Object> superProperties;
    private AtomicBoolean initialized;

    private String state;
    private long state_stamp;
    private double latitude;
    private double longitude;

    private static ForkizeEventManager instance;

    protected ForkizeEventManager() {
        this.initialized = new AtomicBoolean(false);
        this.localStorageExecutor = Executors.newSingleThreadExecutor();
    }

    public static ForkizeEventManager getInstance() {
        if (instance == null) {
            instance = new ForkizeEventManager();
        }

        return instance;
    }

    public void setState(String stateArg) {
        if (!ForkizeHelper.isNullOrEmpty(this.state)) {
            Map<String, Object> map = new HashMap<>();
            long stateDuration = System.currentTimeMillis() - this.state_stamp;
            map.put(STATE_PREVIOUS, this.state);
            map.put(STATE_NEXT, stateArg);
            map.put(STATE_DURATION, stateDuration);
            this.queueEvent(STATE_ADVANCE, map);
        }
        this.state = stateArg;
        this.state_stamp = System.currentTimeMillis();
    }

    public void resetState() {
        if (!ForkizeHelper.isNullOrEmpty(this.state)) {
            Map<String, Object> map = new HashMap<>();
            long stateDuration = System.currentTimeMillis() - this.state_stamp;
            map.put(STATE_PREVIOUS, this.state);
            map.put(STATE_NEXT, "");
            map.put(STATE_DURATION, stateDuration);
            this.queueEvent(STATE_ADVANCE, map);
        }
        this.state = null;
        this.state_stamp = 0;
    }

    public String[] getEvents(int count) {
        if (this.initialized.get()) {
            return this.localStorage.getEvents(count);
        }
        return null;
    }

    public void removeEvents(int count) {
        if (this.initialized.get()) {
            this.localStorage.removeEvents(count);
        }
    }

    protected void flushCacheToDatabase() {
        if (this.initialized.get()) {
            this.localStorage.flushCacheToDatabase();
        }
    }

    protected void setContext(Context context) {
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
    }

    protected void eventDuration(String eventName) {
        if (scheduledEvents == null) {
            scheduledEvents = new HashMap<>();
        }

        scheduledEvents.put(eventName, System.currentTimeMillis());
    }

    protected void setSuperProperties(Map<String, Object> properties) {
        if (superProperties == null) {
            superProperties = new HashMap<>();
        }

        for (Object o : properties.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            if (ForkizeHelper.isKeyValid((String) entry.getKey())) {
                this.superProperties.put((String) entry.getKey(), entry.getValue());
            }
        }
    }

    protected void setSuperPropertiesOnce(Map<String, Object> properties) {
        if (superProperties == null) {
            superProperties = new HashMap<>();
        }

        for (Object o : properties.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            if (ForkizeHelper.isKeyValid((String) entry.getKey()) && this.superProperties.get(entry.getKey()) == null) {
                this.superProperties.put((String) entry.getKey(), entry.getValue());
            }
        }
    }

    protected void queueSessionStart() {
        queueEvent(SESSION_START, null);
    }

    protected void queueSessionEnd() {
        Map<String, Object> map = new HashMap<>();
        map.put(SESSION_LENGTH, SessionInstance.getInstance().getSessionLength());
        queueEvent(SESSION_END, map);
    }

    protected void queueEvent(String event, Map<String, Object> parameters) {
        if (!this.initialized.get()) {
            this.localStorage = (EventStorage) StorageFactory.getInstance().getStorage("EVENT");
            this.initialized.set(true);
        }
        _queueEvent(event, parameters);
    }

    private void _queueEvent(String event, Map<String, Object> parameters) {
        try {
            storageExecutorExecute(new QueueEventRunnable(event, parameters));
        } catch (Exception e) {
            Log.e("Forkize SDK", "Error when queue event", e);
        }
    }

    protected void close() throws InterruptedException {
        this.localStorageExecutor.shutdown();
        this.localStorageExecutor.awaitTermination(5L, TimeUnit.SECONDS);
    }

    private boolean storageExecutorExecute(Runnable runnable) {
        try {
            if (localStorageExecutor.isShutdown()) {
                Log.i("Forkize SDK", "Trying to schedule a storage execution while shutdown");
            } else {
                localStorageExecutor.execute(runnable);
                return true;
            }
        } catch (Exception e) {
            Log.e("Forkize SDK", "Error while scheduling a storage execution", e);
        }
        return false;
    }

    private String _eventAsJSON(String event, Map<String, Object> parameters) throws JSONException {
        JSONObject object = new JSONObject();
        JSONObject event_data = new JSONObject();

        object.put(EVENT_TYPE, event);
        object.put(EVENT_SESSION_ID, SessionInstance.getInstance().getSessionId());

        // FZ::TODO  currentTimeMillis ? which timezone
        event_data.put(PREFIX + EVENT_UTC_TIME, System.currentTimeMillis());

        if (this.scheduledEvents != null) {
            Long t = this.scheduledEvents.get(event);
            if (t != null) {
                event_data.put(PREFIX + EVENT_DURATION, System.currentTimeMillis() - t);
                this.scheduledEvents.remove(event);
            }
        }

        if (!ForkizeHelper.isNullOrEmpty(this.state)) {
            event_data.put(PREFIX + EVENT_STATE, this.state);
            event_data.put(PREFIX + EVENT_STATE_TIME, System.currentTimeMillis() - this.state_stamp);
        }

        event_data.put(PREFIX + EVENT_SESSION_TIME, System.currentTimeMillis() - SessionInstance.getInstance().getSessionStartTime());

        latitude = LocationInstance.getInstance().getLatitude();
        longitude = LocationInstance.getInstance().getLongitude();

        // FZ::POINT
        if (latitude != 1000 && longitude != 1000) {
            event_data.put(PREFIX + LONGITUDE, this.longitude);
            event_data.put(PREFIX + LATITUDE, this.latitude);
        }

        if (!ForkizeHelper.isNullOrEmpty(DeviceInfo.getInstance().getAppVersion())) {
            event_data.put(PREFIX + APP_VERSION, DeviceInfo.getInstance().getAppVersion());
        }

        if (DeviceInfo.getInstance().getBatteryLevel() != -1) {
            event_data.put(PREFIX + BATTERY_LEVEL, DeviceInfo.getInstance().getBatteryLevel());
        }


        if (this.connectivityManager != null) {
            NetworkInfo activeNetwork = this.connectivityManager.getActiveNetworkInfo();

            String type = "";
            boolean isCon = (activeNetwork != null) && activeNetwork.isConnected();
            if (isCon) {
                switch (activeNetwork.getType()) {
                    case ConnectivityManager.TYPE_ETHERNET:
                        type = "ethernet";
                        break;
                    case ConnectivityManager.TYPE_MOBILE:
                        type = "mobile";
                        break;
                    case ConnectivityManager.TYPE_WIFI:
                        type = "wifi";
                        break;
                    default:
                        type = "unknown";
                }
            }

            if (!type.equals("unknown")) {
                event_data.put(PREFIX + CONNECTION_TYPE, type);
            }
        }

        if (parameters != null) {
            for (Object o : parameters.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                if (ForkizeHelper.isKeyValid((String) entry.getKey())) {
                    event_data.put((String) entry.getKey(), entry.getValue());
                }
            }
        }

        if (this.superProperties != null) {
            for (Object o : superProperties.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                event_data.put((String) entry.getKey(), entry.getValue());
            }
        }

        object.put(EVENT_DATA, event_data);
        Log.e("FZ", "---PROCESS_EVENT---");
        ForkizeInstance.getInstance().getUserProfile().processEvent(object);

        return object.toString();
    }

    private class QueueEventRunnable implements Runnable {
        private String event;
        private Map<String, Object> parameters;

        public QueueEventRunnable(String event, Map<String, Object> parameters) {
            this.event = event;
            this.parameters = parameters;
        }

        @Override
        public void run() {
            String eventString;

            try {
                eventString = _eventAsJSON(event, parameters);
                localStorage.addEvent(eventString);
                parameters = null;

                Log.i("Forkize SDK", event + " event queued");

            } catch (JSONException e) {
                Log.e("Forkize SDK", "parameter or payload are not convertible to JSON", e);
            } catch (Exception e) {
                Log.e("Forkize SDK", "Unable to insert into local storage", e);
            }
        }
    }
}