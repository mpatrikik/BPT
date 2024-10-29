package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EditServiceIntervalActivity extends AppCompatActivity {

    private EditText serviceIntervalNameEditText, valueEditText;
    private Switch repeatSwitch;
    private ImageButton backButton, submitServiceIntervalButton;
    private String partId, serviceIntervalId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editserviceinterval);

        serviceIntervalNameEditText = findViewById(R.id.serviceinterval_name_edit_text);
        valueEditText = findViewById(R.id.value_edit_text);
        repeatSwitch = findViewById(R.id.repeat_switch);
        backButton = findViewById(R.id.back_button);
        submitServiceIntervalButton = findViewById(R.id.submit_service_interval_button);

        Intent intent = getIntent();
        partId = intent.getStringExtra("partId");
        serviceIntervalId = intent.getStringExtra("serviceIntervalId");

    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


}
