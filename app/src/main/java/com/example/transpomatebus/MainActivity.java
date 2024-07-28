package com.example.transpomatebus;

import android.Manifest;
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

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
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

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private FirebaseUser currentUser;
    private String userId;
    private String routeId;
    private String busInfo;
    private boolean isTracking;

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

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (currentUser != null) {
            userId = currentUser.getUid();
            fetchUserData();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }

        updateButton.setOnClickListener(v -> updateBusDetails());

        trackLocationButton.setOnClickListener(v -> startLocationTracking());

        stopTrackingButton.setOnClickListener(v -> stopLocationTracking());
    }

    private void fetchUserData() {
        databaseReference.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    routeId = snapshot.child("routeId").getValue(String.class);
                    busInfo = snapshot.child("busInfo").getValue(String.class);

                    fetchRouteInfo(routeId);
                    busInfoTextView.setText("Bus Info: " + busInfo);

                    fetchBusDetails();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchRouteInfo(String routeId) {
        databaseReference.child("routes").child(routeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String routeName = snapshot.getValue(String.class);
                    routeTextView.setText("Route: " + routeId + " - " + routeName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to fetch route info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchBusDetails() {
        databaseReference.child("buses").child(routeId).child(busInfo).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long seatsAvailable = snapshot.child("seatsAvailable").getValue(Long.class);
                    String departureTime = snapshot.child("departureTime").getValue(String.class);

                    seatsEditText.setText(String.valueOf(seatsAvailable));
                    departureTimeEditText.setText(departureTime);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to fetch bus details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBusDetails() {
        String seats = seatsEditText.getText().toString();
        String departureTime = departureTimeEditText.getText().toString();

        if (!seats.isEmpty() && !departureTime.isEmpty()) {
            databaseReference.child("buses").child(routeId).child(busInfo).child("seatsAvailable").setValue(Integer.parseInt(seats));
            databaseReference.child("buses").child(routeId).child(busInfo).child("departureTime").setValue(departureTime);
            Toast.makeText(this, "Bus details updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
        }
    }

    private void startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        isTracking = true;

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (location != null && isTracking) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();

                        databaseReference.child("buses").child(routeId).child(busInfo).child("location").child("lat").setValue(lat);
                        databaseReference.child("buses").child(routeId).child(busInfo).child("location").child("lng").setValue(lng);
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show();
    }

    private void stopLocationTracking() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isTracking = false;
            Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationTracking();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationTracking();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
