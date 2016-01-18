package com.forkize.sdk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DeviceInfo {

    private String language;
    private String country;
    private String appVersion;
    private Context context;
    private int batteryLevel;

    private Map<String, Object> deviceParams;
    private BroadcastReceiver broadcastReceiver;

    private static DeviceInfo instance;

    private DeviceInfo() {
        this.batteryLevel = -1;
    }

    protected static DeviceInfo getInstance() {
        if (instance == null) {
            instance = new DeviceInfo();
        }

        return instance;
    }

    protected void setContext(Context contextVal) {
        this.context = contextVal;
    }

    protected void setActivity(Activity activity) {
        if (broadcastReceiver == null) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    DeviceInfo.this.batteryLevel = -1;
                    int rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    if (rawLevel >= 0 && scale > 0) {
                        DeviceInfo.this.batteryLevel = (rawLevel * 100) / scale;
                    }
                }
            };

            activity.registerReceiver(broadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }

    protected void unregisterBatteryReceiver(Activity activity) {
        activity.unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
    }

    protected Map<String, Object> getDeviceInfo() {
        if (this.deviceParams == null)
            _fetchDeviceParams();
        try {
            return deviceParams;
        } catch (Exception e) {
            Log.e("Forkize SDK", "Exception thrown getting device info", e);
        }
        return null;
    }

    private void _fetchDeviceParams() {
        try {

            deviceParams = new HashMap<>();

            if (ForkizeHelper.isNullOrEmpty(this.language)) {
                this.language = _toLanguageTag(Locale.getDefault());
                Log.i("Forkize SDK", "Language tag " + this.language);
            }

            if (ForkizeHelper.isNullOrEmpty(this.appVersion) && this.context != null) {
                PackageInfo info;
                info = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0);
                this.appVersion = info.versionName;
                Log.i("Forkize SDK", "Version Name " + this.appVersion);
            }

            deviceParams.put("device_manufacturer", Build.MANUFACTURER);
            deviceParams.put("device_model", Build.MODEL);
            deviceParams.put("device_os_name", "android");
            deviceParams.put("device_os_version", Build.VERSION.RELEASE);

            if (this.context != null) {
                //  FZ::TODO get screen params
                //  Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                int width = metrics.widthPixels;
                int height = metrics.heightPixels;
                float density = metrics.density;
                deviceParams.put("device_width", width);
                deviceParams.put("device_height", height);
                deviceParams.put("density", density);
            }

            if (!ForkizeHelper.isNullOrEmpty(this.country)) {
                // FZ::TODO what is country format
                deviceParams.put("country", this.country);
            }

            deviceParams.put("language", this.language);
            deviceParams.put("app_version", this.appVersion);

            //  FZ::TODO add needed params like !!! battery level

        } catch (Exception e) {
            Log.e("Forkize SDK", "Exception thrown when device info collecting", e);
        }
    }

    private String _toLanguageTag(Locale locale) {
        StringBuilder languageTag = new StringBuilder();
        languageTag.append(locale.getLanguage().toLowerCase());

        if (!ForkizeHelper.isNullOrEmpty(locale.getCountry())) {
            this.country = locale.getCountry().toLowerCase();
        }
        if (!ForkizeHelper.isNullOrEmpty(locale.getVariant())) {
            languageTag.append('-').append(locale.getVariant().toLowerCase());
        }
        return languageTag.toString();
    }

    // ** we should calculate it with every event
    public int getBatteryLevel() {
        return this.batteryLevel;
    }

    public String getAppVersion() {
        return this.appVersion;
    }
}
