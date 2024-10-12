package com.example.bpt;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class BikeDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bikedetails);

        // Az Intent-ből kiolvassuk az adatokat
        String bikeName = getIntent().getStringExtra("bike_name");
        String totalDistance = getIntent().getStringExtra("total_distance");

        // A layout elemek beállítása
        TextView bikeNameTextView = findViewById(R.id.bike_name);
        TextView totalDistanceTextView = findViewById(R.id.total_distance);

        bikeNameTextView.setText(bikeName);
        totalDistanceTextView.setText("Total distance: " + totalDistance + " km");
    }
}
