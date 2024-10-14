package com.example.bpt;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

public class BikeDetailsActivity extends AppCompatActivity {

    private TextView bikeNameTextView;
    private RecyclerView recyclerViewParts, recyclerViewRides;
    private PartAdapter adapterParts;
    private RideAdapter adapterRides;
    private List<Part> partList;
    private List<DashboardActivity.Ride> rideList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bikedetails);

        // Initialize Firebase and UI elements
        mDatabase = FirebaseDatabase.getInstance().getReference();
        bikeNameTextView = findViewById(R.id.bike_name_text_view);
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

        // Set swipe refresh listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadPartsForBike(bikeName);
            loadRidesForBike(bikeName);
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadPartsForBike(String bikeName) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("parts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        partList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String partName = snapshot.child("partName").getValue(String.class);
                            calculateTotalDistanceForPart(partName, bikeName);
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
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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

    private void loadRidesForBike(String bikeName) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
                        adapterRides.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }
}
