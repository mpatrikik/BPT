package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

    private Spinner bicycleSpinner, partTypeSpinner;
    private ArrayList<String> bicycleList, partTypeList;
    private ArrayAdapter<String> bicycleAdapter, partTypeAdapter;
    private ImageButton addPartButton;
    private EditText partNameEditText;
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
        bicycleSpinner = findViewById(R.id.bicycle_spinner);
        partTypeSpinner = findViewById(R.id.part_type_spinner);
        addPartButton = findViewById(R.id.add_part_button);
        partNameEditText = findViewById(R.id.part_name_edittext);  // Using the existing part_name_edittext
        bicycleList = new ArrayList<>();
        partTypeList = new ArrayList<>();

        loadBicycles();
        checkAndCreatePartTypesNode();
        checkAndCreatePartsNode();

        // Set up the adapter for the bicycle spinner
        bicycleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bicycleList);
        bicycleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bicycleSpinner.setAdapter(bicycleAdapter);

        // Set up the adapter for the part type spinner
        partTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, partTypeList);
        partTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        partTypeSpinner.setAdapter(partTypeAdapter);

        // Gomb alapértelmezés szerint letiltva és halványítva
        addPartButton.setEnabled(false);
        addPartButton.setAlpha(0.5f);

        // TextWatcher az alkatrész névhez (partNameEditText)
        partNameEditText.addTextChangedListener(inputWatcher);

        // Spinner események figyelése
        bicycleSpinner.setOnItemSelectedListener(spinnerWatcher);
        partTypeSpinner.setOnItemSelectedListener(spinnerWatcher);

        bicycleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedBicycle = bicycleList.get(position);
                if ("Add new bicycle".equals(selectedBicycle)) {
                    showAddBicycleDialog();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        partTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedPartType = partTypeList.get(position);
                if ("Add new type".equals(selectedPartType)) {
                    showAddPartTypeDialog();  // Hívja meg az új part type dialogust
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Handle the homebutton click event
        ImageButton homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
        });

        // Set up the "Add Part" button
        addPartButton.setOnClickListener(v -> addPartToDatabase());
    }

    // TextWatcher az EditText figyelésére
    private final TextWatcher inputWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkFormCompletion();
        }

        @Override
        public void afterTextChanged(Editable s) { }
    };

    // AdapterView.OnItemSelectedListener a Spinnerek figyelésére
    private final AdapterView.OnItemSelectedListener spinnerWatcher = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            checkFormCompletion();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    private void checkFormCompletion() {
        String selectedBicycle = bicycleSpinner.getSelectedItem().toString();
        String selectedPartType = partTypeSpinner.getSelectedItem().toString();
        String partName = partNameEditText.getText().toString().trim();

        // Ellenőrizzük, hogy minden mező ki van-e töltve
        if (!partName.isEmpty() && !selectedBicycle.equals("Add new bicycle") && !selectedPartType.equals("Add new type")) {
            addPartButton.setEnabled(true);
            addPartButton.setAlpha(1.0f); // Visszaállítjuk az átlátszatlanságot
        } else {
            addPartButton.setEnabled(false);
            addPartButton.setAlpha(0.5f); // Halványítjuk, ha nincs kitöltve
        }
    }

    private void showAddPartTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Part Type");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String partTypeName = input.getText().toString().trim();
            if (!partTypeName.isEmpty()) {
                checkIfPartTypeExistsAndAdd(partTypeName);
            } else {
                Toast.makeText(this, "Part type name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void checkIfPartTypeExistsAndAdd(String partTypeName) {
        // Lekérdezzük az adatbázisból az összes part_type-ot
        mDatabase.child("users").child(userId).child("part_types")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean exists = false;
                        // Ellenőrizzük, hogy a partTypeName szerepel-e már az adatbázisban
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String existingTypeName = snapshot.getValue(String.class);
                            if (partTypeName.equalsIgnoreCase(existingTypeName)) {
                                exists = true;
                                break;
                            }
                        }
                        if (exists) {
                            // Ha létezik, értesítjük a felhasználót
                            Toast.makeText(PartsaddingActivity.this, "Part type already exists!", Toast.LENGTH_SHORT).show();
                        } else {
                            // Ha nem létezik, hozzáadjuk az adatbázishoz
                            addNewPartTypeToDatabase(partTypeName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(PartsaddingActivity.this, "Error checking part type", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addNewPartTypeToDatabase(String partTypeName) {
        DatabaseReference partTypeRef = mDatabase.child("users").child(userId).child("part_types").push();
        partTypeRef.setValue(partTypeName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(PartsaddingActivity.this, "Part type added successfully", Toast.LENGTH_SHORT).show();
                    partTypeList.add(partTypeList.size() - 1, partTypeName);  // Az új típust a listába is hozzáadjuk
                    partTypeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(PartsaddingActivity.this, "Failed to add part type", Toast.LENGTH_SHORT).show());
    }

    private void checkAndCreatePartTypesNode() {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild("part_types")) {
                    mDatabase.child("users").child(userId).child("part_types").setValue(new ArrayList<>())
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(PartsaddingActivity.this, "Part types section created.", Toast.LENGTH_SHORT).show();
                                    loadPartTypes();
                                } else {
                                    Toast.makeText(PartsaddingActivity.this, "Failed to create part types section.", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    loadPartTypes();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PartsaddingActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndCreatePartsNode() {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild("parts")) {
                    mDatabase.child("users").child(userId).child("parts").setValue(new ArrayList<>())
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(PartsaddingActivity.this, "Parts section created in database.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(PartsaddingActivity.this, "Failed to create parts section.", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else { }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PartsaddingActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPartTypes() {
        mDatabase.child("users").child(userId).child("part_types").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                partTypeList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String partTypeName = snapshot.getValue(String.class);
                    partTypeList.add(partTypeName);
                }
                partTypeList.add("Add new type");
                partTypeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PartsaddingActivity.this, "Failed to load part types", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addPartToDatabase() {
        String selectedBicycle = bicycleSpinner.getSelectedItem().toString();
        String partName = partNameEditText.getText().toString().trim();
        String partType = partTypeSpinner.getSelectedItem().toString();

        if (partName.isEmpty() || selectedBicycle.equals("Add new bicycle") || partType.equals("Add new type")) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String partId = mDatabase.child("users").child(userId).child("parts").push().getKey();

        Part part = new Part(selectedBicycle, partName, partType);

        mDatabase.child("users").child(userId).child("parts").child(partId).setValue(part)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(PartsaddingActivity.this, "Part added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(PartsaddingActivity.this, "Failed to add part", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadBicycles() {
        mDatabase.child("users").child(userId).child("bicycles")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        bicycleList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String bicycleName = snapshot.getValue(String.class);
                            bicycleList.add(bicycleName);
                        }
                        bicycleList.add("Add new bicycle");
                        bicycleAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(PartsaddingActivity.this, "Failed to load bicycles", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddBicycleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Bicycle");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String bicycleName = input.getText().toString().trim();
            if (!bicycleName.isEmpty()) {
                addNewBicycleToDatabase(bicycleName);
            } else {
                Toast.makeText(this, "Bicycle name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addNewBicycleToDatabase(String bicycleName) {
        DatabaseReference bicycleRef = mDatabase.child("users").child(userId).child("bicycles").push();
        bicycleRef.setValue(bicycleName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(PartsaddingActivity.this, "Bicycle added successfully", Toast.LENGTH_SHORT).show();
                    bicycleList.add(bicycleName);
                    bicycleAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(PartsaddingActivity.this, "Failed to add bicycle", Toast.LENGTH_SHORT).show());
    }

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
