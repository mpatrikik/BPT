package com.example.bpt;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    private Context context;
    private List<DataSnapshot> serviceList;
    private String partId, partName, userId;
    private final Map<String, Double> cachedDistances = new HashMap<>();
    private DatabaseReference mDatabase;

    public ServiceAdapter(Context context, List<DataSnapshot> serviceList, String partId, String partName) {
        this.context = context;
        this.serviceList = serviceList;
        this.partId = partId;
        this.partName = partName;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        this.userId = auth.getCurrentUser().getUid();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();

        sortServicesByDateAndTime();
    }

    private void sortServicesByDateAndTime() {
        Collections.sort(serviceList, new Comparator<DataSnapshot>() {
            @Override
            public int compare(DataSnapshot o1, DataSnapshot o2) {
                String date1 = o1.child("serviceDate").getValue(String.class);
                String time1 = o1.child("serviceTime").getValue(String.class);
                String date2 = o2.child("serviceDate").getValue(String.class);
                String time2 = o2.child("serviceTime").getValue(String.class);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
                try {
                    Date dateTime1 = sdf.parse(date1 + " " + time1);
                    Date dateTime2 = sdf.parse(date2 + " " + time2);
                    return dateTime2.compareTo(dateTime1);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_service, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataSnapshot serviceSnapshot = serviceList.get(position);

        String serviceId = serviceSnapshot.getKey();
        String serviceName = serviceSnapshot.child("serviceName").getValue(String.class);
        String serviceDate = serviceSnapshot.child("serviceDate").getValue(String.class);
        String serviceTime = serviceSnapshot.child("serviceTime").getValue(String.class);

        //Alap értékek az adatoknak
        holder.serviceNameTextView.setText(serviceName != null ? serviceName : "Unknown Service");
        String dateTimeDisplaying = (serviceDate != null ? serviceDate : "No date") +  (", at") + (serviceTime != null ? " " + serviceTime : "");
        holder.serviceDateTextView.setText(dateTimeDisplaying);


        if (cachedDistances.containsKey(serviceId)) {
            // Ha a távolság már ki van számolva, használjuk a cache-t
            holder.overallAddedKmTextView.setText(String.format("%.1f km", cachedDistances.get(serviceId)));
        } else {
            holder.overallAddedKmTextView.setText("Loading...");
            if (context instanceof PartDetailsActivity) {
                PartDetailsActivity activity = (PartDetailsActivity) context;
                activity.calculateTotalDistanceSinceDateTime(partId, serviceDate, serviceTime, totalDistance -> {
                    if (position == serviceList.size() - 1) {
                        cachedDistances.put(serviceId, totalDistance);
                    } else if (!cachedDistances.containsKey(serviceId)) {
                        cachedDistances.put(serviceId, totalDistance);
                    }
                    holder.overallAddedKmTextView.setText(String.format("%.1f km", cachedDistances.get(serviceId)));
                });
            }
        }

        // Handling popup menu
        holder.moreButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.moreButton);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu_service, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.edit_service) {
                    Toast.makeText(context, "Edit logic for service ID: " + serviceSnapshot.getKey(), Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.delete_service) {
                    // Törlés megerősítő párbeszédablak
                    showDeleteConfirmationDialog(serviceSnapshot, position);
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });




    }

    private void showDeleteConfirmationDialog(DataSnapshot serviceSnapshot, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Service")
                .setMessage("Are you sure you want to delete this service?")
                .setPositiveButton("Yes", (dialog, which) -> deleteService(serviceSnapshot, position))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteService(DataSnapshot serviceSnapshot, int position) {
        String mainServiceId = serviceSnapshot.getRef().getParent().getParent().getKey();
        String serviceId = serviceSnapshot.getKey();

        // Firebase adatbázis törlése
        mDatabase.child("users").child(userId).child("parts").child(partId)
                .child("MAINSERVICES").child(mainServiceId).child("SERVICES").child(serviceId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    // RecyclerView frissítése
                    serviceList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, serviceList.size());
                    Toast.makeText(context, "Service deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete service: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return serviceList != null ? serviceList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView serviceNameTextView, serviceDateTextView, overallAddedKmTextView;
        ImageButton moreButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceNameTextView = itemView.findViewById(R.id.service_name_textview);
            serviceDateTextView = itemView.findViewById(R.id.service_date_textview);
            overallAddedKmTextView = itemView.findViewById(R.id.overall_added_servicekm_textview);
            moreButton = itemView.findViewById(R.id.more_button);
        }
    }
}
