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
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        EditText usernameEditText = findViewById(R.id.username);
        EditText passwordEditText = findViewById(R.id.password);
        EditText passwordAgainEditText = findViewById(R.id.password_again);
        ImageButton registerButton = findViewById(R.id.register_button);
        TextView loginRedirect = findViewById(R.id.login_redirect);

        // Redirect to login
        String text = "Already have an account? Login here!";
        SpannableString spannableString = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        };

        int start = text.indexOf("Login here!");
        int end = start + "Login here!".length();
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        loginRedirect.setText(spannableString);
        loginRedirect.setMovementMethod(LinkMovementMethod.getInstance());

        // Regisztráció kezelése
        registerButton.setOnClickListener(v -> {
            Log.d(TAG, "Register button clicked");
            String username = usernameEditText.getText().toString().trim();
            String password = passwordAgainEditText.getText().toString().trim();
            String passwordAgain = passwordAgainEditText.getText().toString().trim();

            if (!password.equals(passwordAgain)) {
                showError("Passwords do not match");
            } else if (!isValidPassword(password)) {
                showError("Password must be at least 5 characters and contain at least one number");
            } else {
                registerUser(username, password);
            }
        });
    }

    private boolean isValidPassword(String password) {
        Pattern pattern = Pattern.compile("^(?=.*[0-9]).{5,}");
        return pattern.matcher(password).matches();
    }

    private void registerUser(String username, String password) {
        mAuth.createUserWithEmailAndPassword(username + "@example.com", password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sikeres regisztráció
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();

                            // Felhasználói adatok feltöltése az adatbázisba
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", username);
                            userData.put("password", password); // Nem javasolt jelszót plain text-ben tárolni!
                            userData.put("bicycles", new HashMap<>()); // Üres kerékpár lista
                            userData.put("rides", new HashMap<>()); // Üres edzés lista

                            mDatabase.child("users").child(userId).setValue(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "User registered with ID: " + userId);
                                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Error adding user", e);
                                        showError("Failed to register user: " + e.getMessage());
                                    });
                        }
                    } else {
                        // Sikertelen regisztráció
                        Log.w(TAG, "Registration failed", task.getException());
                        showError("Failed to register: " + task.getException().getMessage());
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
