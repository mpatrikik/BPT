package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.bpt.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        EditText loginusernameEditText = findViewById(R.id.login_username);
        EditText loginpasswordEditText = findViewById(R.id.login_password);
        ImageButton loginButton = findViewById(R.id.login_button);
        TextView registerRedirect = findViewById(R.id.register_redirect);

        // Redirect to register
        String text = "Don't have a profile yet? Register here!";
        SpannableString spannableString = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        };

        int start = text.indexOf("Register here!");
        int end = start + "Register here!".length();
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        registerRedirect.setText(spannableString);
        registerRedirect.setMovementMethod(LinkMovementMethod.getInstance());

        // Login process
        loginButton.setOnClickListener(v -> {
            String username = loginusernameEditText.getText().toString().trim();
            String password = loginpasswordEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showError("Please enter both username and password");
            } else {
                loginUser(username, password);
            }
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            loadUserData(user.getUid());
                        } else {
                            showError("Authentication failed");
                        }
                    } else{
                            showError("Authentication failed");
                        }
                });
    }

    private void loadUserData(String userId) {
        mDatabase.child("users").child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // Felhasználói adatokat sikeresen betöltöttük
                DataSnapshot snapshot = task.getResult();
                String username = snapshot.child("username").getValue(String.class);

                // Mentjük a felhasználói adatokat
                SharedPreferences preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("isLoggedIn", true);
                editor.putString("username", username); // A felhasználónév mentése
                editor.apply();

                // Továbbirányítás a DashboardActivity-re
                Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            } else {
                showError("Failed to load user data.");
            }
        });
    }

    private void showError(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle("Error")
                .setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
