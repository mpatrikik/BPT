package com.example.bpt;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
    private boolean hasNotified20Percent = false;
    private boolean hasNotifiedZero = false;

    public ServiceIntervalsAdapter(Context context, List<DataSnapshot> serviceIntervalsList, String partId) {
        this.context = context;
        this.serviceIntervalsList = serviceIntervalsList;
        this.partId = partId;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            this.userId = currentUser.getUid();
        }
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "SERVICE_NOTIFICATION_CHANNEL",
                    "Service Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for Service Intervals");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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

        // Beállítjuk a lastServiceDateTextView láthatóságát az ismétlődés alapján
        holder.lastServiceDateTextView.setVisibility(isRepeat ? View.VISIBLE : View.GONE);

        if (serviceIntervalValueKmStr != null) {
            int serviceIntervalValueKm = Integer.parseInt(serviceIntervalValueKmStr);

            if (isRepeat) { // Ismétlődő intervallum
                mDatabase.child("users").child(userId).child("parts").child(partId)
                        .child("MAINSERVICES").child(serviceIntervalSnapshot.getKey()).child("SERVICES")
                        .orderByChild("serviceDate").limitToLast(1)
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
                                    // Dátum megjelenítése és távolság számítása
                                    holder.lastServiceDateTextView.setText("Last serviced on " + lastServiceDate + ", at " + lastServiceTime);
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

        // Menükezelés
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
                    editServiceInterval(serviceIntervalSnapshot, serviceIntervalName);
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

    // EditServiceInterval funkció elkülönítése az olvashatóság érdekében
    private void editServiceInterval(DataSnapshot serviceIntervalSnapshot, String serviceIntervalName) {
        String serviceIntervalId = serviceIntervalSnapshot.getKey();
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
                        intent.putExtra("serviceIntervalValueKm", serviceIntervalValueKm);
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "Nem sikerült lekérni az adatokat.", Toast.LENGTH_SHORT).show();
                    }
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
            } else {
                Toast.makeText(context, "Failed to delete service interval", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateDistanceSinceLastService(String partId, String lastServiceDate, String lastServiceTime, int serviceIntervalValueKm, ViewHolder holder) {
        ((PartDetailsActivity) context).calculateTotalDistanceSinceDateTime(partId, lastServiceDate, lastServiceTime, totalDistanceSinceLastService -> {
            int remainingDistance = (int) (serviceIntervalValueKm - totalDistanceSinceLastService);
            if (remainingDistance < 0) remainingDistance = 0;
            holder.remainingDistanceTextView.setText(remainingDistance + " km of " + serviceIntervalValueKm + " km left");

            if (remainingDistance <= serviceIntervalValueKm * 0.2 && !hasNotified20Percent) {
                sendNotification("Service interval approaching limit", "The service interval for part " + partId + " is close to reaching its limit.");
                hasNotified20Percent = true;
            }

            if (remainingDistance == 0 && !hasNotifiedZero) {
                sendNotification("Service interval reached", "The service interval for part " + partId + " has been reached.");
                hasNotifiedZero = true;
            }
        });
    }

    private void sendNotification(String title, String message) {
        // Ellenőrizzük, hogy Android 13 vagy újabb van-e, és van-e engedély
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Engedély kérésére van szükség
                Toast.makeText(context, "Notification permission required", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "SERVICE_NOTIFICATION_CHANNEL")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Ideiglenes ikon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }


}
