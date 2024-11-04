package com.example.bpt;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ServiceIntervalsAdapter extends RecyclerView.Adapter<ServiceIntervalsAdapter.ViewHolder> {

    private Context context;
    private List<DataSnapshot> serviceIntervalsList;
    private String partId, userId;
    private DatabaseReference mDatabase;

    public ServiceIntervalsAdapter(Context context, List<DataSnapshot> serviceIntervalsList, String partId) {
        this.context = context;
        this.serviceIntervalsList = serviceIntervalsList;
        this.partId = partId;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            this.userId = currentUser.getUid();
        }
        this.mDatabase = FirebaseDatabase.getInstance().getReference(); // Adatbázis referencia definiálása
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_serviceintervals, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataSnapshot serviceIntervalSnapshot = serviceIntervalsList.get(position);
        String serviceIntervalName = serviceIntervalSnapshot.child("serviceInterval").child("serviceIntervalName").getValue(String.class);
        boolean isRepeat = serviceIntervalSnapshot.child("serviceInterval").child("isRepeat").getValue(Boolean.class);
        String serviceIntervalValueKmStr = serviceIntervalSnapshot.child("serviceInterval").child("serviceIntervalValueKm").getValue(String.class);

        holder.serviceIntervalNameTextView.setText(serviceIntervalName != null ? serviceIntervalName : "Unknown");

        if (serviceIntervalValueKmStr != null) {
            int serviceIntervalValueKm = Integer.parseInt(serviceIntervalValueKmStr);

            if (isRepeat) { // Ismétlődő intervallum
                mDatabase.child("users").child(userId).child("parts").child(partId)
                        .child("MAINSERVICES").child(serviceIntervalSnapshot.getKey()).child("SERVICES")
                        .orderByChild("serviceDate").limitToLast(1) // Legutolsó szerviz dátum lekérése
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot serviceSnapshot) {
                                String lastServiceDate = null;
                                String lastServiceTime = null;
                                for (DataSnapshot snapshot : serviceSnapshot.getChildren()) {
                                    lastServiceDate = snapshot.child("serviceDate").getValue(String.class);
                                    lastServiceTime = snapshot.child("serviceTime").getValue(String.class);
                                }
                                if (lastServiceDate != null && lastServiceTime != null) {
                                    // Távolság számítása az utolsó szerviz dátuma és ideje óta
                                    calculateDistanceSinceLastService(partId, lastServiceDate, lastServiceTime, serviceIntervalValueKm, holder);
                                } else {
                                    holder.remainingDistanceTextView.setText("No last service date");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("ServiceIntervalsAdapter", "Failed to get last service date: ", error.toException());
                            }
                        });
            } else { // Nem ismétlődő intervallum
                ((PartDetailsActivity) context).calculateTotalDistanceForPart(partId, totalDistance -> {
                    int remainingDistance = (int) (serviceIntervalValueKm - totalDistance);
                    if (remainingDistance < 0) remainingDistance = 0;
                    holder.remainingDistanceTextView.setText(remainingDistance + " km of " + serviceIntervalValueKm + " km left");
                });
            }
        } else {
            holder.remainingDistanceTextView.setText("No interval distance set");
        }

        //popum menu kezelése
        holder.moreButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.moreButton);

            if (isRepeat) {
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu_repeating_serviceintervals, popupMenu.getMenu());
            } else {
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu_non_repeating_serviceintervals, popupMenu.getMenu());
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.add_service) {
                    Intent intent = new Intent(context, ServiceAddingActivity.class);
                    intent.putExtra("partId", partId);
                    intent.putExtra("serviceIntervalId", serviceIntervalSnapshot.getKey());
                    context.startActivity(intent);
                    return true;
                } else if (itemId == R.id.edit_service_interval) {
                    String serviceIntervalId = serviceIntervalSnapshot.getKey(); // Meghatározzuk az aktuális intervallum ID-t
                    mDatabase.child("users").child(userId).child("parts").child(partId)
                            .child("MAINSERVICES").child(serviceIntervalId)
                            .child("serviceIntervalValueKm").get().addOnCompleteListener(task -> {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    String serviceIntervalValueKm = task.getResult().getValue(String.class);
                                    Intent intent = new Intent(context, EditServiceIntervalActivity.class);
                                    intent.putExtra("userId", userId);
                                    intent.putExtra("partId", partId);
                                    intent.putExtra("serviceIntervalId", serviceIntervalId);
                                    intent.putExtra("serviceIntervalName", serviceIntervalName);
                                    intent.putExtra("serviceIntervalValueKm", serviceIntervalValueKm); // Átadjuk a lekérdezett értéket
                                    context.startActivity(intent);
                                } else {
                                    Toast.makeText(context, "Nem sikerült lekérni az adatokat.", Toast.LENGTH_SHORT).show();
                                }
                            });
                    return true;
                } else if (itemId == R.id.delete_service_interval) {
                    showDeleteConfirmationDialog(serviceIntervalSnapshot.getKey(), position);
                    return true;
                } else {
                    return false;
                }
            });
            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() { return serviceIntervalsList.size(); }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView serviceIntervalNameTextView, remainingDistanceTextView, lastServiceDateTextView;
        public ImageButton moreButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceIntervalNameTextView = itemView.findViewById(R.id.service_interval_name_textview);
            remainingDistanceTextView = itemView.findViewById(R.id.service_interval_valuekm_textview);
            lastServiceDateTextView = itemView.findViewById(R.id.last_service_interval_date_textview);
            moreButton = itemView.findViewById(R.id.more_button);
        }
    }

    private void showDeleteConfirmationDialog(String intervalId, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Service Interval")
                .setMessage("Are you sure you want to delete this service interval?")
                .setPositiveButton("Yes", (dialog, which) -> deleteServiceInterval(intervalId, position))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void deleteServiceInterval(String intervalId, int position) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("users").child(userId).child("parts").child(partId).child("MAINSERVICES").child(intervalId);
        databaseReference.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(context, "Service interval deleted", Toast.LENGTH_SHORT).show();
                serviceIntervalsList.remove(position);
                notifyItemRemoved(position);
                Log.d("ServiceIntervalsAdapter", "Service interval deleted successfully");
            } else {
                Toast.makeText(context, "Failed to delete service interval", Toast.LENGTH_SHORT).show();
                Log.e("ServiceIntervalsAdapter", "Error deleting service interval", task.getException());
            }
        });
    }

    private void calculateDistanceSinceLastService(String partId, String lastServiceDate, String lastServiceTime, int serviceIntervalValueKm, ViewHolder holder) {
        Log.d("ServiceIntervalsAdapter", "calculateDistanceSinceLastService called");
        Log.d("ServiceIntervalsAdapter", "lastServiceDate: " + lastServiceDate + ", lastServiceTime: " + lastServiceTime + ", serviceIntervalValueKm: " + serviceIntervalValueKm);

        ((PartDetailsActivity) context).calculateTotalDistanceSinceDateTime(partId, lastServiceDate, lastServiceTime, totalDistanceSinceLastService -> {
            Log.d("ServiceIntervalsAdapter", "Total distance since last service: " + totalDistanceSinceLastService);

            int remainingDistance = (int) (serviceIntervalValueKm - totalDistanceSinceLastService);
            if (remainingDistance < 0) remainingDistance = 0;
            Log.d("ServiceIntervalsAdapter", "Remaining distance: " + remainingDistance);

            holder.remainingDistanceTextView.setText(remainingDistance + " km of " + serviceIntervalValueKm + " km left");
        });
    }


}
