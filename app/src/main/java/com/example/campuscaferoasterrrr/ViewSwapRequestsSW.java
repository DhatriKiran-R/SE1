package com.example.campuscaferoasterrrr;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ViewSwapRequestsSW extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<SwapRequest> openSwapRequests;
    private SwapRequestsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_swap_requests_sw);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.swap_requests_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        openSwapRequests = new ArrayList<>();
        adapter = new SwapRequestsAdapter(openSwapRequests);
        recyclerView.setAdapter(adapter);

        fetchOpenSwapRequests();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Intent intent;
            if (item.getItemId() == R.id.nav_request_swaps) {
                intent = new Intent(ViewSwapRequestsSW.this, RequestSwapsActivity.class);
            } else if (item.getItemId() == R.id.nav_view_swaps) {
                intent = new Intent(ViewSwapRequestsSW.this, ViewSwapRequestsSW.class);
            } else if (item.getItemId() == R.id.nav_profile) {
                intent = new Intent(ViewSwapRequestsSW.this, StudentWorkerActivity.class);
            }  else if (item.getItemId() == R.id.nav_calendar) {
                intent = new Intent(ViewSwapRequestsSW.this, CalendarSWActivity.class);
            } else if (item.getItemId() == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                intent = new Intent(ViewSwapRequestsSW.this, LoginActivity.class);
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

    private void fetchOpenSwapRequests() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        db.collection("swap_requests")
                .whereEqualTo("swapStatus", "Pending")
                .whereEqualTo("shiftStatus", "Open")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        openSwapRequests.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            SwapRequest request = document.toObject(SwapRequest.class);
                            System.out.println(document);
                            request.setId(document.getId());
                            if (userEmail != request.getRequestedStudentEmail()) {

                            fetchStudentDetails(request.getRequestedStudentEmail(), (name, workRole) -> {
                                request.setRequestedStudentName(name);
                                request.setRequestedStudentRole(workRole);

                                openSwapRequests.add(request);

                                // Notify adapter after all details are fetched
                                if (openSwapRequests.size() == task.getResult().size()) {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                        }
                    } else {
                        Toast.makeText(this, "Failed to fetch open swap requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void claimShift(SwapRequest request) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Step 1: Check if more than 20 hours have passed since the shift start time
        long currentTime = System.currentTimeMillis();
        long shiftStartTime = getTimeInMillis(request.getStartTime()); // Convert start time to milliseconds
        long timeDifference = currentTime - shiftStartTime;


        System.out.println(1);
        // Step 2: Check if the student already has another shift during the same time
        checkForOverlappingShifts(request, userEmail, (hasConflict) -> {
            System.out.println(2);
            if (hasConflict) {
                Toast.makeText(this, "You already have another shift at this time.", Toast.LENGTH_SHORT).show();
            } else {
                System.out.println(3);
                // Step 3: Check if the student has exceeded weekly work limit
                checkWeeklyWorkLimit(userEmail, request.getWeekId(), request.getDuration(), (exceededLimit) -> {
                    System.out.println(4);
                    if (exceededLimit) {
                        Toast.makeText(this, "You have exceeded your weekly work limit.", Toast.LENGTH_SHORT).show();
                    } else {
                        System.out.println(5);
                        // Proceed with claiming the shift if no conflicts or limits
                        claimShiftInFirestore(request, userEmail);
                    }
                });
            }
        });
    }

    // Step 3A: Check for overlapping shifts in the same time frame
//    private void checkForOverlappingShifts(SwapRequest request, String userEmail, OverlapCallback callback) {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
//
//        try {
//            Date newStart = sdf.parse(request.startTime);
//            Date newEnd = sdf.parse(request.endTime);
//
//            db.collection("shifts")
//                    .whereEqualTo("studentClockInId", userEmail)
//                    .get()
//                    .addOnCompleteListener(task -> {
//                        if (task.isSuccessful()) {
//                            boolean hasOverlap = false;
//
//                            for (QueryDocumentSnapshot doc : task.getResult()) {
//                                String existingStartTime = doc.getString("startTime");
//                                String existingEndTime = doc.getString("endTime");
//
//                                Date existingStart = null;
//                                try {
//                                    existingStart = sdf.parse(existingStartTime);
//                                } catch (ParseException e) {
//                                    throw new RuntimeException(e);
//                                }
//                                Date existingEnd = null;
//                                try {
//                                    existingEnd = sdf.parse(existingEndTime);
//                                } catch (ParseException e) {
//                                    throw new RuntimeException(e);
//                                }
//
//                                if (newStart.before(existingEnd) && newEnd.after(existingStart)) {
//                                    callback.onCallback(true);
//                                    break;
//                                } else{
//                                    callback.onCallback(false);
//                                }
//                            }
//
//                        } else {
//                            Toast.makeText(this, "Error checking overlapping shifts.", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//        } catch (ParseException e) {
//            e.printStackTrace();
//            Toast.makeText(this, "Error parsing shift times.", Toast.LENGTH_SHORT).show();
//        }
//    }
    private void checkForOverlappingShifts(SwapRequest request, String userEmail, OverlapCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        try {
            Date newStart = sdf.parse(request.getStartTime());
            Date newEnd = sdf.parse(request.getEndTime());

            db.collection("shifts")
                    .whereEqualTo("studentClockInId", userEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            boolean hasOverlap = false;

                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String existingStartTime = doc.getString("startTime");
                                String existingEndTime = doc.getString("endTime");

                                try {
                                    Date existingStart = sdf.parse(existingStartTime);
                                    Date existingEnd = sdf.parse(existingEndTime);

                                    // Check for overlap
                                    if (newStart.before(existingEnd) && newEnd.after(existingStart)) {
                                        hasOverlap = true;
                                        break; // Exit loop early if overlap is found
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }

                            // Invoke callback after checking all shifts
                            callback.onCallback(hasOverlap);
                        } else {
                            Toast.makeText(this, "Error checking overlapping shifts.", Toast.LENGTH_SHORT).show();
                            callback.onCallback(false); // Default to no overlap on failure
                        }
                    });
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing shift times.", Toast.LENGTH_SHORT).show();
            callback.onCallback(false); // Default to no overlap if parsing fails
        }
    }


    // Step 3B: Check if the student has exceeded the weekly work limit
    private void checkWeeklyWorkLimit(String userEmail, String shiftWeekId, int duration, WeeklyLimitCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        // Query to get all shifts for the student within the same week
        db.collection("shifts")
                .whereEqualTo("studentClockInId", userEmail)
                .whereEqualTo("weekId", shiftWeekId)// Only consider shifts that are already claimed
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        long totalWorkedHours = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {

                            totalWorkedHours += document.get("duration", Integer.class);// Convert to hours
                        }
                        totalWorkedHours += duration;
                        // Assume the weekly work limit is 40 hours
                        if (totalWorkedHours >= 20) {
                            callback.onCallback(true); // Exceeded limit
                        } else {
                            callback.onCallback(false); // No limit exceeded
                        }
                    } else {
                        callback.onCallback(false); // Handle any errors gracefully
                    }
                });
    }

    // Proceed with claiming the shift in Firestore
    private void claimShiftInFirestore(SwapRequest request, String userEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: Update the swap_requests table
        db.collection("swap_requests")
                .document(request.getId())
                .update("coveringStudentEmail", userEmail,
                        "shiftStatus", "Closed")
                .addOnSuccessListener(aVoid -> {
                    // Step 2: Update the shifts table
//                    db.collection("shifts")
//                            .document(request.getShiftId()) // Use the shift ID from the request
//                            .update("studentClockInId", userEmail) // Reset the swap request status
//                            .addOnSuccessListener(aVoid2 -> {
//                                // Step 3: Notify user and refresh
//                                Toast.makeText(this, "Shift claimed successfully", Toast.LENGTH_SHORT).show();
//                                fetchOpenSwapRequests(); // Refresh the swap requests list
//                            })
//                            .addOnFailureListener(e -> {
//                                // Handle failure for the shifts update
//                                Toast.makeText(this, "Failed to update shift details", Toast.LENGTH_SHORT).show();
//                            });
                    Toast.makeText(this, "Shift swap request successfully sent to the dining manager", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Handle failure for the swap_requests update
                    Toast.makeText(this, "Failed to claim shift", Toast.LENGTH_SHORT).show();
                });
    }


    // Helper function to convert the time (String) to milliseconds
    private long getTimeInMillis(String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(timeStr);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Callback interfaces
    private interface OverlapCallback {
        void onCallback(boolean hasConflict);
    }

    private interface WeeklyLimitCallback {
        void onCallback(boolean exceededLimit);
    }


    private class SwapRequestsAdapter extends RecyclerView.Adapter<SwapRequestsAdapter.ViewHolder> {

        private final ArrayList<SwapRequest> swapRequests;

        public SwapRequestsAdapter(ArrayList<SwapRequest> swapRequests) {
            this.swapRequests = swapRequests;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
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
            private final TextView textView;
            private final Button claimButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
                claimButton = new Button(ViewSwapRequestsSW.this);
                claimButton.setText("I'll Cover");

                ((ViewGroup) itemView).addView(claimButton);
            }

            public void bind(SwapRequest request) {
                String startTime = request.getStartTime().substring(11); // Extract time part only
                String endTime = request.getEndTime().substring(11); // Extract time part only

                textView.setText(
                        "Request by: " + request.getRequestedStudentName() +
                                "\nRole: " + request.getRequestedStudentRole() +
                                "\nLocation: " + request.getLocation() +
                                "\nTime: " + startTime + " - " + endTime
                );

                claimButton.setOnClickListener(v -> claimShift(request));
            }

        }
    }
    private void fetchStudentDetails(String email, FirestoreCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                        String name = document.getString("name");
                        String workRole = document.getString("workRole");
                        callback.onCallback(name, workRole);
                    } else {
                        callback.onCallback("Unknown", "Unknown Role"); // Defaults if no data is found
                    }
                });
    }

    private interface FirestoreCallback {
        void onCallback(String name, String workRole);
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
        private String shiftStatus;

        private String requestedStudentName; // New field
        private String requestedStudentRole;

        private String weekId;

        private int duration;

        // Empty constructor for Firestore
        public SwapRequest() {}

        // Getters and setters
        public String getRequestedStudentName() { return requestedStudentName; }
        public void setRequestedStudentName(String requestedStudentName) { this.requestedStudentName = requestedStudentName; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public String getWeekId() { return weekId; }
        public void setWeekId(String weekId) { this.weekId = weekId; }
        public String getRequestedStudentRole() { return requestedStudentRole; }
        public void setRequestedStudentRole(String requestedStudentRole) { this.requestedStudentRole = requestedStudentRole; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCoveringStudentEmail() { return coveringStudentEmail; }
        public void setCoveringStudentEmail(String coveringStudentEmail) { this.coveringStudentEmail = coveringStudentEmail; }
        public String getRequestedStudentEmail() { return requestedStudentEmail; }
        public void setRequestedStudentEmail(String requestedStudentEmail) { this.requestedStudentEmail = requestedStudentEmail; }
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
        public String getShiftStatus() { return shiftStatus; }
        public void setShiftStatus(String shiftStatus) { this.shiftStatus = shiftStatus; }
    }
}
