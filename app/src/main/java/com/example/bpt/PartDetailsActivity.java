package com.example.bpt;

import android.content.Intent;
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
    private String partName, userId;
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

        partNameTextView.setText(partName);

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
            mDatabase.child("users").child(userId).child("parts").orderByChild("partName").equalTo(partName)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            boolean hasRepeatingServiceInterval = false;
                            for (DataSnapshot partSnapshot : dataSnapshot.getChildren()) {
                                DataSnapshot mainServicesSnapshot = partSnapshot.child("MAINSERVICES");
                                for (DataSnapshot serviceIntervalSnapshot : mainServicesSnapshot.getChildren()) {
                                    Boolean isRepeat = serviceIntervalSnapshot.child("serviceInterval").child("isRepeat").getValue(Boolean.class);
                                    if (Boolean.TRUE.equals(isRepeat)) {
                                        hasRepeatingServiceInterval = true;
                                        break;
                                    }
                                }
                                if (hasRepeatingServiceInterval) break;
                            }
                            if (hasRepeatingServiceInterval) {
                                Intent intent = new Intent(PartDetailsActivity.this, ServiceAddingActivity.class);
                                intent.putExtra("partName", partName);
                                startActivity(intent);
                            } else {
                                Toast.makeText(PartDetailsActivity.this, "No repeating service interval available.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("PartDetailsActivity", "Error checking repeating service intervals", databaseError.toException());
                        }
                    });
        });

        addRideButton = findViewById(R.id.add_ride_button);
        addRideButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualRideAddingActivity.class);
            intent.putExtra("selected_part_name", partName);
            startActivity(intent);
        });

        addServiceIntervalsButton = findViewById(R.id.add_service_intervals_button);
        addServiceIntervalsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ServiceIntervalAddingActivity.class);
            intent.putExtra("selected_part_name", partName);
            startActivity(intent);
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

}