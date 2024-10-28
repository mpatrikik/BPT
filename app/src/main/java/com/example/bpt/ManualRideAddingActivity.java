package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.TextView;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ManualRideAddingActivity extends AppCompatActivity {

    private ArrayList<String> bicycleList;
    private ArrayList<String> partList;
    private ArrayAdapter<String> adapter;
    private boolean[] selectedParts;
    private boolean isFirstSelection = true;
    private ArrayList<Integer> selectedPartsIndexes = new ArrayList<>();
    private String userId, selectedBike;
    private DatabaseReference mDatabase;
    private TextView selectedPartsTextView, datePickerTextView, timePickerTextView;;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manualrideadding);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        ImageButton homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
        });

        Spinner bicycleSpinner = findViewById(R.id.bicycle_spinner);
        bicycleList = new ArrayList<>();

        selectedPartsTextView = findViewById(R.id.select_parts);
        partList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bicycleList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bicycleSpinner.setAdapter(adapter);

        datePickerTextView = findViewById(R.id.date_picker);
        timePickerTextView = findViewById(R.id.time_picker);

        ImageButton submitButton = findViewById(R.id.manual_ride_adding_button_submit);
        submitButton.setOnClickListener(v -> saveRide());

        EditText distanceEditText = findViewById(R.id.ridden_distance_input);
        // Add TextWatcher to restrict distance input
        distanceEditText.addTextChangedListener(new TextWatcher() {
            private boolean isManualChange = false; // Jelzi, ha kézi módosítás történik
            private boolean isDecimalAdded = false; // Jelzi, ha már van tizedesvessző

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isManualChange) {
                    return; // Ha kézzel módosítunk, nem futtatjuk újra
                }

                String input = s.toString();

                // Ellenőrizzük, hogy van-e tizedesvessző
                if (input.contains(".")) {
                    if (!isDecimalAdded) {
                        if (input.indexOf(".") == 0) {
                            // Ha a tizedesvessző az első karakter, töröljük
                            isManualChange = true;
                            distanceEditText.setText(input.substring(1));
                            distanceEditText.setSelection(input.length() - 1);
                            isManualChange = false;
                            Toast.makeText(ManualRideAddingActivity.this, "A tizedesvessző előtt számot kell megadni!", Toast.LENGTH_SHORT).show();
                        } else {
                            isDecimalAdded = true; // Az első tizedesvesszőt elfogadjuk
                        }
                    }
                }

                // Ha már van tizedesvessző, további tizedesvessző beírását megakadályozzuk
                if (isDecimalAdded && input.indexOf(".", input.indexOf(".") + 1) != -1) {
                    isManualChange = true;
                    distanceEditText.setText(input.substring(0, input.length() - 1));
                    distanceEditText.setSelection(input.length() - 1);
                    isManualChange = false;
                    Toast.makeText(ManualRideAddingActivity.this, "Csak egy tizedesvessző lehet!", Toast.LENGTH_SHORT).show();
                }

                // Ha az input üres, alaphelyzetbe állítjuk a tizedest
                if (input.isEmpty()) { isDecimalAdded = false; }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        // Ellenőrizzük a bejelentkezett felhasználót
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user signed in. Redirecting to login...", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        } else {
            userId = currentUser.getUid();
            Log.d("ManualRideAdding", "User ID: " + userId);
            checkAndCreateUser(userId);
        }

        selectedBike = getIntent().getStringExtra("selected_bike_name");
        if (selectedBike != null) {
            bicycleList.clear();
            bicycleList.add(selectedBike);
            adapter.notifyDataSetChanged();
            bicycleSpinner.setSelection(0);
            bicycleSpinner.setEnabled(true);
            Log.d("ManualRideAddingActivity", "Selected bike: " + bicycleList.toString());
        } else {
            loadBicycles();
        }

        // Spinner esemény kezelése: új kerékpár hozzáadása ha a "+"-ra kattint
        bicycleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedBicycle = bicycleList.get(position);
                if ("Add new bicycle".equals(selectedBicycle)) {
                    showAddBicycleDialog();
                } else {
                    loadPartsForBicycle(selectedBicycle);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        selectedPartsTextView.setOnClickListener(v -> {
            if (!partList.isEmpty()) {
                showPartsSelectionDialog();
            }else {
                Toast.makeText(ManualRideAddingActivity.this, "No parts available for this bicycle.", Toast.LENGTH_SHORT).show();
            }
        });
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void loadPartsForBicycle(String selectedBicycle) {
        if (userId == null || selectedBicycle == null) {
            Toast.makeText(this, "Felhasználó ID vagy kerékpár hiányzik. Nem lehet betölteni az alkatrészeket.", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("users").child(userId).child("parts").orderByChild("bicycle").equalTo(selectedBicycle)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        partList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String partName = snapshot.child("partName").getValue(String.class);
                            partList.add(partName);
                        }

                        if (partList.isEmpty()) {
                            Toast.makeText(ManualRideAddingActivity.this, "No parts available for this bicycle..", Toast.LENGTH_SHORT).show();
                        } else {
                            // A többválasztásos lista beállítása
                            selectedParts = new boolean[partList.size()];
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("ManualRideAddingActivity", "Nem sikerült betölteni az alkatrészeket: ", databaseError.toException());
                        Toast.makeText(ManualRideAddingActivity.this, "Hiba történt az alkatrészek betöltésekor.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showPartsSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select parts");

        builder.setMultiChoiceItems(partList.toArray(new String[0]), selectedParts, (dialog, which, isChecked) -> {
            if (isChecked) {
                selectedPartsIndexes.add(which);
            } else {
                selectedPartsIndexes.remove(Integer.valueOf(which));
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            if (selectedPartsIndexes.isEmpty()) {
                selectedPartsTextView.setText("Select parts");
            } else {
                StringBuilder selectedPartsText = new StringBuilder();
                for (int i = 0; i < selectedPartsIndexes.size(); i++) {
                    selectedPartsText.append(partList.get(selectedPartsIndexes.get(i)));
                    if (i != selectedPartsIndexes.size() - 1) {
                        selectedPartsText.append(", ");
                    }
                }
                selectedPartsTextView.setText(selectedPartsText.toString());
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void checkAndCreateUser(String userId) {
        // Ellenőrizzük, hogy a felhasználó már létezik-e
        mDatabase.child("users").child(userId).get().addOnCompleteListener(task -> {
            if (!task.getResult().exists()) {
                // Létrehozzuk a felhasználói adatokat
                UserData newUser = new UserData(userId, new ArrayList<>());
                mDatabase.child("users").child(userId).setValue(newUser)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "User data created.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to create user data.", Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Ha létezik, betöltjük a kerékpárokat
                loadBicycles();
            }
        });
    }

    private void loadBicycles() {
        if (userId == null) {
            Toast.makeText(this, "User ID is null. Cannot load bicycles.", Toast.LENGTH_SHORT).show();
            return;
        }
        mDatabase.child("users").child(userId).child("bicycles").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                bicycleList.clear();
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String bicycleName = snapshot.getValue(String.class);
                    bicycleList.add(bicycleName);
                }
                // Hozzáadjuk a "+" opciót a végére
                bicycleList.add("Add new bicycle");
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to load bicycles.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addNewBicycleToDatabase(String bicycleName) {
        if (userId == null) {
            Toast.makeText(this, "User ID is null. Cannot add bicycle.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference bicycleRef = mDatabase.child("users").child(userId).child("bicycles").push();
        bicycleRef.setValue(bicycleName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ManualRideAddingActivity.this, "Bicycle added successfully!", Toast.LENGTH_SHORT).show();
                    bicycleList.add(bicycleName);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(ManualRideAddingActivity.this, "Failed to add bicycle.", Toast.LENGTH_SHORT).show());
    }

    private void showAddBicycleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Bicycle");

        // Input field hozzáadása
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Gombok hozzáadása
        builder.setPositiveButton("Add", (dialog, which) -> {
            String bicycleName = input.getText().toString().trim();
            if (!bicycleName.isEmpty()) {
                addNewBicycleToDatabase(bicycleName);
            } else {
                Toast.makeText(this, "Bicycle name cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    public class UserData {
        private String userId;
        private ArrayList<String> bicycles;

        public UserData(String userId, ArrayList<String> bicycles) {
            this.userId = userId;
            this.bicycles = bicycles;
        }

        public String getUserId() {
            return userId;
        }

        public ArrayList<String> getBicycles() {
            return bicycles;
        }

        public void setBicycles(ArrayList<String> bicycles) {
            this.bicycles = bicycles;
        }
    }

    public void showDatePicker(View view) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view1, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedYear + "/" + (selectedMonth + 1) + "/" + selectedDay;
                    datePickerTextView.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    public void showTimePicker(View view) {
        final Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        String selectedDate = datePickerTextView.getText().toString();

        if (selectedDate.equals(new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(calendar.getTime()))) {
            // Aktuális nap, csak a múltbeli vagy aktuális időpontokat engedjük
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view1, selectedHour, selectedMinute) -> {
                        String selectedTime = selectedHour + ":" + String.format("%02d", selectedMinute);
                        timePickerTextView.setText(selectedTime);
                    }, currentHour, currentMinute, true);

            timePickerDialog.show();
        } else {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view12, selectedHour, selectedMinute) -> {
                        String selectedTime = selectedHour + ":" + String.format("%02d", selectedMinute);
                        timePickerTextView.setText(selectedTime);
                    }, 0, 0, true);
            timePickerDialog.show();
        }
    }


    private void saveRide() {
        String selectedBicycle = ((Spinner) findViewById(R.id.bicycle_spinner)).getSelectedItem().toString();
        String rideTitle = ((EditText) findViewById(R.id.title_of_ride)).getText().toString();
        String date = ((TextView) findViewById(R.id.date_picker)).getText().toString();
        String time = ((TextView) findViewById(R.id.time_picker)).getText().toString();
        String distance = ((EditText) findViewById(R.id.ridden_distance_input)).getText().toString();

        // Ellenőrizzük, hogy minden kötelező adat meg van-e adva
        if (selectedBicycle.isEmpty() || selectedBicycle.equals("Add new bicycle")) {
            Toast.makeText(this, "Please select a bicycle", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rideTitle.isEmpty()) {
            Toast.makeText(this, "Please enter a ride title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.isEmpty() || date.equals("Today")) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (time.isEmpty() || time.equals("Now")) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (distance.isEmpty()) {
            Toast.makeText(this, "Please enter the ridden distance", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Double.parseDouble(distance);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid distance", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        try {
            String rideDateTime = date + " " + time;
            long rideTimeInMillis = sdf.parse(rideDateTime).getTime();
            long currentTimeInMillis = System.currentTimeMillis();

            if (rideTimeInMillis > currentTimeInMillis) {
                Toast.makeText(this, "Enter valid date and time", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(this, "Invalid date or time format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ride ID létrehozása
        String userId = mAuth.getCurrentUser().getUid();
        String rideId = mDatabase.child("users").child(userId).child("rides").push().getKey();

        if (selectedPartsIndexes.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No parts selected")
                    .setMessage("Are you sure you want to save the ride without selecting any parts?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        saveRideToDatabase(selectedBicycle, rideTitle, date, time, distance, new ArrayList<>());
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            ArrayList<String> selectedPartsList = new ArrayList<>();
            for (int index : selectedPartsIndexes) {
                selectedPartsList.add(partList.get(index));
            }
            saveRideToDatabase(selectedBicycle, rideTitle, date, time, distance, selectedPartsList);
        }
    }

    private void saveRideToDatabase(String selectedBicycle, String rideTitle, String date, String time, String distance, ArrayList<String> selectedPartsList) {
        String userId = mAuth.getCurrentUser().getUid();
        String rideId = mDatabase.child("users").child(userId).child("rides").push().getKey();

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild("bicycles")) {
                    mDatabase.child("users").child(userId).child("bicycles").setValue(new ArrayList<>());
                }
                if (!dataSnapshot.hasChild("rides")) {
                    mDatabase.child("users").child(userId).child("rides").setValue(new ArrayList<>());
                }

                // Ride adatok mentése, beleértve a kiválasztott alkatrészeket is
                Ride ride = new Ride(selectedBicycle, rideTitle, date, time, distance, selectedPartsList);
                mDatabase.child("users").child(userId).child("rides").child(rideId).setValue(ride)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(ManualRideAddingActivity.this, "Ride saved successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ManualRideAddingActivity.this, DashboardActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(ManualRideAddingActivity.this, "Failed to save ride", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ManualRideAddingActivity", "Failed to load bikes: ", databaseError.toException());
                Toast.makeText(ManualRideAddingActivity.this, "Error loading bikes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    class Ride {
        public String selectedBicycle;
        public String rideTitle;
        public String date;
        public String time;
        public String distance;
        public ArrayList<String> selectedParts;

        public Ride(){}

        public Ride(String selectedBicycle, String rideTitle, String date, String time, String distance, ArrayList<String> selectedParts){
            this.selectedBicycle = selectedBicycle;
            this.rideTitle = rideTitle;
            this.date = date;
            this.time = time;
            this.distance = distance;
            this.selectedParts = selectedParts;
        }
    }
}