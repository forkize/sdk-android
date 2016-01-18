package com.forkize.sdk;

import android.app.Activity;
import android.content.Context;

import java.io.File;

public class ForkizeBaseConfig {

    private final String sdkVersion = "1.0";

    protected long newSessionInterval = 30000L;

    // FZ::TODO set clever configs

    private final String dbName = "forkize.db";
    private final long maxSQLiteDBSize = 1048576L;
    private final int maxEventsPerFlush = 10;
    private final int timeAfterFlush = 15000;

    // FZ::TODO applicationContext ?
    private Context applicationContext;
    private String appId;
    private String appKey;
    private File cacheDir;

    protected ForkizeBaseConfig() {
        // read config from xml
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public String getAppId() {
        return this.appId;
    }

    public void setAppId(String value) {
        this.appId = value;
    }

    public String getAppKey() {
        return this.appKey;
    }

    public void setAppKey(String value) {
        this.appKey = value;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public File getCacheDir() {
        return this.cacheDir;
    }

    public String getDbName() {
        return this.dbName;
    }

    public long getMaxSQLiteDBSize() {
        return this.maxSQLiteDBSize;
    }

    public int getMaxEventsPerFlush() {
        return this.maxEventsPerFlush;
    }

    public int getTimeAfterFlush() {
        return timeAfterFlush;
    }
}
