package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
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
import java.util.Arrays;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private RecyclerView recyclerViewRides, recyclerViewBikes;
    private ItemAdapter adapterRides;
    private BikeAdapter adapterBikes;
    private List<String> bikeList;
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
        adapterBikes = new BikeAdapter(bikeList);
        recyclerViewBikes.setAdapter(adapterBikes);

        //Recycler view for rides
        recyclerViewRides = findViewById(R.id.recycler_view_rides);
        recyclerViewRides.setLayoutManager(new LinearLayoutManager(this));
        rideList = new ArrayList<>();
        adapterRides = new ItemAdapter(rideList);
        recyclerViewRides.setAdapter(adapterRides);

        loadBikes();
        loadRides();

        //Recycler view for parts
//        RecyclerView recyclerViewParts = findViewById(R.id.recycler_view_parts);
//        recyclerViewParts.setLayoutManager(new LinearLayoutManager(this));
//        List<String> itemsparts = Arrays.asList("Part 1", "Part 2", "Part 3");
//        ItemAdapter adapterparts = new ItemAdapter(itemsparts);
//        recyclerViewParts.setAdapter(adapterparts);

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

        //Manual ride adding button
        ImageButton manualRideAddingButton = findViewById(R.id.manual_ride_adding_button);
        manualRideAddingButton.setOnClickListener(v -> {
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
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String bikeName = snapshot.getValue(String.class);
                            bikeList.add(bikeName);
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