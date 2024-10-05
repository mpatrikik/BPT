package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

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

public class DashboardActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerViewRides, recyclerViewBikes;
    private ItemAdapter adapterRides;
    private BikeAdapter adapterBikes;
    private List<String> bikeList;
    private List<String> distanceList;
    private List<Ride> rideList;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Recycler view for bikes
        recyclerViewBikes = findViewById(R.id.recycler_view_bikes);
        recyclerViewBikes.setLayoutManager(new LinearLayoutManager(this));
        bikeList = new ArrayList<>();
        distanceList = new ArrayList<>();
        adapterBikes = new BikeAdapter(bikeList, distanceList);
        recyclerViewBikes.setAdapter(adapterBikes);

        //Recycler view for rides
        recyclerViewRides = findViewById(R.id.recycler_view_rides);
        recyclerViewRides.setLayoutManager(new LinearLayoutManager(this));
        rideList = new ArrayList<>();
        adapterRides = new ItemAdapter(rideList);
        recyclerViewRides.setAdapter(adapterRides);

        loadBikes();
        loadRides();


        //SwipeRefreshLayout settings
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadBikes();
            loadRides();
            swipeRefreshLayout.setRefreshing(false);
        });

        //Account button
        ImageButton accountButton = findViewById(R.id.account_button);
        accountButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountActivity.class);
            startActivity(intent);
        });

        ImageButton bikeAddingButton = findViewById(R.id.add_bikes_button);
        bikeAddingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, BikeAddingActivity.class);
            startActivity(intent);
        });

        //Manual ride adding button
        ImageButton manualRideAddingButton = findViewById(R.id.manual_ride_adding_button);
        manualRideAddingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualRideAddingActivity.class);
            startActivity(intent);
        });

        //Add parts button
        ImageButton addPartsButton = findViewById(R.id.add_parts_button);
        addPartsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PartsaddingActivity.class);
            startActivity(intent);
        });

        ImageButton addRidesButton = findViewById(R.id.add_rides_button);
        addRidesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualRideAddingActivity.class);
            startActivity(intent);
        });
    }

    private void loadBikes() {
        String userId = mAuth.getCurrentUser().getUid();  // Get current user ID
        mDatabase.child("users").child(userId).child("bicycles")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        bikeList.clear();
                        distanceList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String bikeName = snapshot.getValue(String.class);
                            calculateTotalDistanceForBike(bikeName);
                        }
                        adapterBikes.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("DashboardActivity", "Failde to load bikes: ", databaseError.toException());
                        Toast.makeText(DashboardActivity.this, "Error loading bikes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void calculateTotalDistanceForBike(String bikeName) {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("rides")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        double totalDistance = 0;
                        for (DataSnapshot rideSnapshot : dataSnapshot.getChildren()) {
                            String selectedBike = rideSnapshot.child("selectedBicycle").getValue(String.class);
                            if (bikeName.equals(selectedBike)) {
                                String distanceStr = rideSnapshot.child("distance").getValue(String.class);
                                totalDistance += Double.parseDouble(distanceStr);
                            }
                        }
                        String formattedDistance;
                        if (totalDistance % 1 == 0) {
                            formattedDistance = String.format("%.0f", totalDistance);
                        } else {
                            formattedDistance = String.format("%.1f", totalDistance);
                        }
                        bikeList.add(bikeName);
                        distanceList.add(formattedDistance);
                        adapterBikes.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("DashboardActivity", "Failed to calculate total distance: ", databaseError.toException());
                    }
                });
    }

    private void loadRides() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("rides")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        rideList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Ride ride = snapshot.getValue(Ride.class);
                            rideList.add(ride);
                        }
                        adapterRides.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("DashboardActivity", "Failed to load rides: ", databaseError.toException());
                        Toast.makeText(DashboardActivity.this, "Error loading rides", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static class Ride {
        public String selectedBicycle;
        public String rideTitle;
        public String date;
        public String time;
        public String distance;

        public Ride() {}

        public Ride(String selectedBicycle, String rideTitle, String date, String time, String distance) {
            this.selectedBicycle = selectedBicycle;
            this.rideTitle = rideTitle;
            this.date = date;
            this.time = time;
            this.distance = distance;
        }

        public String getSelectedBicycle() {
            return selectedBicycle;
        }

        public String getRideTitle() {
            return rideTitle;
        }

        public String getDate() {
            return date;
        }

        public String getTime() {
            return time;
        }

        public String getDistance() {
            return distance;
        }
    }
}