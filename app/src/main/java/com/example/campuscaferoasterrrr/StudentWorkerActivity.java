package com.example.campuscaferoasterrrr;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentWorkerActivity extends AppCompatActivity {

    private TextView emailTextView;
    private TextView studentIdTextView;
    private TextView nameTextView;
    private TextView phoneTextView;
    private TextView ssnTextView;
    private FirebaseFirestore db;
    private boolean isSSNVisible = false; // Tracks SSN visibility

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_worker);

        emailTextView = findViewById(R.id.email_text_view);
        studentIdTextView = findViewById(R.id.student_id_text_view);
        nameTextView = findViewById(R.id.name);
        phoneTextView = findViewById(R.id.phone);
        ssnTextView = findViewById(R.id.ssn);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Retrieve user data
        retrieveUserData();

        // Set click listener for SSN TextView
        ssnTextView.setOnClickListener(view -> {
            if (isSSNVisible) {
                // Hide SSN
                ssnTextView.setText("***-**-****");
            } else {
                // Fetch the full SSN from the database if needed
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    db.collection("users").document(user.getUid())
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        String ssn = document.getString("ssn");
                                        ssnTextView.setText(ssn);
                                    } else {
                                        Toast.makeText(this, "SSN not found.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(this, "Failed to retrieve SSN.", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
            isSSNVisible = !isSSNVisible; // Toggle visibility state
        });

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            if (item.getItemId() == R.id.nav_request_swaps) {
                intent = new Intent(StudentWorkerActivity.this, RequestSwapsActivity.class);
            } else if (item.getItemId() == R.id.nav_view_swaps) {
                intent = new Intent(StudentWorkerActivity.this, ViewSwapRequestsSW.class);
            } else if (item.getItemId() == R.id.nav_calendar) {
                intent = new Intent(StudentWorkerActivity.this, CalendarSWActivity.class);
            } else if (item.getItemId() == R.id.nav_profile) {
                intent = new Intent(StudentWorkerActivity.this, StudentWorkerActivity.class);
            } else if (item.getItemId() == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                intent = new Intent(StudentWorkerActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                return true; // Exit the listener for logout
            } else {
                return false; // Return false for unhandled items
            }
            startActivity(intent);
            return true; // Return true to indicate the item was handled
        });
    }

    private void retrieveUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String email = document.getString("email");
                                String studentId = document.getString("clockinId");
                                String phone = document.getString("phone");
                                String name = document.getString("name");

                                // Update the UI with the retrieved data
                                emailTextView.setText(email);
                                studentIdTextView.setText(studentId);
                                nameTextView.setText(name);
                                phoneTextView.setText(phone);

                                // Set SSN initially hidden
                                ssnTextView.setText("***-**-****");
                            } else {
                                Toast.makeText(StudentWorkerActivity.this, "No such user data found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(StudentWorkerActivity.this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}