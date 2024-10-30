package com.example.campuscaferoasterrrr;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class RequestSwapsActivity extends AppCompatActivity {

    private RecyclerView shiftsRecyclerView;
    private FirebaseFirestore db;
    private ArrayList<Shift> shiftList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_swaps);

        db = FirebaseFirestore.getInstance();
        shiftsRecyclerView = findViewById(R.id.shifts_recycler_view);
        shiftsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadShifts();
    }

    private void loadShifts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail(); // Replace with actual email from Firebase Auth

        db.collection("shifts")
                .whereEqualTo("studentClockInId", userEmail)
                .whereEqualTo("requestedSwap", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        shiftList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Shift shift = new Shift(
                                    document.getId(),
                                    document.getString("studentClockInId"),
                                    document.getString("startTime"),
                                    document.getString("endTime"),
                                    document.getString("location"),
                                    document.getString("workRole"),
                                    document.getString("date"),
                                    document.getString("weekId"),
                                    document.getLong("duration").intValue()
                            );
                            shiftList.add(shift);
                        }
                        ShiftsAdapter adapter = new ShiftsAdapter(shiftList, "rutwik@gmail.com");
                        shiftsRecyclerView.setAdapter(adapter);
                    } else {
                        Toast.makeText(this, "Failed to load shifts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private class ShiftsAdapter extends RecyclerView.Adapter<ShiftsAdapter.ShiftViewHolder> {

        private ArrayList<Shift> shiftList;
        private String requestedStudentEmail;

        public ShiftsAdapter(ArrayList<Shift> shiftList, String requestedStudentEmail) {
            this.shiftList = shiftList;
            this.requestedStudentEmail = requestedStudentEmail;
        }

        @NonNull
        @Override
        public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_shift, parent, false);
            return new ShiftViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {
            Shift shift = shiftList.get(position);
            String shiftDetail = "Start: " + shift.startTime + " | End: " + shift.endTime + " | Location: " + shift.location + " | Role: " + shift.workRole;
            holder.shiftDetailsTextView.setText(shiftDetail);

            holder.requestSwapButton.setOnClickListener(v -> {
                String coveringEmail = holder.coveringStudentEmail.getText().toString().trim();
                submitSwapRequest(shift, coveringEmail);
            });
        }

        @Override
        public int getItemCount() {
            return shiftList.size();
        }

        public class ShiftViewHolder extends RecyclerView.ViewHolder {
            TextView shiftDetailsTextView;
            EditText coveringStudentEmail;
            Button requestSwapButton;

            public ShiftViewHolder(@NonNull View itemView) {
                super(itemView);
                shiftDetailsTextView = itemView.findViewById(R.id.shift_details);
                coveringStudentEmail = itemView.findViewById(R.id.covering_student_email);
                requestSwapButton = itemView.findViewById(R.id.request_swap_button);
            }
        }
    }

    private void submitSwapRequest(Shift shift, String coveringEmail) {
        String shiftStatus = coveringEmail.isEmpty() ? "Open" : "Close";
        String swapStatus = "Pending";

        SwapRequest swapRequest = new SwapRequest(
                shift.shiftId,
                shift.studentClockInId,
                coveringEmail,
                swapStatus,
                shiftStatus,
                System.currentTimeMillis(),
                shift.startTime,
                shift.endTime,
                String.valueOf(shift.duration),
                shift.date,
                shift.location
        );

        db.collection("swap_requests")
                .add(swapRequest)
                .addOnSuccessListener(documentReference -> {
                    db.collection("shifts")
                            .document(shift.shiftId)
                            .update("requestedSwap", true)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(RequestSwapsActivity.this, "Swap request submitted", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RequestSwapsActivity.this, RequestSwapsActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(RequestSwapsActivity.this, "Swap request added but failed to update shift: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
//                    Toast.makeText(RequestSwapsActivity.this, "Swap request submitted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RequestSwapsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public static class SwapRequest {
        public String shiftId;
        public String requestedStudentEmail;
        public String coveringStudentEmail;
        public String swapStatus;
        public String shiftStatus;
        public long requestTimestamp;
        public String startTime;
        public String endTime;
        public String duration;
        public String date;
        public String location;

        public SwapRequest() {}

        public SwapRequest(String shiftId, String requestedStudentEmail, String coveringStudentEmail,
                           String swapStatus, String shiftStatus, long requestTimestamp,
                           String startTime, String endTime, String duration,
                           String date, String location) {
            this.shiftId = shiftId;
            this.requestedStudentEmail = requestedStudentEmail;
            this.coveringStudentEmail = coveringStudentEmail;
            this.swapStatus = swapStatus;
            this.shiftStatus = shiftStatus;
            this.requestTimestamp = requestTimestamp;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
            this.date = date;
            this.location = location;
        }
    }

    public static class Shift {
        public String shiftId;
        public String studentClockInId;
        public String startTime;
        public String endTime;
        public String location;
        public String workRole;
        public String date;
        public String weekId;
        public int duration;

        public Shift() {}

        public Shift(String shiftId, String studentClockInId, String startTime, String endTime,
                     String location, String workRole, String date, String weekId, int duration) {
            this.shiftId = shiftId;
            this.studentClockInId = studentClockInId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.location = location;
            this.workRole = workRole;
            this.date = date;
            this.weekId = weekId;
            this.duration = duration;
        }
    }
}
