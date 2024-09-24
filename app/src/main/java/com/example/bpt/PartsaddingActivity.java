package com.example.bpt;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class PartsaddingActivity extends AppCompatActivity {

    private EditText partNameEditText, partTypeEditText;
    private Button addPartButton;
    private Spinner bicycleSpinner;
    private ArrayList<String> bicycleList;
    private ArrayAdapter<String> adapter;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partsadding);

        // Firebase authentication and database initialization
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get the current logged-in user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user is logged in", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no user is logged in
            return;
        }
        userId = currentUser.getUid();

        // Initialize UI elements
        partNameEditText = findViewById(R.id.part_name_edittext);
        partTypeEditText = findViewById(R.id.part_type_edittext);
        addPartButton = findViewById(R.id.add_part_button);
        bicycleSpinner = findViewById(R.id.bicycle_spinner);
        bicycleList = new ArrayList<>();

        // Set up the adapter for the spinner
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bicycleList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bicycleSpinner.setAdapter(adapter);

        checkAndCreatePartsNode();

        // Load bicycles from Firebase
        loadBicycles();

        // Set up the "Add Part" button
        addPartButton.setOnClickListener(v -> addPartToDatabase());
    }

    private void checkAndCreatePartsNode() {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // If "parts" node doesn't exist, create it
                if (!dataSnapshot.hasChild("parts")) {
                    mDatabase.child("users").child(userId).child("parts").setValue(new ArrayList<>())
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(PartsaddingActivity.this, "Parts section created in database.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(PartsaddingActivity.this, "Failed to create parts section.", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PartsaddingActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBicycles() {
        // Get user's bicycles from Firebase
        mDatabase.child("users").child(userId).child("bicycles")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        bicycleList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String bicycleName = snapshot.getValue(String.class);
                            bicycleList.add(bicycleName);
                        }
                        adapter.notifyDataSetChanged(); // Update the spinner with the new data
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(PartsaddingActivity.this, "Failed to load bicycles", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addPartToDatabase() {
        // Get selected bicycle and part details from input fields
        String selectedBicycle = bicycleSpinner.getSelectedItem().toString();
        String partName = partNameEditText.getText().toString().trim();
        String partType = partTypeEditText.getText().toString().trim();

        if (partName.isEmpty() || partType.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a unique ID for the part and save it to Firebase
        String partId = mDatabase.child("users").child(userId).child("parts").push().getKey();

        Part part = new Part(selectedBicycle, partName, partType);

        mDatabase.child("users").child(userId).child("parts").child(partId).setValue(part)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(PartsaddingActivity.this, "Part added successfully", Toast.LENGTH_SHORT).show();
                        finish(); // Go back to the previous activity (e.g. Dashboard)
                    } else {
                        Toast.makeText(PartsaddingActivity.this, "Failed to add part", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Create a Part class to store part details
    public static class Part {
        public String bicycle;
        public String partName;
        public String partType;

        public Part() {
        }

        public Part(String bicycle, String partName, String partType) {
            this.bicycle = bicycle;
            this.partName = partName;
            this.partType = partType;
        }
    }
}
