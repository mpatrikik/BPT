package com.example.bpt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PartAdapter extends RecyclerView.Adapter<PartAdapter.ViewHolder> {
    private List<Part> parts;

    public PartAdapter(List<Part> parts) {
        this.parts = parts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_part, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Part part = parts.get(position);
        holder.partNameTextView.setText(part.getPartName());
        holder.distanceTextView.setText(part.getTotalDistance() + " km");
    }

    @Override
    public int getItemCount() {
        return parts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView partNameTextView;
        public TextView distanceTextView;

        public ViewHolder(View view) {
            super(view);
            partNameTextView = view.findViewById(R.id.part_name_text_view);
            distanceTextView = view.findViewById(R.id.part_distance_text_view);
        }
    }
}
