package com.example.transpomatebus;

import android.os.Bundle;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, busInfoEditText;
    private Spinner routeSpinner;
    private Button registerButton;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private FirebaseUser currentUser;

    private List<String> routeList;
    private Map<String, String> routeMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        busInfoEditText = findViewById(R.id.busInfoEditText);
        routeSpinner = findViewById(R.id.routeSpinner);
        registerButton = findViewById(R.id.registerButton);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        routeList = new ArrayList<>();
        routeMap = new HashMap<>();

        loadRoutes();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void loadRoutes() {
        databaseReference.child("routes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot routeSnapshot : dataSnapshot.getChildren()) {
                    String routeKey = routeSnapshot.getKey();
                    String routeName = routeSnapshot.getValue(String.class);
                    String routeDisplay = routeKey + " - " + routeName;
                    routeList.add(routeDisplay);
                    routeMap.put(routeDisplay, routeKey);
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(RegisterActivity.this,
                        android.R.layout.simple_spinner_item, routeList);
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
        final String email = emailEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();
        final String busInfo = busInfoEditText.getText().toString().trim();
        final String selectedRoute = routeSpinner.getSelectedItem().toString();
        final String routeId = routeMap.get(selectedRoute);

        if (email.isEmpty() || password.isEmpty() || busInfo.isEmpty() || routeId == null) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    DatabaseReference userRef = databaseReference.child("users").child(userId);
                    userRef.child("email").setValue(email);
                    userRef.child("routeId").setValue(routeId);
                    userRef.child("busInfo").setValue(busInfo);

                    DatabaseReference busRef = databaseReference.child("buses").child(routeId).child(busInfo);
                    busRef.child("info").setValue(busInfo);
                    busRef.child("seatsAvailable").setValue(0); // Initially 0 seats available
                    busRef.child("departureTime").setValue(""); // Empty departure time
                    busRef.child("location").child("lat").setValue(0); // Placeholder for initial latitude
                    busRef.child("location").child("lng").setValue(0); // Placeholder for initial longitude

                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
