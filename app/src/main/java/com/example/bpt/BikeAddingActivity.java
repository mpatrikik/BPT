package com.example.bpt;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class BikeAddingActivity extends AppCompatActivity {

    private EditText bikeNameEditText;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;
    private ImageButton bikeAddingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bikeadding);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = mAuth.getCurrentUser().getUid();

        bikeNameEditText = findViewById(R.id.bike_name_edit_text);

        bikeAddingButton = findViewById(R.id.bike_adding_button);
        bikeAddingButton.setEnabled(false);
        bikeAddingButton.setAlpha(0.5f);

        bikeNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (!charSequence.toString().trim().isEmpty()) {
                    bikeAddingButton.setEnabled(true);
                    bikeAddingButton.setAlpha(1.0f);
                } else {
                    bikeAddingButton.setEnabled(false);
                    bikeAddingButton.setAlpha(0.5f);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });
        bikeAddingButton.setOnClickListener(v -> addBikeToDatabase());

        ImageButton homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
        });
    }

    private void addBikeToDatabase() {
        String bikeName = bikeNameEditText.getText().toString().trim();

        if (bikeName.isEmpty()) {
            Toast.makeText(BikeAddingActivity.this, "Please enter a bike name", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference bikeRef = mDatabase.child("users").child(userId).child("bicycles").push();
        bikeRef.setValue(bikeName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(BikeAddingActivity.this, "Bike added successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // visszatérünk az előző Activity-re
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BikeAddingActivity.this, "Failed to add bike", Toast.LENGTH_SHORT).show();
                });
    }
}
