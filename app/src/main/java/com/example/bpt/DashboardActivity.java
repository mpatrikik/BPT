package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Recycler view for bikes
        RecyclerView recyclerViewBikes = findViewById(R.id.recycler_view_bikes);
        recyclerViewBikes.setLayoutManager(new LinearLayoutManager(this));

        List<String> itemsbikes = Arrays.asList("Bike 1", "Bike 2", "Bike 3");
        ItemAdapter adapterbikes = new ItemAdapter(itemsbikes);
        recyclerViewBikes.setAdapter(adapterbikes);


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


        ImageButton accountButton = findViewById(R.id.account_button);

        accountButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountActivity.class);
            startActivity(intent);
        });

        ImageButton manualRideAddingButton = findViewById(R.id.manual_ride_adding_button);

        manualRideAddingButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualRideAddingActivity.class);
            startActivity(intent);
        });
    }
}
