package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditServiceIntervalActivity extends AppCompatActivity {

    private EditText serviceIntervalNameEditText, valueEditText;
    private ImageButton backButton, submitButton;
    private String userId, partId, serviceIntervalId, serviceIntervalName, serviceIntervalValueKm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editserviceinterval);

        serviceIntervalNameEditText = findViewById(R.id.serviceinterval_name_edit_text);
        valueEditText = findViewById(R.id.value_edit_text);
        backButton = findViewById(R.id.back_button);
        submitButton = findViewById(R.id.submit_edit_service_interval_button);

        submitButton.setEnabled(false);
        submitButton.setAlpha(0.5f);

        backButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Discard Changes")
                    .setMessage("Are you sure you want to discard your changes?")
                    .setPositiveButton("Yes", (dialog, which) -> { finish(); })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) .show();
        });

        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        partId = intent.getStringExtra("partId");
        serviceIntervalId = intent.getStringExtra("serviceIntervalId");
        serviceIntervalName = intent.getStringExtra("serviceIntervalName");
        serviceIntervalValueKm = intent.getStringExtra("serviceIntervalValueKm");

        if (serviceIntervalName != null) {
            serviceIntervalNameEditText.setText(serviceIntervalName);
        }
        if (serviceIntervalValueKm != null) {
            valueEditText.setText(serviceIntervalValueKm);
        }

        serviceIntervalNameEditText.addTextChangedListener(textWatcher);
        valueEditText.addTextChangedListener(textWatcher);

        submitButton.setOnClickListener(v -> saveChanges());
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            checkForChanges();
        }
    };

    private void checkForChanges() {
        String newServiceIntervalName = serviceIntervalNameEditText.getText().toString().trim();
        String newServiceIntervalValueKm = valueEditText.getText().toString().trim();
        boolean isNameChanged = !newServiceIntervalName.equals(serviceIntervalName);
        boolean isValueChanged = !newServiceIntervalValueKm.equals(serviceIntervalValueKm);
        submitButton.setEnabled(isNameChanged || isValueChanged);
        submitButton.setAlpha(isNameChanged || isValueChanged ? 1f : 0.5f);
    }


    private void saveChanges() {
        String newServiceIntervalName = serviceIntervalNameEditText.getText().toString().trim();
        String newServiceIntervalValueKm = valueEditText.getText().toString().trim();
        boolean isNameChanged = !newServiceIntervalName.equals(serviceIntervalName);
        boolean isValueChanged = !newServiceIntervalValueKm.equals(serviceIntervalValueKm);
        if (isNameChanged || isValueChanged) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(userId).child("parts").child(partId)
                    .child("MAINSERVICES").child(serviceIntervalId).child("serviceInterval");
            if (isNameChanged) {
                databaseReference.child("serviceIntervalName").setValue(newServiceIntervalName);
            }
            if (isValueChanged) {
                databaseReference.child("serviceIntervalValueKm").setValue(newServiceIntervalValueKm);
            }
            Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "No changes for saving.", Toast.LENGTH_SHORT).show();
        }
    }

}
