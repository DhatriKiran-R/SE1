package com.example.campuscaferoasterrrr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DiningManagerActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_dining_manager);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
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
            if (item.getItemId() == R.id.nav_shift_management) {
                intent = new Intent(DiningManagerActivity.this, ShiftManagementDMActivity.class);
            } else if (item.getItemId() == R.id.nav_swap_requests) {
                intent = new Intent(DiningManagerActivity.this, SwapRequestsDMActivity.class);
            } else if (item.getItemId() == R.id.nav_student_tracking) {
                intent = new Intent(DiningManagerActivity.this, StudentTrackingDMActivity.class);
            } else if (item.getItemId() == R.id.nav_calendar) {
                intent = new Intent(DiningManagerActivity.this, CalendarDMActivity.class);
            } else if (item.getItemId() == R.id.nav_account) {
                // Show the submenu when Account item is clicked
                showAccountMenu();
                return true; // prevent going to next activity
            } else if (item.getItemId() == R.id.nav_logout) {
                // Handle logout directly
                mAuth.signOut();
                intent = new Intent(DiningManagerActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else {
                return false; // For other items, return false
            }
            startActivity(intent);
            return true; // Return true to indicate the item was handled
        });
    }

    private void showAccountMenu() {
        View view = findViewById(R.id.nav_account); // This view is where the menu will be attached
        PopupMenu popupMenu = new PopupMenu(DiningManagerActivity.this, view);
        getMenuInflater().inflate(R.menu.account_menu, popupMenu.getMenu());

        // Set click listeners for the submenu items (Profile and Logout)
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.nav_profile) {
                // Handle profile option
                Intent intent = new Intent(DiningManagerActivity.this, DiningManagerActivity.class);
                startActivity(intent);
                return true;
            } else if (menuItem.getItemId() == R.id.nav_logout) {
                // Handle logout option
                FirebaseAuth.getInstance().signOut();
                Intent logoutIntent = new Intent(DiningManagerActivity.this, LoginActivity.class);
                startActivity(logoutIntent);
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show(); // Show the submenu
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
                                Toast.makeText(DiningManagerActivity.this, "No such user data found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(DiningManagerActivity.this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}