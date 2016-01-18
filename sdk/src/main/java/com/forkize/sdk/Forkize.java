package com.forkize.sdk;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.forkize.sdk.messaging.ForkizeMessage;
import com.forkize.sdk.messaging.ForkizeNotification;
import com.forkize.sdk.rest.RestClient;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Forkize implements IForkize {

    private AtomicInteger counter;
    private boolean destroyed, initialized;
    // FZ::TODO  Do we need this ?
    private Date initializedTime;

    private ForkizeConfig config;
    private SessionInstance sessionInstance;
    private RestClient restClient;

    private DeviceInfo deviceInfo;
    private UserProfile userProfile;
    private ForkizeEventManager eventManager;

    private WeakReference<Activity> activityWeakReference;
    private WeakReference<Context> contextWeakReference;

    private Thread thread;

    private boolean mustCleanInstance;
    private volatile boolean isRunning;

    // FZ::TODO currrency enum
    //public static enum Season { AMD SPRING, SUMMER, AUTUMN }
    //public static enum CURRENCY =

    protected Forkize() {
        this.destroyed = true;
        this.counter = new AtomicInteger();

        this.config = ForkizeConfig.getInstance();
        this.userProfile = UserProfile.getInstance();
        this.sessionInstance = SessionInstance.getInstance();
        this.deviceInfo = DeviceInfo.getInstance();
        this.eventManager = ForkizeEventManager.getInstance();

        Log.e("Forkize SDK", "Forkize constructor called !");
    }

    @Override
    public void authorize(String appId, String appKey) {
        this.config.setAppId(appId);
        this.config.setAppKey(appKey);
        // TODO check
//        if (!this.thread.isAlive()) {
//            this.thread.start();
//        }
    }

    @Override
    public void track(String eventName, Map<String, Object> parameters) {
        this.eventManager.queueEvent(eventName, parameters);
    }

    @Override
    public void purchase(String productId, String currency, double price, int quantity) {
        Map<String, Object> map = new HashMap<>();
        map.put("product_id", productId);
        map.put("currency", currency);
        map.put("price", price);
        map.put("quantity", quantity);
        this.eventManager.queueEvent("purchase", map);
    }

    // ** Session part

    @Override
    public void sessionStart() {
        this.sessionInstance.start();
    }

    @Override
    public void sessionEnd() {
        this.sessionInstance.end();
    }

    @Override
    public void sessionPause() {
        this.sessionInstance.pause();
    }

    @Override
    public void sessionResume() {
        this.sessionInstance.resume();
    }

    @Override
    public void eventDuration(String eventName) {
        this.eventManager.eventDuration(eventName);
    }

    @Override
    public void setSuperProperties(Map<String, Object> properties) {
        this.eventManager.setSuperProperties(properties);
    }

    @Override
    public void setSuperPropertiesOnce(Map<String, Object> properties) {
        this.eventManager.setSuperPropertiesOnce(properties);
    }

    @Override
    public void advanceToState(String stateArg) {
        this.eventManager.setState(stateArg);
    }

    @Override
    public void resetState() {
        this.eventManager.resetState();
    }

    @Override
    public UserProfile getUserProfile() {
        return this.userProfile;
    }

    @Override
    // ** should be called on with every activity onCreate
    public IForkize onCreate(Activity activity) throws IllegalArgumentException {
        if (activity == null) {
            // FZ::TODO lets create ForkizeException
            throw new IllegalArgumentException("onCreate must be called with valid Activity object");
        }

        this.isRunning = true;

        if (this.destroyed) {
            this.destroyed = this.initialized = false;
            this.counter.set(1);
        }

        if (this.counter.get() == 1) {
            userProfile.setAliased(0);
        }

        if (!this.initialized) {
            this.initialized = true;
            this.initializedTime = new Date();

            Context context = bindToContext(activity);

            LocationInstance.getInstance().setContext(context);
            LocationInstance.getInstance().setListeners();

            StorageFactory.getInstance().setContext(context);
            ForkizeConfig.getInstance().setApplicationContext(context);
//            this.userProfile.loadMessages();

            this.eventManager.setContext(context);
            this.deviceInfo.setContext(context);
            this.deviceInfo.getDeviceInfo();

            this.restClient = new RestClient();

            this.thread = new Thread(new MainRunnable());
        } else {
            bindToContext(activity);
            if (!thread.isAlive()) {
                thread.start();
            }
        }

        return this;
    }

    private Context bindToContext(Activity activity) {
        this.counter.incrementAndGet();
        this.activityWeakReference = new WeakReference<>(activity);
        this.contextWeakReference = new WeakReference<>(activity.getApplicationContext());

        this.deviceInfo.setActivity(this.activityWeakReference.get());

        // FZ::TODO
        ForkizeMessage.getInstance().setActivity(this.activityWeakReference.get());
        UserProfile.getInstance().setActivity(this.activityWeakReference.get());

        return contextWeakReference.get();
    }

    @Override
    public void onPause() {
        try {
            _onPause();
        } catch (Exception e) {
            // ** FZ::TODO throw Forkize exception
            Log.e("Forkize SDK", "Exception thrown onPause");
        }
    }

    private void _onPause() {
        Log.i("Forkize SDK", "On Pause");
        this.mustCleanInstance = true;
        if (this.activityWeakReference != null) {
            this.mustCleanInstance = this.activityWeakReference.get().isFinishing();
        }

        this.counter.decrementAndGet();

        this.eventManager.flushCacheToDatabase();
        this.sessionInstance.pause();
    }


    @Override
    public void onResume(Activity activity) {
        try {
            _onResume(activity);
        } catch (Exception e) {
            Log.e("Forkize SDK", "Exception thrown onResume");
        }
    }

    private void _onResume(Activity activity) {
        Log.i("Forkize SDK", "On Resume");

        if (activity != null) {
            bindToContext(activity);
            this.sessionInstance.resume();
        }
    }

    @Override
    public void onDestroy() {
        try {
            _onDestroy();
        } catch (Exception e) {
            Log.e("Forkize SDK", "Exception thrown onDestroy", e);
        }
    }

    private void _onDestroy() {
        try {

            //FZ::TODO
//            this.activityWeakReference = null;

            int destroyCounter = this.counter.decrementAndGet();

            Log.e("Forkize SDK", "counter in time of destroy " + counter.get());

            if (destroyCounter == 0 && mustCleanInstance) {
                _shutDown();
            }

            // **
            if (this.counter.get() == 1) {
                this.sessionInstance.end();
                this.eventManager.flushCacheToDatabase();
                this.userProfile.flushToDatabase();
                Log.e("Forkize SDK", "Everything gone to database !!!");

                this.userProfile.saveMessages();
                this.deviceInfo.unregisterBatteryReceiver(this.activityWeakReference.get());

                // FZ::TODO
                ForkizeMessage.getInstance().releaseActivity();
//                ForkizeNotification.getInstance().releaseActivity();
                UserProfile.getInstance().releaseActivity();
            }

            Log.e("Forkize SDK", "_onDestroy!");
        } catch (Exception e) {
            Log.e("Forkize SDK", "Exception thrown _onDestroy", e);
        }
    }

    private void _shutDown() throws InterruptedException {

        if (!this.destroyed) {
            Log.i("Forkize SDK", "Shutting down the SDK ...");

            this.initializedTime = null;

            this.activityWeakReference = null;

            this.userProfile.flushToDatabase();

            this.restClient.close();
            this.restClient = null;

            this.eventManager.close();
            this.eventManager = null;

            this.isRunning = false;
            this.destroyed = true;

            Log.i("Forkize SDK", "SDK is shot down!");
        }
    }

    @Override
    public void onLowMemory() {
        try {
            _onLowMemory();
        } catch (Exception e) {
            Log.e("Forkize SDK", "Exception thrown onLowMemory", e);
        }
    }

    private void _onLowMemory() {
        if (this.eventManager != null) {
            this.eventManager.flushCacheToDatabase();
        }
    }

    private class MainRunnable implements Runnable {
        @Override
        public void run() {

            while (Forkize.this.isRunning) {
                try {
                    Thread.sleep(ForkizeConfig.getInstance().getTimeAfterFlush());
                    Forkize.this.restClient.tryToSend();

                } catch (InterruptedException e) {
                    Log.i("Forkize SDK", "Something went wrong in MainRunnable", e);
                }
            }
        }
    }
}