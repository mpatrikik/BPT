package com.example.bpt;

import android.content.Context;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ServiceIntervalsAdapter extends RecyclerView.Adapter<ServiceIntervalsAdapter.ViewHolder> {

    private Context context;
    private List<DataSnapshot> serviceIntervalsList;
    private String partId;
    private String userId;


    public ServiceIntervalsAdapter(Context context, List<DataSnapshot> serviceIntervalsList, String partId) {
        this.context = context;
        this.serviceIntervalsList = serviceIntervalsList;
        this.partId = partId;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            this.userId = currentUser.getUid();
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
        String remainingDistance = "000km of 000km left";
        String lastServiceDate = "Last serviced on 0000.00.00., at 00:00 after 000km";

        holder.serviceIntervalNameTextView.setText(serviceIntervalName != null ? serviceIntervalName : "Unknown");
        holder.remainingDistanceTextView.setText(remainingDistance);
        holder.lastServiceDateTextView.setText(lastServiceDate);

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
                    addService(serviceIntervalSnapshot.getKey());
                    return true;
                } else if (itemId == R.id.edit_service_interval) {
                    editServiceInterval(serviceIntervalSnapshot.getKey());
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
    private void addService(String intervalId) {
        // Implementáld a szolgáltatás hozzáadásának logikáját
        Toast.makeText(context, "Add service to interval: " + intervalId, Toast.LENGTH_SHORT).show();
    }

    private void editServiceInterval(String intervalId) {
        // Implementáld a szolgáltatás intervallum szerkesztését
        Toast.makeText(context, "Edit interval: " + intervalId, Toast.LENGTH_SHORT).show();
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

}

