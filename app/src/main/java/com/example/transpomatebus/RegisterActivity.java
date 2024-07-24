package com.example.transpomatebus;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, busInfoEditText;
    private Spinner routeSpinner;
    private Button registerButton;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private ArrayList<String> routeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        busInfoEditText = findViewById(R.id.busInfoEditText);
        routeSpinner = findViewById(R.id.routeSpinner);
        registerButton = findViewById(R.id.registerButton);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        loadRoutes();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void loadRoutes() {
        routeList = new ArrayList<>();
        databaseReference.child("routes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    routeList.add(snapshot.getValue(String.class));
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(RegisterActivity.this, android.R.layout.simple_spinner_item, routeList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                routeSpinner.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(RegisterActivity.this, "Failed to load routes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String busInfo = busInfoEditText.getText().toString().trim();
        String selectedRoute = routeSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        if (TextUtils.isEmpty(busInfo)) {
            busInfoEditText.setError("Bus information is required");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    String userId = user.getUid();
                    DatabaseReference busRef = databaseReference.child("buses").child(selectedRoute).push();
                    Bus bus = new Bus(busInfo, 0, "", new LocationWrapper(0, 0), userId);
                    busRef.setValue(bus);

                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            } else {
                Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class Bus {
        public String info;
        public int seatsAvailable;
        public String departureTime;
        public LocationWrapper location;
        public String ownerId;

        public Bus() {}

        public Bus(String info, int seatsAvailable, String departureTime, LocationWrapper location, String ownerId) {
            this.info = info;
            this.seatsAvailable = seatsAvailable;
            this.departureTime = departureTime;
            this.location = location;
            this.ownerId = ownerId;
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
