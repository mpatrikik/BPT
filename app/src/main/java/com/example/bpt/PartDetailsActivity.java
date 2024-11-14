package com.example.bpt;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class PartDetailsActivity extends AppCompatActivity {

    private TextView partNameTextView, usedBikeTextView, partTotalDistanceTextView;
    private ImageButton addServiceIntervalsButton, addServiceButton, addRideButton;
    private RecyclerView recyclerViewServiceIntervals , recyclerViewServices, recyclerViewRides;
    private RideAdapter adapterRides;
    private List<DashboardActivity.Ride> rideList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String partName, userId, partId;
    private List<String> usedBikes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partdetails);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        partNameTextView = findViewById(R.id.part_name_text_view);
        usedBikeTextView = findViewById(R.id.used_bike_forcurrentpart_text_view);
        partTotalDistanceTextView = findViewById(R.id.part_totaldistance_text_view);
        recyclerViewServiceIntervals = findViewById(R.id.recycler_view_service_intervals);
        recyclerViewServices = findViewById(R.id.recycler_view_services);
        recyclerViewRides = findViewById(R.id.recycler_view_rides);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        addServiceIntervalsButton = findViewById(R.id.add_service_intervals_button);
        addServiceButton = findViewById(R.id.add_service_button);
        addRideButton = findViewById(R.id.add_ride_button);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        partName = getIntent().getStringExtra("part_name");
        if (partName != null) {
            partNameTextView.setText(partName);
            loadServiceIntervalsForPart(partName);
        } else {
            Toast.makeText(this, "Error: No part name provided", Toast.LENGTH_SHORT).show();
            finish();
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadServiceIntervalsForPart(partName);
            loadRidesForPart(partName);
            loadBikesForPart(partName);
            calculateTotalDistanceForPart(partName);
            swipeRefreshLayout.setRefreshing(false);
                });

        recyclerViewRides.setLayoutManager(new LinearLayoutManager(this));
        rideList = new ArrayList<>();
        adapterRides = new RideAdapter(rideList);
        recyclerViewRides.setAdapter(adapterRides);

        loadBikesForPart(partName);
        calculateTotalDistanceForPart(partName);
        loadServiceIntervalsForPart(partName);
        loadRidesForPart(partName);

        ImageButton homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
        });

        ImageButton deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Part")
                    .setMessage("Are you sure you want to delete this part?")
                    .setPositiveButton("Yes", (dialog, which) -> deletePart(partName))
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        addServiceButton.setOnClickListener(v -> {
                    fetchRepeatingServiceIntervalIdAndNavigate();
        });

        addRideButton = findViewById(R.id.add_ride_button);
        addRideButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualRideAddingActivity.class);
            intent.putExtra("selected_part_name", partName);
            intent.putStringArrayListExtra("used_bikes", new ArrayList<>(usedBikes));
            startActivity(intent);
        });

        addServiceIntervalsButton.setOnClickListener(v -> { checkServiceIntervalsBeforeAdding(); });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }


    }


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void fetchRepeatingServiceIntervalIdAndNavigate() {
        mDatabase.child("users").child(userId).child("parts").orderByChild("partName").equalTo(partName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot partSnapshot : dataSnapshot.getChildren()) {
                            partId = partSnapshot.getKey();
                            for (DataSnapshot serviceIntervalSnapshot : partSnapshot.child("MAINSERVICES").getChildren()) {
                                Boolean isRepeat = serviceIntervalSnapshot.child("serviceInterval").child("isRepeat").getValue(Boolean.class);
                                if (Boolean.TRUE.equals(isRepeat)) {
                                    Intent intent = new Intent(PartDetailsActivity.this, ServiceAddingActivity.class);
                                    intent.putExtra("partName", partName);
                                    intent.putExtra("serviceIntervalId", serviceIntervalSnapshot.getKey());
                                    intent.putExtra("partId", partId);
                                    startActivity(intent);
                                    return;
                                }
                            }
                        }
                        Toast.makeText(PartDetailsActivity.this, "No repeating service interval available.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("PartDetailsActivity", "Error checking repeating service intervals", databaseError.toException());
                    }
                });
    }

    private void checkServiceIntervalsBeforeAdding() {
        mDatabase.child("users").child(userId).child("parts").orderByChild("partName").equalTo(partName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean hasRepeatingInterval = false;
                        boolean hasNonRepeatingInterval = false;

                        for (DataSnapshot partSnapshot : dataSnapshot.getChildren()) {
                            DataSnapshot mainServicesSnapshot = partSnapshot.child("MAINSERVICES");
                            for (DataSnapshot serviceIntervalSnapshot : mainServicesSnapshot.getChildren()) {
                                Boolean isRepeat = serviceIntervalSnapshot.child("serviceInterval").child("isRepeat").getValue(Boolean.class);
                                if (Boolean.TRUE.equals(isRepeat)) {
                                    hasRepeatingInterval = true;
                                } else if (Boolean.FALSE.equals(isRepeat)) {
                                    hasNonRepeatingInterval = true;
                                }
                            }

                            // Ha mindkét típusú intervallum létezik, megakadályozzuk az új hozzáadását
                            if (hasRepeatingInterval && hasNonRepeatingInterval) {
                                Toast.makeText(PartDetailsActivity.this, "You can only add one repeating and one non-repeating service interval per part.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Ha csak egy adott típusú intervallum létezik, engedjük a másik hozzáadását
                            Intent intent = new Intent(PartDetailsActivity.this, ServiceIntervalAddingActivity.class);
                            intent.putExtra("selected_part_name", partName);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("PartDetailsActivity", "Error checking service intervals", databaseError.toException());
                    }
                });
    }

    private void loadBikesForPart(String partName) {
        mDatabase.child("users").child(userId).child("parts")
                .orderByChild("partName").equalTo(partName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        usedBikes.clear();
                        for (DataSnapshot partSnapshot : dataSnapshot.getChildren()) {
                            String bicycle = partSnapshot.child("bicycle").getValue(String.class);
                            if (!usedBikes.contains(bicycle)) {
                                usedBikes.add(bicycle);
                            }
                        }
                        usedBikeTextView.setText(String.join(", ", usedBikes));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("PartDetailsActivity", "Failed to load used bikes: ", databaseError.toException());
                    }
                });
    }

    private void calculateTotalDistanceForPart(String partName) {
        mDatabase.child("users").child(userId).child("rides")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        double totalDistance = 0;
                        for (DataSnapshot rideSnapshot : dataSnapshot.getChildren()) {
                            List<String> partsUsed = (List<String>) rideSnapshot.child("selectedParts").getValue();
                            if (partsUsed != null && partsUsed.contains(partName)) {
                                String distanceStr = rideSnapshot.child("distance").getValue(String.class);
                                totalDistance += Double.parseDouble(distanceStr);
                            }
                        }
                        partTotalDistanceTextView.setText(String.format(Locale.getDefault(), "%.1f km", totalDistance));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("PartDetailsActivity", "Failed to calculate total distance: ", databaseError.toException());
                    }
                });
    }

    private void deletePart(String partName) {
        mDatabase.child("users").child(userId).child("parts").orderByChild("partName").equalTo(partName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot bikeSnapshot : dataSnapshot.getChildren()) {
                            bikeSnapshot.getRef().removeValue();
                        }
                        Toast.makeText(PartDetailsActivity.this, "Part deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("PartDetailsActivity", "Failed to delete bike: ", databaseError.toException());
                    }
                });
    }

    private void loadServiceIntervalsForPart(String partName) {
        mDatabase.child("users").child(userId).child("parts")
                .orderByChild("partName").equalTo(partName)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot partSnapshot : dataSnapshot.getChildren()) {
                                String partId = partSnapshot.getKey();
                                List<DataSnapshot> serviceIntervalsList = new ArrayList<>();
                                DataSnapshot mainServicesSnapshot = partSnapshot.child("MAINSERVICES");
                                for (DataSnapshot serviceIntervalSnapshot : mainServicesSnapshot.getChildren()) {
                                    serviceIntervalsList.add(serviceIntervalSnapshot);
                                }

                                // Az adapter létrehozása a partId átadásával
                                recyclerViewServiceIntervals.setLayoutManager(new LinearLayoutManager(PartDetailsActivity.this));
                                ServiceIntervalsAdapter adapter = new ServiceIntervalsAdapter(PartDetailsActivity.this, serviceIntervalsList, partId);
                                recyclerViewServiceIntervals.setAdapter(adapter);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("PartDetailsActivity", "Failed to load service intervals: ", databaseError.toException());
                    }
                });
    }

    private void loadRidesForPart(String partName) {
        mDatabase.child("users").child(userId).child("rides")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        rideList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            DashboardActivity.Ride ride = snapshot.getValue(DashboardActivity.Ride.class);
                            if (ride != null && ride.selectedParts() != null && ride.selectedParts().contains(partName)) {
                                rideList.add(ride);
                            }
                        }
                        Collections.sort(rideList, (r1, r2) -> {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
                            try {
                                String dateTime1 = r1.getDate() + " " + r1.getTime();
                                String dateTime2 = r2.getDate() + " " + r2.getTime();
                                return sdf.parse(dateTime2).compareTo(sdf.parse(dateTime1));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            return 0;
                        });
                        adapterRides.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("PartDetailsActivity", "Failed to load rides: ", databaseError.toException());
                    }
                });
    }

    public void calculateTotalDistanceForPart(String partId, DistanceCallback callback) {
        mDatabase.child("users").child(userId).child("parts").child(partId).child("partName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String partName = snapshot.getValue(String.class);
                        if (partName == null) {
                            Log.e("calculateTotalDistance", "Part name not found for partId: " + partId);
                            callback.onDistanceCalculated(0);
                            return;
                        }
                        mDatabase.child("users").child(userId).child("rides")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        double totalDistance = 0;
                                        for (DataSnapshot rideSnapshot : dataSnapshot.getChildren()) {
                                            List<String> partsUsed = (List<String>) rideSnapshot.child("selectedParts").getValue();
                                            if (partsUsed != null && partsUsed.contains(partName)) {
                                                String distanceStr = rideSnapshot.child("distance").getValue(String.class);
                                                totalDistance += Double.parseDouble(distanceStr);
                                            }
                                        }
                                        callback.onDistanceCalculated(totalDistance);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Log.e("calculateTotalDistance", "Failed to calculate total distance: ", databaseError.toException());
                                        callback.onDistanceCalculated(0);
                                    }
                                });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("calculateTotalDistance", "Failed to retrieve part name: ", error.toException());
                        callback.onDistanceCalculated(0);
                    }
                });
    }

    public void calculateTotalDistanceSinceDateTime(String partId, String lastServiceDate, String lastServiceTime, DistanceCallback callback) {
        mDatabase.child("users").child(userId).child("parts").child(partId).child("partName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String partName = snapshot.getValue(String.class);
                        if (partName == null) {
                            Log.e("PartDetailsActivity", "Part name not found for partId: " + partId);
                            callback.onDistanceCalculated(0);
                            return;
                        }

                        mDatabase.child("users").child(userId).child("rides")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        double totalDistance = 0;
                                        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

                                        try {
                                            Date lastServiceDateTime = dateTimeFormat.parse(lastServiceDate + " " + lastServiceTime);

                                            for (DataSnapshot rideSnapshot : dataSnapshot.getChildren()) {
                                                String rideDate = rideSnapshot.child("date").getValue(String.class);
                                                String rideTime = rideSnapshot.child("time").getValue(String.class);

                                                if (rideDate != null && rideTime != null) {
                                                    Date rideDateTime = dateTimeFormat.parse(rideDate + " " + rideTime);
                                                    if (rideDateTime != null && rideDateTime.after(lastServiceDateTime)) {
                                                        List<String> partsUsed = (List<String>) rideSnapshot.child("selectedParts").getValue();

                                                        if (partsUsed != null && partsUsed.contains(partName)) {
                                                            String distanceStr = rideSnapshot.child("distance").getValue(String.class);
                                                            try {
                                                                double rideDistance = Double.parseDouble(distanceStr);
                                                                totalDistance += rideDistance;
                                                            } catch (NumberFormatException e) {
                                                            }
                                                        }
                                                    } else {
                                                        Log.d("PartDetailsActivity", "Ride dateTime is not after lastServiceDateTime, skipping ride");
                                                    }
                                                }
                                            }
                                        } catch (ParseException e) {
                                            Log.e("PartDetailsActivity", "Date parsing error", e);
                                        }
                                        callback.onDistanceCalculated(totalDistance);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        callback.onDistanceCalculated(0);
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onDistanceCalculated(0);
                    }
                });
    }

    // Callback interface definition
    public interface DistanceCallback {
        void onDistanceCalculated(double totalDistance);
    }

}