package com.example.transpomatebus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private TextView routeTextView, busInfoTextView;
    private EditText seatsEditText, departureTimeEditText;
    private Button updateButton, trackLocationButton, stopTrackingButton;

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;

    private String busId = "bus1"; // This should be dynamically set based on the user's bus
    private String routeId = "101"; // This should be dynamically set based on the user's route

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

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        loadBusDetails();

        updateButton.setOnClickListener(v -> updateBusDetails());

        trackLocationButton.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                startLocationService();
            } else {
                requestLocationPermission();
            }
        });

        stopTrackingButton.setOnClickListener(v -> stopService(new Intent(MainActivity.this, LocationTrackerService.class)));
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                Toast.makeText(this, "Location permission is required for tracking", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationService() {
        startService(new Intent(MainActivity.this, LocationTrackerService.class));
    }

    private void loadBusDetails() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            databaseReference.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        routeId = snapshot.child("routeId").getValue(String.class);
                        busId = snapshot.child("busId").getValue(String.class);

                        databaseReference.child("routes").child(routeId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String routeInfo = snapshot.getValue(String.class);
                                routeTextView.setText(routeInfo);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(MainActivity.this, "Failed to load route info", Toast.LENGTH_SHORT).show();
                            }
                        });

                        databaseReference.child("buses").child(routeId).child(busId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String busInfo = snapshot.child("info").getValue(String.class);
                                    busInfoTextView.setText(busInfo);
                                } else {
                                    Toast.makeText(MainActivity.this, "Bus details not found", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(MainActivity.this, "Failed to load bus details", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(MainActivity.this, "User details not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Failed to load user details", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateBusDetails() {
        String availableSeats = seatsEditText.getText().toString().trim();
        String departureTime = departureTimeEditText.getText().toString().trim();

        if (availableSeats.isEmpty() || departureTime.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseReference.child("buses").child(routeId).child(busId).child("seatsAvailable").setValue(Integer.parseInt(availableSeats));
        databaseReference.child("buses").child(routeId).child(busId).child("departureTime").setValue(departureTime)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Bus details updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to update bus details", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
