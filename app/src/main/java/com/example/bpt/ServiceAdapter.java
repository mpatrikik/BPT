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
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;

import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    private Context context;
    private List<DataSnapshot> serviceList;
    private String partId, partName;

    public ServiceAdapter(Context context, List<DataSnapshot> serviceList, String partId, String partName) {
        this.context = context;
        this.serviceList = serviceList;
        this.partId = partId;
        this.partName = partName;
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

        String serviceName = serviceSnapshot.child("serviceName").getValue(String.class);
        String serviceDate = serviceSnapshot.child("serviceDate").getValue(String.class);
        String overallAddedKm = serviceSnapshot.child("overallAddedKm").getValue(String.class);

        // Setting values in the views
        holder.serviceNameTextView.setText(serviceName != null ? serviceName : "Unknown Service");
        holder.serviceDateTextView.setText(serviceDate != null ? serviceDate : "No Date");
        holder.overallAddedKmTextView.setText(overallAddedKm != null ? overallAddedKm : "0 km");

        // Handling more button popup menu
        holder.moreButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.moreButton);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu_service, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.edit_service) {
                    Toast.makeText(context, "Edit logic for service ID: " + serviceSnapshot.getKey(), Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.delete_service) {
                    Toast.makeText(context, "Delete logic for service ID: " + serviceSnapshot.getKey(), Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });
    }

    private void deleteService(String serviceId, int position) {
        // Logic to delete the service from Firebase
        Toast.makeText(context, "Delete logic for service ID: " + serviceId, Toast.LENGTH_SHORT).show();
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
