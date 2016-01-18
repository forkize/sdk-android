package com.forkize.sdk;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationInstance {

    private double longitude;
    private double latitude;

    private LocationManager locationManager;

    private static LocationInstance instance;

    private LocationInstance() {
        this.latitude = 1000;
        this.longitude = 1000;
    }

    protected static LocationInstance getInstance() {
        if (instance == null) {
            instance = new LocationInstance();
        }

        return instance;
    }

    protected void setContext(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    protected void setListeners() {
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new MyLocationListener());
            Log.i("Forkize SDK", "Location manager network provider");
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new MyLocationListener());
            Log.i("Forkize SDK", "Location manager GPS provider");
        } else {
            Log.i("Forkize SDK", "Location manager providers are not enabled");
        }
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }
}
