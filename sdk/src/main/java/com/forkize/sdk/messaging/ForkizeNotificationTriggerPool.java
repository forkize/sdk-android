package com.forkize.sdk.messaging;

import android.support.annotation.NonNull;
import android.util.Log;

import com.forkize.sdk.ForkizeEventManager;
import com.forkize.sdk.ForkizeHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ForkizeNotificationTriggerPool {
    private static ForkizeNotificationTriggerPool instance = new ForkizeNotificationTriggerPool();

    private ArrayList<Trigger> eventTriggers;
    private ArrayList<Trigger> noEventTriggers;
    private HashMap<String, Integer> app_events;
    private HashMap<String, HashMap<String, Integer>> state_events;

    // FZ::TODO save the state we in, when notification needed to show, give higher priority to them which are in current state

    private ForkizeNotificationTriggerPool() {
        eventTriggers = new ArrayList<>();
        noEventTriggers = new ArrayList<>();
        app_events = new HashMap<>();
        state_events = new HashMap<>();
    }

    protected static ForkizeNotificationTriggerPool getInstance() {
        return instance;
    }

    protected void reset() {
        eventTriggers.clear();
        noEventTriggers.clear();
        app_events.clear();
        state_events.clear();
    }

    protected void setTriggers(ArrayList<JSONObject> campaignList) {
        for (JSONObject campaign : campaignList) {
            setTrigger(campaign);
        }
        Log.e("FZ", "Triggers are set!!!\nCount of triggers is " + String.valueOf(eventTriggers.size()));
    }

    protected void setTrigger(JSONObject campaign) {
        try {
            Trigger trigger;
            String id = campaign.getString("_id");
            JSONObject scheduling = campaign.getJSONObject("scheduling");
            long start = scheduling.getJSONObject("schedule").getLong("start");
            long end = scheduling.getJSONObject("schedule").getLong("end");

            String type = scheduling.getJSONObject("trigger").getString("type");
            String eventName = "";
            String property = "";

            switch (type) {
                case "app":
                    eventName = ForkizeEventManager.SESSION_START;
                    break;
                case "state":
                    eventName = ForkizeEventManager.PREFIX + ForkizeEventManager.STATE_ADVANCE;
                    property = scheduling.getJSONObject("trigger").getString("state_name");
                    break;
                case "event":
                    eventName = scheduling.getJSONObject("trigger").getString("event_name");
                    break;
                case "no_event":
                    eventName = scheduling.getJSONObject("trigger").getString("event_name");
                    break;
            }

            if (type.equals("no_event")) {
                trigger = new NoEventTrigger(id, eventName, start, end);
            } else {
                trigger = new EventTrigger(id, eventName, start, end);
            }

            if (scheduling.getJSONObject("schedule").getBoolean("timezone")) {
                trigger.setTimezone(true, scheduling.getJSONObject("schedule").getString("offset"));
            } else {
                trigger.setTimezone(false, null);
            }

            Log.e("Forkize SDK", "EVENT TYPE :::---> \t" + type);
            if (type.equals("no_event")) {
                String entry = scheduling.getJSONObject("trigger").getString("entry");
                ((NoEventTrigger) trigger).setEntry(entry);
                if (entry.equals("state")) {
                    ((NoEventTrigger) trigger).setStateName(scheduling.getJSONObject("trigger").getString("state_name"));
                }
                noEventTriggers.add(trigger);
            } else {
                ((EventTrigger) trigger).setProperty(property);
                eventTriggers.add(trigger);
                Log.e("Forkize SDK", eventTriggers.toString() + " ~~~ " + String.valueOf(eventTriggers.size()));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void registerEvent(JSONObject event) {
        try {
            Log.e("FZ", "---REGISTER_EVENT---");
            Log.e("Forkize SDK", event.toString());

            String eventName = event.getString(ForkizeEventManager.EVENT_TYPE);
            if (app_events.containsKey(eventName)) {
                app_events.put(eventName, app_events.get(eventName) + 1);
            } else {
                app_events.put(eventName, 0);
            }

            String stateName = event.optString(ForkizeEventManager.PREFIX + ForkizeEventManager.EVENT_STATE);
            if (!ForkizeHelper.isNullOrEmpty(stateName)) {
                if (state_events.containsKey(stateName)) {
                    if (state_events.get(stateName).containsKey(eventName)) {
                        state_events.get(stateName).put(eventName, state_events.get(stateName).get(eventName) + 1);
                    } else {
                        state_events.get(stateName).put(eventName, 0);
                    }
                } else {
                    HashMap<String, Integer> temp = new HashMap<>();
                    temp.put(eventName, 0);
                    state_events.put(stateName, temp);
                }
            }

            Log.e("Forkize SDK", eventTriggers.toString() + " ~~~ " + String.valueOf(eventTriggers.size()));

            ArrayList<Trigger> scheduled = _getScheduled(true);
            HashSet<String> removable = new HashSet<>();

            Log.e("Forkize SDK", "Scheduled length " + String.valueOf(scheduled.size()));
            for (Trigger trigger : scheduled) {
                if (trigger.satisfy(event)) {
                    Log.e("Forkize SDK", "SATISFIED!!!");
                    ForkizeNotificationPool.getInstance().makeAvailable(trigger.getId());
                    removable.add(trigger.getId());
                }
            }
            _removeTriggers(removable, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void prepare() {
        ArrayList<String> ids = _getNoEvents();
        for (String id : ids) {
            ForkizeNotificationPool.getInstance().makeAvailable(id);
        }
    }

    private ArrayList<String> _getNoEvents() {
        ArrayList<String> list = new ArrayList<>();
        ArrayList<Trigger> scheduled = _getScheduled(false);
        HashSet<String> removable = new HashSet<>();

        for (Trigger scheduledTrigger : scheduled) {
            NoEventTrigger trigger = (NoEventTrigger) scheduledTrigger;

            if (trigger.satisfy(null)) {
                list.add(trigger.getId());
                removable.add(trigger.getId());
            }
        }

        _removeTriggers(removable, false);
        return list;
    }

    private ArrayList<Trigger> _getScheduled(boolean isEvent) {
        ArrayList<Trigger> list = new ArrayList<>();
        if (isEvent) {
            Log.e("Forkize SDK", String.valueOf(eventTriggers.size()));
            for (Trigger trigger : eventTriggers) {
                if (trigger.isProperTime()) {
                    list.add(trigger);
                    Log.e("Forkize SDK", "ADDED TO SCHEDULED TRIGGERS");
                }
            }
        } else {
            for (Trigger trigger : noEventTriggers) {
                if (trigger.isProperTime()) {
                    list.add(trigger);
                }
            }
        }
        Log.e("Forkize SDK", "TRIGGER_LIST :::---> " + list.toString());
        return list;
    }

    private void _removeTriggers(HashSet<String> ids, boolean isEvent) {
        if (isEvent) {
            for (int i = eventTriggers.size() - 1; i >= 0; --i) {
                if (ids.contains(eventTriggers.get(i).getId())) {
                    eventTriggers.remove(i);
                }
            }
        } else {
            for (int i = noEventTriggers.size() - 1; i >= 0; --i) {
                if (ids.contains(noEventTriggers.get(i).getId())) {
                    noEventTriggers.remove(i);
                }
            }
        }
    }

    private abstract class Trigger {

        protected String id, eventName;
        protected long startStamp, endStamp;
        protected boolean isTimeZoned;
        protected String timeZone;

        protected Trigger(@NonNull String id, @NonNull String eventName, long startStamp, long endStamp) {
            this.id = id;
            this.eventName = eventName;
            this.startStamp = startStamp;
            this.endStamp = endStamp;
        }

        abstract boolean satisfy(JSONObject event);

        protected boolean isProperTime() {
            // FZ:::TODO
            return true;
        }

        protected void setTimezone(boolean isTimeZoned, String timeZone) {
            this.isTimeZoned = isTimeZoned;
            this.timeZone = timeZone;
        }

        public String getId() {
            return id;
        }

        public String getEventName() {
            return eventName;
        }
    }

    private class EventTrigger extends Trigger {
        private String property;

        public EventTrigger(@NonNull String id, @NonNull String eventName, long startStamp, long endStamp) {
            super(id, eventName, startStamp, endStamp);
        }

        @Override
        boolean satisfy(JSONObject event) {
            Log.e("Forkize SDK", "trigger.satisfy");
            try {
                String eventName = event.getString(ForkizeEventManager.EVENT_TYPE);
                if (eventName.equals(ForkizeEventManager.STATE_ADVANCE)) {
                    if (property.equals(event.getString(ForkizeEventManager.STATE_NEXT))) {
                        return true;
                    }
                } else {
                    return this.eventName.equals(eventName);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }

        public void setProperty(String property) {
            this.property = property;
        }
    }

    private class NoEventTrigger extends Trigger {
        private String entry;
        private String stateName;

        public NoEventTrigger(@NonNull String id, @NonNull String eventName, long startStamp, long endStamp) {
            super(id, eventName, startStamp, endStamp);
        }

        @Override
        boolean satisfy(JSONObject event) {
            if (entry.equals("app")) {
                return !app_events.containsKey(eventName);
            } else {
                return !state_events.containsKey(stateName) || !state_events.get(stateName).containsKey(eventName);
            }
//                if (!app_events.containsKey(trigger.getEventName())){
//                    list.add(trigger.getId());
//                }
//            } else {
//                if (!state_events.containsKey(trigger.getStateName()) || !state_events.get(trigger.getStateName()).containsKey(trigger.getEventName())){
//                    list.add(trigger.getId());
//                }
//            }
        }

        public String getEntry() {
            return entry;
        }

        public void setEntry(String entry) {
            this.entry = entry;
        }

        public String getStateName() {
            return stateName;
        }

        public void setStateName(String stateName) {
            this.stateName = stateName;
        }
    }
}
