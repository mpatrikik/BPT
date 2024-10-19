package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
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

public class BikeDetailsActivity extends AppCompatActivity {

    private TextView bikeNameTextView, bikeTotalDistanceTextView;
    private RecyclerView recyclerViewParts, recyclerViewRides;
    private PartAdapter adapterParts;
    private RideAdapter adapterRides;
    private List<Part> partList;
    private List<DashboardActivity.Ride> rideList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bikedetails);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase and UI elements
        bikeNameTextView = findViewById(R.id.bike_name_text_view);
        bikeTotalDistanceTextView = findViewById(R.id.bike_totaldistance_text_view);
        recyclerViewParts = findViewById(R.id.recycler_view_parts);
        recyclerViewRides = findViewById(R.id.recycler_view_rides);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        recyclerViewParts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRides.setLayoutManager(new LinearLayoutManager(this));

        partList = new ArrayList<>();
        rideList = new ArrayList<>();

        adapterParts = new PartAdapter(partList);
        adapterRides = new RideAdapter(rideList);

        recyclerViewParts.setAdapter(adapterParts);
        recyclerViewRides.setAdapter(adapterRides);

        // Get bike name from intent
        String bikeName = getIntent().getStringExtra("bike_name");
        bikeNameTextView.setText(bikeName);

        // Load data from database
        loadPartsForBike(bikeName);
        loadRidesForBike(bikeName);
        calculateTotalDistanceForBike(bikeName);

        // Set swipe refresh listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadPartsForBike(bikeName);
            loadRidesForBike(bikeName);
            calculateTotalDistanceForBike(bikeName);
            swipeRefreshLayout.setRefreshing(false);
        });

        // Handle the homebutton click event
        ImageButton homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
        });

        // Handle the delete button click event
        ImageButton deleteButton = findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Bike")
                    .setMessage("Are you sure you want to delete this bike and all related data?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteBikeAndRelatedData(bikeName))
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        //Handle the add parts button click event
        ImageButton addPartsButton = findViewById(R.id.add_parts_button);
        addPartsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PartsaddingActivity.class);
            intent.putExtra("selected_bike_name", bikeName);
            startActivity(intent);
        });

        //Handle the add rides button click event
        ImageButton addRidesButton = findViewById(R.id.add_rides_button);
        addRidesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualRideAddingActivity.class);
            intent.putExtra("selected_bike_name", bikeName);
            startActivity(intent);
        });

    }

    private void loadPartsForBike(String bikeName) {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("parts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        partList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String bicycle = snapshot.child("bicycle").getValue(String.class);
                            if (bikeName.equals(bicycle)) {
                                String partName = snapshot.child("partName").getValue(String.class);
                                calculateTotalDistanceForPart(partName, bikeName);;
                            }
                        }
                        adapterParts.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("BikeDetailsActivity", "Failed to load parts: ", databaseError.toException());
                        Toast.makeText(BikeDetailsActivity.this, "Error loading parts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void calculateTotalDistanceForPart(String partName, String bikeName) {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("rides")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        double totalDistance = 0;
                        for (DataSnapshot rideSnapshot : dataSnapshot.getChildren()) {
                            String selectedBike = rideSnapshot.child("selectedBicycle").getValue(String.class);
                            List<String> partsUsed = (List<String>) rideSnapshot.child("selectedParts").getValue();
                            if (bikeName.equals(selectedBike) && partsUsed != null && partsUsed.contains(partName)) {
                                String distanceStr = rideSnapshot.child("distance").getValue(String.class);
                                totalDistance += Double.parseDouble(distanceStr);
                            }
                        }
                        partList.add(new Part(partName, totalDistance));
                        adapterParts.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    private void calculateTotalDistanceForBike(String bikeName) {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("rides")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        double totalDistance = 0;
                        for (DataSnapshot rideSnapshot : dataSnapshot.getChildren()) {
                            String selectedBike = rideSnapshot.child("selectedBicycle").getValue(String.class);
                            if (bikeName.equals(selectedBike)) {
                                String distanceStr = rideSnapshot.child("distance").getValue(String.class);
                                totalDistance += Double.parseDouble(distanceStr);
                            }
                        }

                        // Set total distance to the TextView
                        bikeTotalDistanceTextView.setText(String.format(Locale.getDefault(), "%.1f km", totalDistance));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("BikeDetailsActivity", "Failed to calculate total distance: ", databaseError.toException());
                    }
                });
    }

    private void loadRidesForBike(String bikeName) {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("rides")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        rideList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            DashboardActivity.Ride ride = snapshot.getValue(DashboardActivity.Ride.class);
                            if (ride != null && bikeName.equals(ride.getSelectedBicycle())) {
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
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    private void deleteBikeAndRelatedData(String bikeName) {
        String userId = mAuth.getCurrentUser().getUid();

        // 1. Delete current bike
        mDatabase.child("users").child(userId).child("bicycles").orderByValue().equalTo(bikeName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot bikeSnapshot : dataSnapshot.getChildren()) {
                            bikeSnapshot.getRef().removeValue();
                        }
                        Toast.makeText(BikeDetailsActivity.this, "Bike deleted", Toast.LENGTH_SHORT).show();
                        finish(); // Close activity after deletion
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("BikeDetailsActivity", "Failed to delete bike: ", databaseError.toException());
                    }
                });

        // 2. Delete parts for the current bike
        mDatabase.child("users").child(userId).child("parts")
                .orderByChild("bicycle").equalTo(bikeName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot partSnapshot : dataSnapshot.getChildren()) {
                            partSnapshot.getRef().removeValue();
                        }
                        Toast.makeText(BikeDetailsActivity.this, "Related parts deleted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("BikeDetailsActivity", "Failed to delete parts: ", databaseError.toException());
                    }
                });

        // 3. Delete rides for the current bike
        mDatabase.child("users").child(userId).child("rides")
                .orderByChild("selectedBicycle").equalTo(bikeName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot rideSnapshot : dataSnapshot.getChildren()) {
                            rideSnapshot.getRef().removeValue();
                        }
                        Toast.makeText(BikeDetailsActivity.this, "Related rides deleted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("BikeDetailsActivity", "Failed to delete rides: ", databaseError.toException());
                    }
                });
    }
}
