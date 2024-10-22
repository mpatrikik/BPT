package com.example.bpt;

import android.util.Log;
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
    private onBikeClickListener onBikeClickListener;

    public BikeAdapter(List<String> bikes, List<String> distances, onBikeClickListener onBikeClickListener) {
        this.bikes = bikes;
        this.distances = distances;
        this.onBikeClickListener = onBikeClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bike, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < bikes.size() && position < distances.size()) {
            String bikeName = bikes.get(position);
            String totalDistance = distances.get(position);
            Log.d("BikeAdapter", "Displaying bike: " + bikeName + ", Distance: " + totalDistance);
            holder.bikeNameTextView.setText(bikeName);
            holder.bikedistanceTextView.setText(totalDistance + " km");
            holder.itemView.setOnClickListener(v -> {
                if (onBikeClickListener != null) {
                    onBikeClickListener.onBikeClick(bikeName);
                }
            });
        } else {
            Log.d("BikeAdapter", "Invalid position: " + position + ", bikes size: " + bikes.size() + ", distances size: " + distances.size());
        }
    }

    @Override
    public int getItemCount() {
        Log.d("BikeAdapter", "Item count: " + Math.min(bikes.size(), distances.size()));
        return Math.min(bikes.size(), distances.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView bikeNameTextView;
        public TextView bikedistanceTextView;

        public ViewHolder(View view) {
            super(view);
            bikeNameTextView = view.findViewById(R.id.bike_name_text_view);
            bikedistanceTextView = view.findViewById(R.id.bike_distance_text_view);
            Log.d("BikeAdapter", "ViewHolder initialized with TextViews");
        }
    }

    public interface onBikeClickListener {
        void onBikeClick(String bikeName);
    }
}