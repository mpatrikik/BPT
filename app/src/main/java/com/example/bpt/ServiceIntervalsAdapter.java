package com.example.bpt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        String remainingDistance = "000km of 000km left";
        String lastServiceDate = "Last serviced on 0000.00.00., at 00:00 after 000km";

        holder.serviceIntervalNameTextView.setText(serviceIntervalName != null ? serviceIntervalName : "Unknown");
        holder.remainingDistanceTextView.setText(remainingDistance);
        holder.lastServiceDateTextView.setText(lastServiceDate);
    }

    @Override
    public int getItemCount() { return serviceIntervalsList.size(); }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView serviceIntervalNameTextView, remainingDistanceTextView, lastServiceDateTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceIntervalNameTextView = itemView.findViewById(R.id.service_interval_name_textview);
            remainingDistanceTextView = itemView.findViewById(R.id.service_interval_valuekm_textview);
            lastServiceDateTextView = itemView.findViewById(R.id.last_service_interval_date_textview);
        }
    }
}

