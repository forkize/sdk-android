package com.forkize.sdk.tmp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.forkize.sdk.Forkize;
import com.forkize.sdk.ForkizeInstance;
import com.forkize.sdk.IForkize;

public class ForkizeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ForkizeActivity", "Activity created");

        ForkizeInstance.getInstance().onCreate(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("ForkizeActivity", "Activity started");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ForkizeActivity", "Activity resumed");
        ForkizeInstance.getInstance().onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("ForkizeActivity", "Activity paused");
        ForkizeInstance.getInstance().onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ForkizeActivity", "Activity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("ForkizeActivity", "Activity destroyed");
        ForkizeInstance.getInstance().onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ForkizeInstance.getInstance().onLowMemory();
    }
}
