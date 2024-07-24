package com.example.transpomatebus;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;

public class LocationTracker extends Service implements LocationListener {
    private final Context mContext;
    private final LocationManager locationManager;
    private final LocationCallback callback;

    public LocationTracker(Context context, LocationCallback callback) {
        this.mContext = context;
        this.callback = callback;
        locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, this);
    }

    public void stopLocationUpdates() {
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        callback.onLocationUpdated(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public interface LocationCallback {
        void onLocationUpdated(Location location);
    }
}
