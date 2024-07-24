package com.example.transpomatebus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LocationTrackerService extends Service implements LocationTracker.LocationCallback {
    public static final String CHANNEL_ID = "LocationTrackerServiceChannel";
    private LocationTracker locationTracker;
    private DatabaseReference databaseReference;
    private String selectedRoute, selectedBusKey;

    @Override
    public void onCreate() {
        super.onCreate();
        locationTracker = new LocationTracker(this, this);
        databaseReference = FirebaseDatabase.getInstance().getReference();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracker Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        selectedRoute = intent.getStringExtra("selectedRoute");
        selectedBusKey = intent.getStringExtra("selectedBusKey");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tracker Service")
                .setContentText("Tracking bus location")
                .setSmallIcon(R.drawable.ic_location)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        locationTracker.startLocationUpdates();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationTracker.stopLocationUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationUpdated(Location location) {
        if (selectedRoute != null && selectedBusKey != null) {
            databaseReference.child("buses").child(selectedRoute).child(selectedBusKey).child("location")
                    .setValue(new LocationWrapper(location.getLatitude(), location.getLongitude()));
        }
    }

    public static class LocationWrapper {
        public double lat;
        public double lng;

        public LocationWrapper() {}

        public LocationWrapper(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }
}
