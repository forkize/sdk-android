package com.forkize.sdk.messaging;

import android.content.SharedPreferences;
import android.util.Log;

import com.forkize.sdk.Forkize;
import com.forkize.sdk.ForkizeConfig;
import com.forkize.sdk.ForkizeHelper;
import com.forkize.sdk.ForkizeInstance;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class ForkizeNotificationPool {

    private static ForkizeNotificationPool instance = new ForkizeNotificationPool();
    private final String PREFS = "forkize_campaigns";
    private JSONArray notificationList;
    private ArrayList<JSONObject> pending;
    private ArrayDeque<JSONObject> available;

    private ForkizeNotificationPool() {
        pending = new ArrayList<>();
        available = new ArrayDeque<>();
    }

    protected static ForkizeNotificationPool getInstance() {
        return instance;
    }

    protected void addNotification(JSONObject campaign) {
        pending.add(campaign);
        SharedPreferences preferences = ForkizeConfig.getInstance().getApplicationContext().getSharedPreferences(PREFS, 0);
        SharedPreferences.Editor editor = preferences.edit();

        String id = campaign.optString("_id", "dump");
        editor.putString(id, campaign.toString());
        editor.apply();

        _updateNotificationList(id);
//        ForkizeNotificationTrigger.getInstance().setTrigger(campaign);
        ForkizeNotificationTriggerPool.getInstance().setTrigger(campaign);
    }

    protected void saveNotifications() {
        String uid = ForkizeInstance.getInstance().getUserProfile().getUserId() + "list";
        SharedPreferences preferences = ForkizeConfig.getInstance().getApplicationContext().getSharedPreferences(PREFS, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(uid, notificationList.toString());
        editor.apply();
    }

    protected void loadNotifications() {
        notificationList = _getUsersNotificationList();
        pending.clear();
        available.clear();

        SharedPreferences preferences = ForkizeConfig.getInstance().getApplicationContext().getSharedPreferences(PREFS, 0);
        for (int i = 0; i < notificationList.length(); ++i) {
            try {
                String id = notificationList.getString(i);
                String content = preferences.getString(id, null);
                if (!ForkizeHelper.isNullOrEmpty(content)) {
                    pending.add(new JSONObject(content));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.e("Forkize SDK", "Notifications have been loaded");
//        ForkizeNotificationTrigger.getInstance().setTriggers(pending);
        ForkizeNotificationTriggerPool.getInstance().setTriggers(pending);
    }

    protected JSONObject getNotification() {
        ForkizeNotificationTriggerPool.getInstance().prepare();
//        if (pending.size() > 0) {
//            return pending.get(0);
//        }

        return this.available.pollFirst();
    }

    protected void makeAvailable(String id) {
        for (int i = 0; i < pending.size(); ++i) {
            JSONObject object = pending.get(i);
            try {
                if (id.equals(object.getString("_id"))) {
                    available.addFirst(object);
                    pending.remove(i);
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    protected JSONObject getNotificationHashArray() {
        JSONObject array = new JSONObject();
        try {
            for (JSONObject notification : pending) {
                array.put(notification.getString("_id"), ForkizeHelper.md5(notification.toString()));
            }
            for (JSONObject notification : available) {
                array.put(notification.getString("_id"), ForkizeHelper.md5(notification.toString()));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return array;
    }

    protected JSONObject getTemplateHashArray() {
        JSONObject array = new JSONObject();
        try {
            String cssName, htmlName;
            for (JSONObject notification : pending) {
                cssName = _getCssName(notification);
                htmlName = _getHtmlName(notification);
                if (!ForkizeHelper.isNullOrEmpty(cssName) && !ForkizeHelper.isNullOrEmpty(htmlName)) {
                    array.put(cssName, ForkizeTemplateManager.getInstance().getHash(cssName));
                    array.put(htmlName, ForkizeTemplateManager.getInstance().getHash(htmlName));
                }
            }
            for (JSONObject notification : available) {
                cssName = _getCssName(notification);
                htmlName = _getHtmlName(notification);
                if (!ForkizeHelper.isNullOrEmpty(cssName) && !ForkizeHelper.isNullOrEmpty(htmlName)) {
                    array.put(cssName, ForkizeTemplateManager.getInstance().getHash(cssName));
                    array.put(htmlName, ForkizeTemplateManager.getInstance().getHash(htmlName));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return array;
    }

    protected void reset() {
        this.pending.clear();
        this.available.clear();
    }

    private JSONArray _getUsersNotificationList() {
        SharedPreferences preferences = ForkizeConfig.getInstance().getApplicationContext().getSharedPreferences(PREFS, 0);
        String uid = ForkizeInstance.getInstance().getUserProfile().getUserId() + "list";
        Log.e("Forkize SDK", uid);
        String str = preferences.getString(uid, "[]");
        try {
            return new JSONArray(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONArray();
    }

    private void _updateNotificationList(String newId) {
        if (notificationList == null) {
            notificationList = new JSONArray();
        }
        for (int i = 0; i < notificationList.length(); ++i) {
            try {
                String id = notificationList.getString(i);
                if (id.equals(newId)) {
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        notificationList.put(newId);
    }

    private String _getCssName(JSONObject object) {
        return object.optJSONObject("template").optString("cssId") + ".css";
    }

    private String _getHtmlName(JSONObject object) {
        return object.optJSONObject("template").optString("layoutId") + ".html";
    }
}
