package com.forkize.sdk;

import android.os.Build;

public class ForkizeInstance {

    private static IForkize instance = null;

    public static synchronized IForkize getInstance() {
        if (instance == null) {
            if (Build.VERSION.SDK_INT >= 10) {
                instance = new Forkize();
            } else {
                instance = new ForkizeEmpty();
            }
        }

        return instance;
    }
}
