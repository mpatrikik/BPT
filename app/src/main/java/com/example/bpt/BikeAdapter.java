package com.example.bpt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BikeAdapter extends RecyclerView.Adapter<BikeAdapter.ViewHolder> {
    private List<String> bikes;

    public BikeAdapter(List<String> bikes) {
        this.bikes = bikes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bikeTextView.setText(bikes.get(position));  // Csak a kerékpár nevét jeleníti meg
    }

    @Override
    public int getItemCount() {
        return bikes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView bikeTextView;

        public ViewHolder(View view) {
            super(view);
            bikeTextView = view.findViewById(android.R.id.text1);
        }
    }
}
