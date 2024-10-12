package com.example.bpt;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BikeAdapter extends RecyclerView.Adapter<BikeAdapter.ViewHolder> {
    private List<String> bikes;
    private List<String> distances;

    public BikeAdapter(List<String> bikes, List<String> distances) {
        this.bikes = bikes;
        this.distances = distances;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bike, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String bikeName = bikes.get(position);
        String totalDistance = distances.get(position);
        holder.bikeNameTextView.setText(bikeName);
        holder.bikedistanceTextView.setText(totalDistance + " km");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), BikeDetailsActivity.class);
            intent.putExtra("bike_name", bikeName);
            intent.putExtra("total_distance", totalDistance);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return bikes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView bikeNameTextView;
        public TextView bikedistanceTextView;

        public ViewHolder(View view) {
            super(view);
            bikeNameTextView = view.findViewById(R.id.bike_name_text_view);
            bikedistanceTextView = view.findViewById(R.id.bike_distance_text_view);
        }
    }
}