package com.example.bpt;

import android.os.Bundle;
import android.util.Log;
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

public class ServiceAddingActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String partName, userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serviceadding);

        TextView titleTextView = findViewById(R.id.title_text_view);
        titleTextView.setText("New service interval");

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Retrieve part name from intent
        partName = getIntent().getStringExtra("selected_part_name");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
                            DatabaseReference newservicesRef = servicesRef.push();
                            newservicesRef.child("serviecName").setValue("New service");
                            Toast.makeText(ServiceAddingActivity.this, "Services node created", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ServiceAddingActivity.this, "Services node already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast.makeText(ServiceAddingActivity.this, "Part not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ServiceAddingActivity", "Error checking services node", databaseError.toException());
            }
        });
    }
}

