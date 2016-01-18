package com.forkize.sdk;

import android.app.Activity;

import java.util.Map;

// FZ::POINT think about other versions of SDK
public class ForkizeEmpty implements IForkize {

    ForkizeEmpty() {
    }

    @Override
    public void authorize(String appId, String appKey) {

    }

    @Override
    public void advanceToState(String stateArg) {

    }

    @Override
    public void resetState() {

    }

    @Override
    public void purchase(String productId, String currency, double price, int quantity) {

    }

    @Override
    public void sessionStart() {

    }

    @Override
    public void sessionEnd() {

    }

    @Override
    public void sessionPause() {

    }

    @Override
    public void sessionResume() {

    }

    @Override
    public void eventDuration(String eventName) {

    }

    @Override
    public void setSuperProperties(Map<String, Object> properties) {

    }

    @Override
    public void setSuperPropertiesOnce(Map<String, Object> properties) {

    }

    @Override
    public UserProfile getUserProfile() {
        return null;
    }

    @Override
    public IForkize onCreate(Activity activity) {
        return null;
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume(Activity activity) {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void track(String eventName, Map<String, Object> parameters) {

    }

    @Override
    public void onLowMemory() {

    }
}
