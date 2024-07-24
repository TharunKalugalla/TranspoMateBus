package com.example.transpomatebus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private TextView routeTextView, busInfoTextView;
    private EditText seatsEditText, departureTimeEditText;
    private Button updateButton, trackLocationButton, stopTrackingButton;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private String selectedRoute, selectedBusKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        routeTextView = findViewById(R.id.routeTextView);
        busInfoTextView = findViewById(R.id.busInfoTextView);
        seatsEditText = findViewById(R.id.seatsEditText);
        departureTimeEditText = findViewById(R.id.departureTimeEditText);
        updateButton = findViewById(R.id.updateButton);
        trackLocationButton = findViewById(R.id.trackLocationButton);
        stopTrackingButton = findViewById(R.id.stopTrackingButton);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();

        updateButton.setOnClickListener(v -> updateBusDetails());
        trackLocationButton.setOnClickListener(v -> startTracking());
        stopTrackingButton.setOnClickListener(v -> stopTracking());

        // Display the bus info and route info
        displayRouteAndBusInfo();
    }

    private void updateBusDetails() {
        String seats = seatsEditText.getText().toString();
        String departureTime = departureTimeEditText.getText().toString();

        if (selectedRoute != null && selectedBusKey != null) {
            databaseReference.child("buses").child(selectedRoute).child(selectedBusKey).child("seatsAvailable").setValue(Integer.parseInt(seats));
            databaseReference.child("buses").child(selectedRoute).child(selectedBusKey).child("departureTime").setValue(departureTime);
            Toast.makeText(MainActivity.this, "Bus details updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Failed to update bus details", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        Intent serviceIntent = new Intent(this, LocationTrackerService.class);
        serviceIntent.putExtra("selectedRoute", selectedRoute);
        serviceIntent.putExtra("selectedBusKey", selectedBusKey);
        startService(serviceIntent);
    }

    private void stopTracking() {
        Intent serviceIntent = new Intent(this, LocationTrackerService.class);
        stopService(serviceIntent);
    }

    private void displayRouteAndBusInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            databaseReference.child("buses").orderByChild("ownerId").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Bus bus = snapshot.getValue(Bus.class);
                        if (bus != null) {
                            selectedRoute = snapshot.getRef().getParent().getKey();
                            selectedBusKey = snapshot.getKey();
                            routeTextView.setText("Route: " + selectedRoute);
                            busInfoTextView.setText("Bus Info: " + bus.info);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(MainActivity.this, "Failed to display bus info", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static class Bus {
        public String info;
        public int seatsAvailable;
        public String departureTime;
        public LocationWrapper location;

        public Bus() {}

        public Bus(String info, int seatsAvailable, String departureTime, LocationWrapper location) {
            this.info = info;
            this.seatsAvailable = seatsAvailable;
            this.departureTime = departureTime;
            this.location = location;
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
