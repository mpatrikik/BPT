package com.example.bpt;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ServiceAddingActivity extends AppCompatActivity {

    private EditText serviceNameEditText;
    private TextView datePickerText, timePickerText;
    private ImageButton submitServiceButton, backButton;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String userId, partName, partId, serviceIntervalId;

    private String selectedDate = "";
    private String selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serviceadding);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        partName = getIntent().getStringExtra("partName");

        DatabaseReference partsRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(userId).child("parts");
        partsRef.orderByChild("partName").equalTo(partName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot partSnapshot : dataSnapshot.getChildren()) {
                        partId = partSnapshot.getKey();
                        DataSnapshot mainServicesSnapshot = partSnapshot.child("MAINSERVICES");
                        for (DataSnapshot serviceIntervalSnapshot : mainServicesSnapshot.getChildren()) {
                            serviceIntervalId = serviceIntervalSnapshot.getKey();
                        }
                    }
                } else {
                    Toast.makeText(ServiceAddingActivity.this, "Part not found", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ServiceAddingActivity", "Error finding partId", databaseError.toException());
            }
        });


        serviceNameEditText = findViewById(R.id.service_name_edit_text);
        datePickerText = findViewById(R.id.date_picker);
        timePickerText = findViewById(R.id.time_picker);

        submitServiceButton = findViewById(R.id.submit_service_button);
        submitServiceButton.setEnabled(false);
        submitServiceButton.setAlpha(0.5f);

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ServiceAddingActivity.this, PartDetailsActivity.class);
            intent.putExtra("partName", partName);
            startActivity(intent);
        });

        submitServiceButton.setOnClickListener(v -> saveServiceDetails());


        datePickerText.setOnClickListener(v -> showDatePicker());
        timePickerText.setOnClickListener(v -> showTimePicker());

        serviceNameEditText.addTextChangedListener(inputWatcher);
        datePickerText.addTextChangedListener(inputWatcher);
        timePickerText.addTextChangedListener(inputWatcher);
    }


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



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

    // Check if all inputs are filled
    private boolean checkInputs() {
        String serviceName = serviceNameEditText.getText().toString().trim();
        String date = datePickerText.getText().toString().trim();
        String time = timePickerText.getText().toString().trim();

        boolean allFieldsFilled = !serviceName.isEmpty() && !date.equals("Today") && !time.equals("Now");

        submitServiceButton.setEnabled(allFieldsFilled);
        submitServiceButton.setAlpha(allFieldsFilled ? 1.0f : 0.5f);

        return allFieldsFilled;
    }

    public void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view1, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedYear + "/" + (selectedMonth + 1) + "/" + selectedDay;
                    datePickerText.setText(selectedDate);
                    checkInputs();
                }, year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    public void showTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    selectedTime = selectedHour + ":" + String.format("%02d", selectedMinute);
                    timePickerText.setText(selectedTime);
                    checkInputs();
                }, currentHour, currentMinute, true);
        timePickerDialog.show();
    }

    private void saveServiceDetails() {
        String serviceName = serviceNameEditText.getText().toString().trim();

        DatabaseReference serviceRef = mDatabase.child("users").child(userId)
                .child("parts").child(partId).child("MAINSERVICES").child(serviceIntervalId)
                .child("SERVICES").push();

        serviceRef.child("serviceName").setValue(serviceName);
        serviceRef.child("serviceDate").setValue(selectedDate);
        serviceRef.child("serviceTime").setValue(selectedTime).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ServiceAddingActivity.this, "Service added successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(ServiceAddingActivity.this, "Failed to add service", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
