package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
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
    private EditText serviceIntervalNameEditText, valueEditText;
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
        serviceIntervalNameEditText = findViewById(R.id.serviceinterval_name_edit_text);
        valueEditText = findViewById(R.id.value_edit_text);
        repeateSwitch = findViewById(R.id.repeat_switch);
        repeateSwitch.setChecked(true);
        submitButton = findViewById(R.id.submit_service_interval_button);
        submitButton.setEnabled(false);
        submitButton.setAlpha(0.5f);

        serviceIntervalNameEditText.addTextChangedListener(inputWatcher);
        valueEditText.addTextChangedListener(inputWatcher);

        repeateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            TextView repeatTextView = findViewById(R.id.repeat_text_view);
            TextView maxLifeTextView = findViewById(R.id.maxlife_text_view);
                    if (isChecked) {
                        serviceIntervalNameEditText.setText("");
                        serviceIntervalNameEditText.setHint("Name");
                        maxLifeTextView.setVisibility(View.GONE);
                        repeatTextView.setAlpha(1.0f);
                    } else {
                        serviceIntervalNameEditText.setText("Max life");
                        maxLifeTextView.setVisibility(View.VISIBLE);
                        repeatTextView.setAlpha(0.0f);
                    }
                    checkInputs();
                });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ServiceIntervalAddingActivity.this, PartDetailsActivity.class);
            intent.putExtra("part_name", partName);
            startActivity(intent);
        });

        submitButton.setOnClickListener(v -> { saveServiceInterval(); });

        checkAndCreateMAINSERVICESNode();
    }

    private final TextWatcher inputWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkInputs();
        }
        @Override
        public void afterTextChanged(Editable s) {}
    };

    private void checkInputs() {
        String serviceName = serviceIntervalNameEditText.getText().toString().trim();
        String value = valueEditText.getText().toString().trim();
        if (!serviceName.isEmpty() && !value.isEmpty()) {
            submitButton.setEnabled(true);
            submitButton.setAlpha(1.0f); // Gomb teljesen láthatóvá válik
        } else {
            submitButton.setEnabled(false);
            submitButton.setAlpha(0.5f); // Gomb halvány marad
        }
    }

    private void checkAndCreateMAINSERVICESNode() {
        mDatabase.child("users").child(userId).child("parts").orderByChild("partName").equalTo(partName)
        .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot partSnapshot : dataSnapshot.getChildren()) {
                        DatabaseReference servicesRef = partSnapshot.child("MAINSERVICES").getRef();
                        if (!partSnapshot.hasChild("MAINSERVICES")) {
                            servicesRef.setValue("");
                            Toast.makeText(ServiceIntervalAddingActivity.this, "SERVICES node created", Toast.LENGTH_SHORT).show();
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
        String serviceName = serviceIntervalNameEditText.getText().toString().trim();
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
                                DatabaseReference servicesRef = partSnapshot.child("MAINSERVICES").getRef();

                                // Új szerviz intervallum létrehozása
                                DatabaseReference newServiceIntervalRef = servicesRef.push();

                                DatabaseReference serviceIntervalRef = newServiceIntervalRef.child("serviceInterval");
                                serviceIntervalRef.child("isRepeat").setValue(isRepeat);
                                serviceIntervalRef.child("maxLife").setValue(isRepeat ? false : value);
                                serviceIntervalRef.child("serviceIntervalName").setValue(serviceName);
                                serviceIntervalRef.child("serviceIntervalValueKm").setValue(value);

                                DatabaseReference servicesNode = newServiceIntervalRef.child("SERVICES").push();
                                servicesNode.child("serviceName").setValue("");
                                servicesNode.child("serviceDate").setValue("");
                                servicesNode.child("serviceTime").setValue("");

                                Toast.makeText(ServiceIntervalAddingActivity.this, "Service interval saved with services node", Toast.LENGTH_SHORT).show();

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