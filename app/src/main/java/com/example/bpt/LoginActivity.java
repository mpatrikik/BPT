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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private void loginUser(String username, String password) {
        String email = username + "@example.com";
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Ha sikeres a bejelentkezés, ellenőrizzük, hogy van-e adat az adatbázisban
                            checkUserInDatabase(user.getUid(), username, password);
                        } else {
                            showError("Authentication failed.");
                        }
                    } else {
                        showError("Authentication failed.");
                    }
                });
    }

    private void checkUserInDatabase(String userId, String username, String password) {
        mDatabase.child("users").child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (!snapshot.exists()) {
                    // Ha nincs adat, akkor létrehozzuk az alapértelmezett adatokat
                    createNewUserInDatabase(userId, username, password);
                }
                // Az adatbázisban már létezik adat vagy most hoztuk létre, továbbítjuk a DashboardActivity-re
                saveUserAndRedirect(username);
            } else {
                showError("Failed to check user data.");
            }
        });
    }


    private void createNewUserInDatabase(String userId, String username, String password) {
        // Létrehozunk egy Map-et a felhasználói adatok tárolására
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);  // A felhasználó neve
        userData.put("password", password);  // A jelszóű
        userData.put("bicycles", new ArrayList<>());  // Üres kerékpár lista
        userData.put("rides", new ArrayList<>());  // Üres edzés lista

        // Feltöltjük az adatokat az adatbázisba
        mDatabase.child("users").child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showError("Failed to create user data.");
                    }
                });
    }


    private void saveUserAndRedirect(String username) {
        // Mentjük a felhasználói adatokat SharedPreferences-be
        SharedPreferences preferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("username", username); // A felhasználónév mentése
        editor.apply();

        // Továbbirányítás a DashboardActivity-re
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
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
