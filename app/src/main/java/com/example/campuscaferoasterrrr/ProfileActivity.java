package com.example.campuscaferoasterrrr;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView emailTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        emailTextView = findViewById(R.id.email_text_view); // Assuming you have a TextView in your layout

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            emailTextView.setText(email); // Display the user's email
            // Load other user data from Firestore if needed
        } else {
            emailTextView.setText("No user logged in");
        }

        // Load user profile data from Firestore or Firebase Auth here if necessary
    }
}