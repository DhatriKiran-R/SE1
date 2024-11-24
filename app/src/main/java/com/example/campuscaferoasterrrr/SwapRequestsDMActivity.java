package com.example.campuscaferoasterrrr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SwapRequestsDMActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<SwapRequest> swapRequestsList;
    private SwapRequestsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap_requests_dmactivity);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        // Adjust for edge-to-edge
        View mainView = findViewById(R.id.main); // Ensure you add this ID in the XML
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.swap_requests_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swapRequestsList = new ArrayList<>();
        adapter = new SwapRequestsAdapter(swapRequestsList);
        recyclerView.setAdapter(adapter);

        fetchSwapRequests();

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            if (item.getItemId() == R.id.nav_shift_management) {
                intent = new Intent(SwapRequestsDMActivity.this, ShiftManagementDMActivity.class);
            } else if (item.getItemId() == R.id.nav_swap_requests) {
                intent = new Intent(SwapRequestsDMActivity.this, SwapRequestsDMActivity.class);
            } else if (item.getItemId() == R.id.nav_student_tracking) {
                intent = new Intent(SwapRequestsDMActivity.this, StudentTrackingDMActivity.class);
            } else if (item.getItemId() == R.id.nav_calendar) {
                intent = new Intent(SwapRequestsDMActivity.this, CalendarDMActivity.class);
            } else if (item.getItemId() == R.id.nav_account) {
                // Show the submenu when Account item is clicked
                showAccountMenu();
                return true; // prevent going to next activity
            } else if (item.getItemId() == R.id.nav_logout) {
                // Handle logout directly
                mAuth.signOut();
                intent = new Intent(SwapRequestsDMActivity.this, LoginActivity.class);
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

    private void sendNotification(String recipientUserId, String title, String message) {
        // Fetch the FCM token of the recipient user from Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        db.collection("users").whereEqualTo("email", recipientUserId)
//                .get()
//                .addOnSuccessListener(document -> {
//                    if (document.exists()) {
//                        String token = document.getString("fcmToken");
//                        if (token != null) {
//                            // Send the notification if token exists
//                            sendNotificationToUser(token, title, message);
//                        }
//                    }
//                });

        db.collection("users")
                .whereEqualTo("email", recipientUserId) // Assuming the "users" collection has an "email" field
                .get()
                .addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful() && !userTask.getResult().isEmpty()) {
                        // Assuming "name" field exists in the "users" document
                        String fcmToken = userTask.getResult().getDocuments().get(0).getString("fcmToken");

                        // Add the studentName to the shift document data
                        sendNotificationToUser(fcmToken, title, message);
                    }


                });
    }

    private void sendNotificationToUser(String token, String title, String body) {
        // Prepare the notification data
        Map<String, String> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("body", body);

        // Construct the RemoteMessage
        RemoteMessage message = new RemoteMessage.Builder(token)
                .addData("title", title)
                .addData("body", body)
                .build();

        // Send the message using FirebaseMessaging and handle success/failure
        FirebaseMessaging.getInstance().send(message);

    }





    private void showAccountMenu() {
        View view = findViewById(R.id.nav_account); // This view is where the menu will be attached
        PopupMenu popupMenu = new PopupMenu(SwapRequestsDMActivity.this, view);
        getMenuInflater().inflate(R.menu.account_menu, popupMenu.getMenu());

        // Set click listeners for the submenu items (Profile and Logout)
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.nav_profile) {
                // Handle profile option
                Intent intent = new Intent(SwapRequestsDMActivity.this, DiningManagerActivity.class);
                startActivity(intent);
                return true;
            } else if (menuItem.getItemId() == R.id.nav_logout) {
                // Handle logout option
                FirebaseAuth.getInstance().signOut();
                Intent logoutIntent = new Intent(SwapRequestsDMActivity.this, LoginActivity.class);
                startActivity(logoutIntent);
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show(); // Show the submenu
    }

    private void fetchSwapRequests() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("swap_requests")
                .whereEqualTo("swapStatus", "Pending")
                .whereEqualTo("shiftStatus", "Closed")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        swapRequestsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            SwapRequest request = document.toObject(SwapRequest.class);
                            request.setId(document.getId());

                            // Fetch names for requested and covering students
                            fetchStudentName(request.getRequestedStudentEmail(), requestedName -> {
                                request.setRequestedStudentName(requestedName);
                                fetchStudentName(request.getCoveringStudentEmail(), coveringName -> {
                                    request.setCoveringStudentName(coveringName);
                                    swapRequestsList.add(request);

                                    // Notify adapter after fetching all names
                                    if (swapRequestsList.size() == task.getResult().size()) {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            });
                        }
                    } else {
                        Toast.makeText(this, "Failed to fetch swap requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void handleSwapRequest(SwapRequest request, boolean isApproved) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (isApproved) {
            // Approve the swap request
            db.collection("swap_requests")
                    .document(request.getId())
                    .update("swapStatus", "Approved")
                    .addOnSuccessListener(aVoid -> {
                        db.collection("shifts")
                                .document(request.getShiftId())
                                .update("studentClockInId", request.getCoveringStudentEmail())
                                .addOnSuccessListener(aVoid1 -> {
                                    // Notify both students
                                    sendNotification(request.getRequestedStudentEmail(), "Swap Request Approved", "Your swap request has been approved.");
                                    sendNotification(request.getCoveringStudentEmail(), "Swap Request Approved", "You are now covering the shift.");
                                    Toast.makeText(this, "Swap approved", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, SwapRequestsDMActivity.class));
                                    finish(); // Refresh the list
                                });
                    });
        } else {
            // Deny the swap request
            db.collection("swap_requests")
                    .document(request.getId())
                    .update("swapStatus", "Denied")
                    .addOnSuccessListener(aVoid -> {
                        // Notify both students
                        sendNotification(request.getRequestedStudentEmail(), "Swap Request Denied", "Your swap request has been denied.");
                        sendNotification(request.getCoveringStudentEmail(), "Swap Request Denied", "The swap request you offered has been denied.");
                        Toast.makeText(this, "Swap denied", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, SwapRequestsDMActivity.class));
                        finish();  // Refresh the list
                    });
        }
    }




    private class SwapRequestsAdapter extends RecyclerView.Adapter<SwapRequestsAdapter.ViewHolder> {

        private final ArrayList<SwapRequest> swapRequests;

        public SwapRequestsAdapter(ArrayList<SwapRequest> swapRequests) {
            this.swapRequests = swapRequests;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_swap_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SwapRequest request = swapRequests.get(position);
            holder.bind(request);
        }

        @Override
        public int getItemCount() {
            return swapRequests.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView swapRequestDetails;
            private final Button approveButton, denyButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                swapRequestDetails = itemView.findViewById(R.id.swap_request_details);
                approveButton = itemView.findViewById(R.id.approve_button);
                denyButton = itemView.findViewById(R.id.deny_button);
            }

            public void bind(SwapRequest request) {
                swapRequestDetails.setText(
                        "Requested by: " + request.getRequestedStudentName() + " (" + request.getRequestedStudentEmail() + ")" +
                                "\nCovering: " + request.getCoveringStudentName() + " (" + request.getCoveringStudentEmail() + ")" +
                                "\nDate: " + request.getDate() +
                                "\nTime: " + request.getStartTime().substring(11) + " - " + request.getEndTime().substring(11) +
                                "\nCovering Student Work Hours: " + request.getTotalWorkHours()
                );

                approveButton.setOnClickListener(v -> handleSwapRequest(request, true));
                denyButton.setOnClickListener(v -> handleSwapRequest(request, false));
            }
        }
    }

    private void fetchStudentName(String email, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String name = task.getResult().getDocuments().get(0).getString("name");
                        callback.onCallback(name);
                    } else {
                        callback.onCallback("Unknown"); // Default if no name is found
                    }
                });
    }

    private interface FirestoreCallback {
        void onCallback(String name);
    }


    private static class SwapRequest {
        private String id;
        private String coveringStudentEmail;
        private String requestedStudentEmail;
        private String shiftId;
        private String date;
        private String startTime;
        private String endTime;
        private String location;
        private String swapStatus;

        private Long totalWorkHours;
        private String requestedStudentName; // New field
        private String coveringStudentName;

        // Empty constructor for Firestore
        public SwapRequest() {}

        // Getters and setters
        public String getId() { return id; }
        public String getRequestedStudentName() { return requestedStudentName; }
        public void setRequestedStudentName(String requestedStudentName) { this.requestedStudentName = requestedStudentName; }
        public String getCoveringStudentName() { return coveringStudentName; }
        public void setCoveringStudentName(String coveringStudentName) { this.coveringStudentName = coveringStudentName; }
        public void setId(String id) { this.id = id; }
        public String getCoveringStudentEmail() { return coveringStudentEmail; }
        public void setCoveringStudentEmail(String coveringStudentEmail) { this.coveringStudentEmail = coveringStudentEmail; }
        public String getRequestedStudentEmail() { return requestedStudentEmail; }
        public void setRequestedStudentEmail(String requestedStudentEmail) { this.requestedStudentEmail = requestedStudentEmail; }

        public Long getTotalWorkHours() { return totalWorkHours; }
        public void setTotalWorkHours(Long totalWorkHours) { this.totalWorkHours = totalWorkHours; }
        public String getShiftId() { return shiftId; }
        public void setShiftId(String shiftId) { this.shiftId = shiftId; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getSwapStatus() { return swapStatus; }
        public void setSwapStatus(String swapStatus) { this.swapStatus = swapStatus; }
    }
}

