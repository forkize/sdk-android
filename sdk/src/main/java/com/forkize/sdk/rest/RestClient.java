package com.forkize.sdk.rest;

import android.util.Log;

import com.forkize.sdk.ForkizeConfig;
import com.forkize.sdk.ForkizeEventManager;
import com.forkize.sdk.ForkizeHelper;
import com.forkize.sdk.ForkizeInstance;
import com.forkize.sdk.UserProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RestClient {

    private ExecutorService restClientExecutor;
    private ForkizeEventManager eventManager;
    private UserProfile userProfile;

    private Request request;

    public RestClient() {
        this.request = new Request();
        this.eventManager = ForkizeEventManager.getInstance();
        this.userProfile = ForkizeInstance.getInstance().getUserProfile();
        this.restClientExecutor = Executors.newSingleThreadExecutor();
    }

    private void getAccessToken() {
        try {
            if (restClientExecutor.isShutdown()) {
                Log.i("Forkize SDK", "Trying to schedule a rest client execution while shutdown");
            } else {
                restClientExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        RestClient.this.request.getAccessToken();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("Forkize SDK", "Error while scheduling a rest client execution", e);
        }
    }

    private void updateUserProfile() {
        try {
            if (restClientExecutor.isShutdown()) {
                Log.i("Forkize SDK", "Trying to schedule a rest client execution while shutdown");
            } else {
                restClientExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (RestClient.this.request.updateUserProfile()) {
                            ForkizeInstance.getInstance().getUserProfile().dropChangeLog();
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e("Forkize SDK", "Error while scheduling a rest client execution", e);
        }
    }

    private void updateCampaign() {
        try {
            if (restClientExecutor.isShutdown()) {
                Log.i("Forkize SDK", "Trying to schedule a rest client execution while shutdown");
            } else {
                restClientExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        RestClient.this.request.updateCampaign();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("Forkize SDK", "Error while scheduling a rest client execution", e);
        }
    }

    public void getTemplates() {
        try {
            if (restClientExecutor.isShutdown()) {
                Log.i("Forkize SDK", "Trying to schedule a rest client execution while shutdown");
            } else {
                restClientExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        RestClient.this.request.getTemplates();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("Forkize SDK", "Error while scheduling a rest client execution", e);
        }
    }

    public void tryToSend() {
        try {
            if (restClientExecutor.isShutdown()) {
                Log.i("Forkize SDK", "Trying to schedule a rest client execution while shutdown");
            } else {

                if (ForkizeHelper.isNullOrEmpty(this.request.getToken())) {
                    getAccessToken();
                    updateUserProfile();
                    updateCampaign();
                    getTemplates();
                }

                if (userProfile.getAliased() == 0) {
                    userProfile.checkAliasState();
                }

                if (userProfile.getAliased() == 1) {
                    restClientExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (RestClient.this.request.postAlias(userProfile.getAliasCursor())) {
                                userProfile.updateAlias();
                            }
                        }
                    });
                }

                // TODO
                ForkizeInstance.getInstance().getUserProfile().showMessage();
                restClientExecutor.execute(new RestClientRunnable());
            }
        } catch (Exception e) {
            Log.e("Forkize SDK", "Error while scheduling a rest client execution", e);
        }
    }

    public void close() throws InterruptedException {
        this.restClientExecutor.shutdown();
        this.restClientExecutor.awaitTermination(5L, TimeUnit.SECONDS);
    }

    private class RestClientRunnable implements Runnable {

        @Override
        public void run() {
            String[] list = RestClient.this.eventManager.getEvents(ForkizeConfig.getInstance().getMaxEventsPerFlush());
            if (list == null) {
                return;
            }

            JSONArray api_data = new JSONArray();
            for (String listItem : list) {
                try {
                    api_data.put(new JSONObject(listItem));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            int responseCode = RestClient.this.request.postEvents(api_data);
            if (responseCode == 1) {
                RestClient.this.eventManager.removeEvents(list.length);
            } else if (responseCode == 2) {
                RestClient.this.request.dropAccessToken();
            }
        }
    }
}