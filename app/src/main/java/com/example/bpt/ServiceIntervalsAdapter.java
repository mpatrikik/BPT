package com.example.bpt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;

import java.util.List;

public class ServiceIntervalsAdapter extends RecyclerView.Adapter<ServiceIntervalsAdapter.ViewHolder> {

    private Context context;
    private List<DataSnapshot> serviceIntervalsList;

    public ServiceIntervalsAdapter(Context context, List<DataSnapshot> serviceIntervalsList) {
        this.context = context;
        this.serviceIntervalsList = serviceIntervalsList;
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

            // Válasszuk ki a megfelelő menüt
            if (isRepeat) {
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu_repeating_serviceintervals, popupMenu.getMenu());
            } else {
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu_non_repeating_serviceintervals, popupMenu.getMenu());
            }

            // Animált megjelenítés
            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.add_service) {
                    addService(serviceIntervalSnapshot.getKey());
                    return true;
                } else if (itemId == R.id.edit_service_interval) {
                    editServiceInterval(serviceIntervalSnapshot.getKey());
                    return true;
                } else if (itemId == R.id.delete_service_interval) {
                    deleteServiceInterval(serviceIntervalSnapshot.getKey());
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

    private void deleteServiceInterval(String intervalId) {
        // Implementáld a szolgáltatás intervallum törlését
        Toast.makeText(context, "Delete interval: " + intervalId, Toast.LENGTH_SHORT).show();
    }

}

