package com.example.bpt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private List<DashboardActivity.Ride> rides;

    public ItemAdapter(List<DashboardActivity.Ride> rides) {
        this.rides = rides;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DashboardActivity.Ride ride = rides.get(position);
        holder.bicycleTextView.setText("Bicycle: " + ride.getSelectedBicycle());
        holder.titleTextView.setText("Title: " + ride.getRideTitle());
        holder.dateTextView.setText("Date: " + ride.getDate());
        holder.timeTextView.setText("Time: " + ride.getTime());
        holder.distanceTextView.setText("Distance: " + ride.getDistance() + " km");
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView bicycleTextView;
        public TextView titleTextView;
        public TextView dateTextView;
        public TextView timeTextView;
        public TextView distanceTextView;

        public ViewHolder(View view) {
            super(view);
            bicycleTextView = view.findViewById(R.id.bicycle_text_view);
            titleTextView = view.findViewById(R.id.title_text_view);
            dateTextView = view.findViewById(R.id.date_text_view);
            timeTextView = view.findViewById(R.id.time_text_view);
            distanceTextView = view.findViewById(R.id.distance_text_view);
        }
    }
}
