package com.forkize.sdk;

import android.app.Activity;

import java.util.Map;

public interface IForkize {

    void authorize(String appId, String appKey);

    IForkize onCreate(Activity activity);

    void onPause();

    void onResume(Activity activity);

    void onDestroy();

    // ** state
    void advanceToState(String stateArg);

    void resetState();

    // ** event tracking interface
    void track(String eventName, Map<String, Object> parameters);

    void purchase(String productId, String currency, double price, int quantity);

    void eventDuration(String eventName);

    // ** event tracking interface
    void sessionStart();

    void sessionEnd();

    void sessionPause();

    void sessionResume();

    void setSuperProperties(Map<String, Object> properties);

    void setSuperPropertiesOnce(Map<String, Object> properties);

    UserProfile getUserProfile();

    void onLowMemory();
}
