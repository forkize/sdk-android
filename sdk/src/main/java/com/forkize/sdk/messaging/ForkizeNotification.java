package com.forkize.sdk.messaging;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.forkize.sdk.ForkizeConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;

public class ForkizeNotification {

    private String PART1_1 = "<!doctype html><html><head></script><script type=\"text/javascript\">";
    private String PART1_2 = "\nvar profile =";
// + ANGULAR + "\nvar profile =";

    private String PART1_0 = "<!doctype html><html><head><script src=\"http://ajax.googleapis.com/ajax/libs/angularjs/1.4.8/angular.min.js\"></script><script type=\"text/javascript\">var profile =";

    private String PART1 = "<!doctype html><html><head><script src=\"file:///android_asset/angular.js\"></script><script type=\"text/javascript\">var profile =";
    private String PART2 = ";var content =";
    private String PART3 = ";angular.module('sdkApp', []).controller('sdkController',['$scope','$window',function($scope,$window){var p=$window.profile;for(var k in p){$scope[k]=p[k];}$scope.content=$window.content;}]);</script><link type=\"text/css\" rel=\"stylesheet\" href=";

    private String PART3_1 = ";angular.module('sdkApp', []).controller('sdkController',['$scope','$window',function($scope,$window){var p=$window.profile;for(var k in p){$scope[k]=p[k];}$scope.content=$window.content;}]);</script><style>";
    private String PART4_1 = "</style></head><body ng-app=\"sdkApp\"><div ng-controller=\"sdkController\">";

    private String PART4 = "></head><body ng-app=\"sdkApp\"><div ng-controller=\"sdkController\">";
    private String PART5 = "</div></body></html>";

    private Activity activity;
    private ForkizeNotificationPool pool;
    private ForkizeNotificationTrigger trigger;

    public ForkizeNotification() {
        this.pool = com.forkize.sdk.messaging.ForkizeNotificationPool.getInstance();
        this.trigger = ForkizeNotificationTrigger.getInstance();
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void releaseActivity() {
        this.activity = null;
    }

    public void loadNotifications() {
        ForkizeNotificationTriggerPool.getInstance().reset();
        this.pool.loadNotifications();
//        this.trigger.reset();
    }

    public void saveNotifications() {
        this.pool.saveNotifications();
    }

    private String getAngular() {
        StringBuilder angular = new StringBuilder();
        try {
            InputStream inputStream = activity.getAssets().open("angular.js");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                angular.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e("Forkize SDK", angular.toString());
        return angular.toString();
    }

    public void showNotification(JSONObject profile) {
        JSONObject notification = this.pool.getNotification();
        if (notification != null) {
            StringBuilder viewData = new StringBuilder();

            try {
                String layoutName = notification.getJSONObject("template").getString("layoutId");
                String cssName = notification.getJSONObject("template").getString("cssId");
                Log.e("Forkize SDK", "CSSNAME::   " + cssName);

                viewData.append(PART1_0);
//                viewData.append(PART1_1);
//                viewData.append(getAngular());
//                viewData.append(PART1_2);
                viewData.append(profile.toString());
                viewData.append(PART2);
                viewData.append(notification.getJSONObject("template").getJSONObject("content").toString());
                viewData.append(PART3_1);

                String css = ForkizeTemplateManager.getInstance().readFiles(cssName + ".css");
                viewData.append(css);
                viewData.append(PART4_1);
//                viewData.append(PART3);
//                viewData.append("'file:///data/user/0/com.forkize.davkhech.theapp/cache/forkize/").append(cssName).append(".css'");
//                viewData.append(PART4);

                String html = ForkizeTemplateManager.getInstance().readFiles(layoutName + ".html");

                viewData.append(html);    //ForkizeTemplateManager.getInstance().readFiles(layoutName + ".html"));
                viewData.append(PART5);

                String raw = viewData.toString();

                Log.e("Forkize SDK", "RAW:DATA\n\n\n" + raw);
//                Log.e("Forkize SDK", this.activity.toString());

                Intent intent = new Intent(this.activity, ForkizeMessageActivity.class);
                intent.putExtra(ForkizeMessageKeys.MESSAGE, notification.toString());
                intent.putExtra("raw", raw);

//                Log.e("Forkize SDK", intent.getAction());
//                Log.e("Forkize SDK", this.activity.getPackageName());

                this.activity.startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void addNotification(String notification) {
        // FZ::TODO Validate

        Log.e("Forkize SDK", "Notification\n\n\n" + notification);

        JSONObject message;
        try {
            message = new JSONObject(notification);
            pool.addNotification(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void processEvent(JSONObject object) {
        ForkizeNotificationTriggerPool.getInstance().registerEvent(object);
//        this.trigger.processEvent(object);
    }

    public JSONObject getNotificationHashArray() {
        return this.pool.getNotificationHashArray();
    }

    public JSONObject getTemplateHashArray() {
        return this.pool.getTemplateHashArray();
    }
}
