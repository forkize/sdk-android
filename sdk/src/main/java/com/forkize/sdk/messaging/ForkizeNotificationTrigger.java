package com.forkize.sdk.messaging;

import com.forkize.sdk.ForkizeEventManager;
import com.forkize.sdk.ForkizeHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ForkizeNotificationTrigger {

    private static ForkizeNotificationTrigger instance = new ForkizeNotificationTrigger();
    private ArrayList<JSONObject> trigger;
    private HashMap<String, HashSet<String>> state_event;
    private HashSet<String> app_event;
    private Thread timer;
    private String state;
    private long app_stamp;
    private long state_stamp;

    private ForkizeNotificationTrigger() {
        trigger = new ArrayList<>();
        app_event = new HashSet<>();
        state_event = new HashMap<>();
        app_stamp = state_stamp = System.currentTimeMillis();
        timer = new Thread(new NoEventRunnable());
        timer.setDaemon(true); // FZ::TODO::RESEARCH daemon threads
        timer.start();
    }

    protected static ForkizeNotificationTrigger getInstance() {
        return instance;
    }

    protected void reset() {
        state = null;
        trigger.clear();
        app_event.clear();
        state_event.clear();
        app_stamp = state_stamp = System.currentTimeMillis();
    }

    protected void setTriggers(ArrayList<JSONObject> pendingNotifications) {
        for (JSONObject notification : pendingNotifications) {
            setTrigger(notification);
        }
    }

    protected void setTrigger(JSONObject notification) {
        if (trigger == null) {
            trigger = new ArrayList<>();
        }
        JSONObject object = new JSONObject();
        try {
            object.put("_id", notification.getString("_id"));
            object.put("schedule", notification.getJSONObject("scheduling").getJSONObject("schedule"));
            object.put("trigger", notification.getJSONObject("scheduling").getJSONObject("trigger"));
            trigger.add(object);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void processEvent(JSONObject event) {
        _registerEvent(event);
        ArrayList<Integer> remove = new ArrayList<>();
        try {
            for (int i = 0; i < trigger.size(); ++i) {
                if (_timeHasCome(trigger.get(i).getJSONObject("schedule"))) {
                    if (_testEvent(event, trigger.get(i).getJSONObject("trigger"))) {
                        ForkizeNotificationPool.getInstance().makeAvailable(trigger.get(i).getString("_id"));
                        remove.add(i);
                    }
                }
            }
            for (int i = remove.size() - 1; i >= 0; --i) {
                trigger.remove((int) remove.get(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void _registerEvent(JSONObject event) {
        try {
            String eventName = event.getString(ForkizeEventManager.EVENT_TYPE);
            app_event.add(eventName);

            if (!ForkizeHelper.isNullOrEmpty(state)) {
                if (state_event.get(state) == null) {
                    HashSet<String> map = new HashSet<>();
                    state_event.put(state, map);
                }

                state_event.get(state).add(eventName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean _timeHasCome(JSONObject time) throws JSONException {
        long start = time.getLong("start");
        long end = time.getLong("end");
        if (time.getBoolean("timezone")) {
            return start >= System.currentTimeMillis() && System.currentTimeMillis() <= end;
        } else {
            // FZ::TODO convert to user time
            String offset = time.getString("offset");
            return false;
        }
    }

    synchronized private void _notEvent() {
        try {
            ArrayList<Integer> remove = new ArrayList<>();
            for (int i = 0; i < trigger.size(); ++i) {
                if (trigger.get(i).getJSONObject("trigger").getString("type").equals("no_event")){
                    if (_timeHasCome(trigger.get(i).getJSONObject("schedule"))){
                        if (_notDoneEvent(trigger.get(i).getJSONObject("trigger"))){
                            ForkizeNotificationPool.getInstance().makeAvailable(trigger.get(i).getString("_id"));
                            remove.add(i);
                        }
                    }
                }
            }
            for (int i = remove.size() - 1; i >= 0; --i) {
                trigger.remove((int) remove.get(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean _notDoneEvent(JSONObject trigger) throws JSONException {
        String entry = trigger.getString("entry");
        String eventName = trigger.getString("event_name");
        long passedTime = trigger.getInt("duration") * 60 * 1000;

        if (entry.equals("app")) {
            if (System.currentTimeMillis() - app_stamp >= passedTime){
                return !app_event.contains(eventName);
            }
        } else {
            String stateName = trigger.getString("state_name");
            if (System.currentTimeMillis() - state_stamp >= passedTime){
                return state_event.get(stateName) != null && !state_event.get(stateName).contains(eventName);
            }
        }
        return false;
    }

    private boolean _testEvent(JSONObject event, JSONObject trigger) throws JSONException {
        String type = trigger.getString("type");
        String eventName = event.getString(ForkizeEventManager.EVENT_TYPE);

        if (type.equals("app")) {
            return eventName.equals(ForkizeEventManager.SESSION_START);
        }

        if (type.equals("state")) {
            String stateName = trigger.getString("state_name");
            return eventName.equals(ForkizeEventManager.STATE_ADVANCE) && stateName.equals(event.getString(ForkizeEventManager.STATE_NEXT));
        }

        if (type.equals("event")) {
            return eventName.equals(trigger.getString("event_name"));
        }

        if (type.equals("url")) {
            return false;
        }
        return false;
    }

    private class NoEventRunnable implements Runnable {
        @Override
        public void run() {

        }
    }
}
