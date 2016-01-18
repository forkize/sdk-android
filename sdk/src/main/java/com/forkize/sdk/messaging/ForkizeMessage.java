package com.forkize.sdk.messaging;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class ForkizeMessage {

    private Activity activity;

    private static ForkizeMessage instance;

    private ForkizeMessage() {

    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void releaseActivity() {
        this.activity = null;
    }

    public static ForkizeMessage getInstance() {
        if (instance == null) {
            instance = new ForkizeMessage();
        }

        return instance;
    }

    public void showNotificationIfAvalable() {

    }

//    public void addCampaignMessage(String message) {
//        JSONObject notification;
//
//        try {
//            notification = new JSONObject(message);
//            ForkizeNotificationPool.getInstance().addNotification(notification);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

    public void showMessage(JSONObject message) {
        if (this.activity != null) {

            if (message != null) {
                Intent intent = new Intent(this.activity, ForkizeMessageActivity.class);
                intent.putExtra(ForkizeMessageKeys.MESSAGE, message.toString());
                this.activity.startActivity(intent);
            }
        } else {
            Log.e("Forkize SDK", "Message Activity is null");
        }
    }
}