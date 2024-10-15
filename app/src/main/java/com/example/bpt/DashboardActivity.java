package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerViewRides, recyclerViewBikes, recyclerViewParts;
    private RideAdapter adapterRides;
    private BikeAdapter adapterBikes;
    private PartAdapter adapterParts;
    private List<String> bikeList;
    private List<String> distanceList;
    private List<Ride> rideList;
    private List<Part> partList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private final List<String> predefinedPartTypes = Arrays.asList("Chain", "Tire", "Wheelset", "Brake pads", "Brake disc", "Handlebar tape", "Casette", "Chainring", "Shifting cable", "Brake cable", "Bottom bracket");

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
        adapterBikes = new BikeAdapter(bikeList, distanceList, bikeName -> {
            Intent intent = new Intent(this, BikeDetailsActivity.class);
            intent.putExtra("bike_name", bikeName);
            startActivity(intent);
        });
        recyclerViewBikes.setAdapter(adapterBikes);

        // Recycler view for parts
        recyclerViewParts = findViewById(R.id.recycler_view_parts);
        recyclerViewParts.setLayoutManager(new LinearLayoutManager(this));
        partList = new ArrayList<>();
        adapterParts = new PartAdapter(partList);
        recyclerViewParts.setAdapter(adapterParts);

        //Recycler view for rides
        recyclerViewRides = findViewById(R.id.recycler_view_rides);
        recyclerViewRides.setLayoutManager(new LinearLayoutManager(this));
        rideList = new ArrayList<>();
        adapterRides = new RideAdapter(rideList);
        recyclerViewRides.setAdapter(adapterRides);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadBikes();
            loadParts();
            loadRides();
            swipeRefreshLayout.setRefreshing(false);
        });

        recyclerViewBikes.addOnScrollListener(scrollListener);
        recyclerViewParts.addOnScrollListener(scrollListener);
        recyclerViewRides.addOnScrollListener(scrollListener);

        loadBikes();
        loadParts();
        loadRides();
        checkAndCreatePartTypesNode();


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

    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            // Only enable SwipeRefreshLayout if all RecyclerViews are at the top
            swipeRefreshLayout.setEnabled(isRecyclerViewAtTop(recyclerViewBikes)
                    && isRecyclerViewAtTop(recyclerViewParts)
                    && isRecyclerViewAtTop(recyclerViewRides));
        }
    };

    private boolean isRecyclerViewAtTop(RecyclerView recyclerView) {
        return recyclerView != null && !recyclerView.canScrollVertically(-1);
    }

    private void checkAndCreatePartTypesNode() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference partTypesRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId).child("part_types");

        // Ellenőrizzük, hogy a "part_types" node létezik-e
        partTypesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (String partType : predefinedPartTypes) {
                    boolean exists = false;

                    // Ellenőrizzük, hogy létezik-e már az adott partType a node-ban
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String existingPartType = snapshot.getValue(String.class);
                        if (partType.equals(existingPartType)) {
                            exists = true;
                            break;
                        }
                    }

                    // Ha az adott partType még nem létezik, hozzáadjuk egy egyedi kulccsal
                    if (!exists) {
                        DatabaseReference newPartTypeRef = partTypesRef.push();  // Egyedi kulcs létrehozása
                        newPartTypeRef.setValue(partType)
                                .addOnSuccessListener(aVoid -> Log.d("DashboardActivity", "Part type added: " + partType))
                                .addOnFailureListener(e -> Log.e("DashboardActivity", "Failed to add part type: " + partType, e));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DashboardActivity", "Database error: " + databaseError.getMessage());
            }
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
                        Log.e("DashboardActivity", "Failed to load rides: ", databaseError.toException());
                        Toast.makeText(DashboardActivity.this, "Error loading rides", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadParts() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("parts")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        partList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String partName = snapshot.child("partName").getValue(String.class);
                            calculateTotalDistanceForPart(partName);
                        }
                        adapterParts.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("DashboardActivity", "Failed to load parts: ", databaseError.toException());
                        Toast.makeText(DashboardActivity.this, "Error loading parts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void calculateTotalDistanceForPart(String partName) {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("rides")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        double totalDistance = 0;
                        for (DataSnapshot rideSnapshot : dataSnapshot.getChildren()) {
                            List<String> partsUsed = (List<String>) rideSnapshot.child("selectedParts").getValue();
                            if (partsUsed != null && partsUsed.contains(partName)) {
                                String distanceStr = rideSnapshot.child("distance").getValue(String.class);
                                totalDistance += Double.parseDouble(distanceStr);
                            }
                        }
                        partList.add(new Part(partName, totalDistance));
                        adapterParts.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("DashboardActivity", "Failed to calculate total distance for part: ", databaseError.toException());
                    }
                });
    }


    public static class Ride {
        public String selectedBicycle;
        public String rideTitle;
        public String date;
        public String time;
        public String distance;
        public List<String> selectedParts;

        public Ride() {}

        public Ride(String selectedBicycle, String rideTitle, String date, String time, String distance, List<String> selectedParts) {
            this.selectedBicycle = selectedBicycle;
            this.rideTitle = rideTitle;
            this.date = date;
            this.time = time;
            this.distance = distance;
            this.selectedParts = selectedParts;
        }

        public String getSelectedBicycle() { return selectedBicycle; }
        public String getRideTitle() { return rideTitle; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getDistance() { return distance; }
        public List<String> selectedParts() { return selectedParts; }
    }
}