package com.forkize.sdk;

public class ForkizeConfig extends ForkizeBaseConfig {

    private static ForkizeConfig instance;

    private ForkizeConfig() {

    }

    public static ForkizeConfig getInstance() {
        if (instance == null) {
            instance = new ForkizeConfig();
        }

        return instance;
    }

    protected void setNewSessionInterval(int value) {
        this.newSessionInterval = value;
    }

    protected long getNewSessionInterval() {
        return this.newSessionInterval;
    }
}
