package com.forkize.sdk.tmp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.forkize.sdk.ForkizeHelper;
import com.forkize.sdk.IForkize;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class ForkizeWorker{

    protected static WeakReference<Activity> activityWeakReference;
    protected String user = "";

    public void getUserId(){
        String userId = "id_" + user;

        SharedPreferences preferences = activityWeakReference.get().getSharedPreferences("forkize_prefs", 0);
        String id = preferences.getString(userId, null);

        if (ForkizeHelper.isNullOrEmpty(id)){
            Log.e("ForkizeWorker", "Missing User Id");
            Log.e("ForkizeWorker", userId);

            id = UUID.randomUUID().toString();
            Log.i("ForkizeWorker", "Id is " + id);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(userId, id);
            editor.apply();
        } else {
            Log.i("ForkizeWorker", "Id is " + id);
        }
    }
}
