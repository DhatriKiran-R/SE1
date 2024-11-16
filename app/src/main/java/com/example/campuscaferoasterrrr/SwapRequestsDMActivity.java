package com.example.campuscaferoasterrrr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class SwapRequestsDMActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<SwapRequest> swapRequestsList;
    private SwapRequestsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap_requests_dmactivity);

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
    }

    private void fetchSwapRequests() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("swap_requests")
                .whereEqualTo("swapStatus", "Pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        swapRequestsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            SwapRequest request = document.toObject(SwapRequest.class);
                            request.setId(document.getId());
                            swapRequestsList.add(request);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to fetch swap requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleSwapRequest(SwapRequest request, boolean isApproved) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        System.out.println(isApproved);
        if (isApproved) {
            db.collection("swap_requests")
                    .document(request.getId())
                    .update("swapStatus", "Approved")
                    .addOnSuccessListener(aVoid -> {
                        db.collection("shifts")
                                .document(request.getShiftId())
                                .update("studentClockInId", request.getCoveringStudentEmail())
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(this, "Swap approved", Toast.LENGTH_SHORT).show();
                                    fetchSwapRequests();
                                });
                    });
        } else {
            db.collection("swap_requests")
                    .document(request.getId())
                    .update("swapStatus", "Denied")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Swap denied", Toast.LENGTH_SHORT).show();
                        fetchSwapRequests();
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
                        "Request by: " + request.getRequestedStudentEmail() +
                                "\nCovering: " + request.getCoveringStudentEmail() +
                                "\nDate: " + request.getDate() +
                                "\nTime: " + request.getStartTime() + " - " + request.getEndTime()
                );

                approveButton.setOnClickListener(v -> handleSwapRequest(request, true));
                denyButton.setOnClickListener(v -> handleSwapRequest(request, false));
            }
        }
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

        // Empty constructor for Firestore
        public SwapRequest() {}

        // Getters and setters
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
    }
}

