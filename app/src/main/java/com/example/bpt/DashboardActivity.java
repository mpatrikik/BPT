package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private RecyclerView recyclerViewBikes;
    private ItemAdapter adapterbikes;
    private List<String> bikeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Recycler view for bikes
        recyclerViewBikes = findViewById(R.id.recycler_view_bikes);
        recyclerViewBikes.setLayoutManager(new LinearLayoutManager(this));

        bikeList = new ArrayList<>();
        adapterbikes = new ItemAdapter(bikeList);
        recyclerViewBikes.setAdapter(adapterbikes);

        loadBikes();

        //Recycler view for parts
        RecyclerView recyclerViewParts = findViewById(R.id.recycler_view_parts);
        recyclerViewParts.setLayoutManager(new LinearLayoutManager(this));

        List<String> itemsparts = Arrays.asList("Part 1", "Part 2", "Part 3");
        ItemAdapter adapterparts = new ItemAdapter(itemsparts);
        recyclerViewParts.setAdapter(adapterparts);



        //Recycler view for rides
        RecyclerView recyclerViewRides = findViewById(R.id.recycler_view_rides);
        recyclerViewRides.setLayoutManager(new LinearLayoutManager(this));

        List<String> itemsrides = Arrays.asList("Ride 1", "Ride 2", "Ride 3");
        ItemAdapter adapterrides = new ItemAdapter(itemsrides);
        recyclerViewRides.setAdapter(adapterrides);

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
                        adapterbikes.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("DashboardActivity", "Failde to load bikes: ", databaseError.toException());
                        Toast.makeText(DashboardActivity.this, "Error loading bikes", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
