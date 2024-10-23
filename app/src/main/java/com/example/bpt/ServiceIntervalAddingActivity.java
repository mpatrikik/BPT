package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
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

public class ServiceIntervalAddingActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String partName, userId;
    private EditText serviceNameEditText, valueEditText;
    private Switch repeateSwitch;
    private ImageButton submitButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serviceintervaladding);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        partName = getIntent().getStringExtra("selected_part_name");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton = findViewById(R.id.back_button);
        serviceNameEditText = findViewById(R.id.serviceinterval_name_edit_text);

        valueEditText = findViewById(R.id.value_edit_text);


        repeateSwitch = findViewById(R.id.repeat_switch);
        submitButton = findViewById(R.id.submit_service_interval_button);

        repeateSwitch.setChecked(true);

        backButton.setOnClickListener(v -> {
            // Visszalép a PartDetailsActivity-be
            Intent intent = new Intent(ServiceIntervalAddingActivity.this, PartDetailsActivity.class);
            intent.putExtra("part_name", partName);
            startActivity(intent);
        });

        submitButton.setOnClickListener(v -> { saveServiceInterval(); });

        checkAndCreateServicesNode();
    }

    private void checkAndCreateServicesNode() {
        mDatabase.child("users").child(userId).child("parts").orderByChild("partName").equalTo(partName)
        .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot partSnapshot : dataSnapshot.getChildren()) {
                        DatabaseReference servicesRef = partSnapshot.child("services").getRef();
                        if (!partSnapshot.hasChild("services")) {
                            servicesRef.setValue("");
                            Toast.makeText(ServiceIntervalAddingActivity.this, "Services node created", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(ServiceIntervalAddingActivity.this, "Part not found", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ServiceAddingActivity", "Error checking services node", databaseError.toException());
            }
        });
    }

    private void saveServiceInterval() {
        String serviceName = serviceNameEditText.getText().toString().trim();
        String value = valueEditText.getText().toString().trim();
        boolean isRepeat = repeateSwitch.isChecked();

        if (serviceName.isEmpty() || value.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("users").child(userId).child("parts").orderByChild("partName").equalTo(partName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot partSnapshot : dataSnapshot.getChildren()) {
                                DatabaseReference servicesRef = partSnapshot.child("services").getRef();

                                // Új szerviz node létrehozása
                                DatabaseReference newServiceRef = servicesRef.push();
                                newServiceRef.child("serviceName").setValue(serviceName);
                                newServiceRef.child("serviceValueKm").setValue(value);

                                // Repeat vagy maxLife beállítása a kapcsoló alapján
                                if (isRepeat) {
                                    newServiceRef.child("serviceInterval").child("repeat").setValue("repeat");
                                } else {
                                    newServiceRef.child("serviceInterval").child("repeat").setValue("maxLife: " + value);
                                }

                                Toast.makeText(ServiceIntervalAddingActivity.this, "Service interval saved", Toast.LENGTH_SHORT).show();

                                // Visszalépünk a PartDetailsActivity-be
                                Intent intent = new Intent(ServiceIntervalAddingActivity.this, PartDetailsActivity.class);
                                intent.putExtra("part_name", partName);
                                startActivity(intent);
                            }
                        } else {
                            Toast.makeText(ServiceIntervalAddingActivity.this, "Part not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("ServiceAddingActivity", "Error saving service interval", databaseError.toException());

                    }
                });
    }
}