package com.example.bpt;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private List<DashboardActivity.Ride> rides;

    public ItemAdapter(List<DashboardActivity.Ride> rides) { this.rides = rides; }

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

//        holder.itemView.setOnLongClickListener(v -> {
//            Intent intent = new Intent(holder.itemView.getContext(), RideDetailsActivity.class);
//            intent.putExtra("ride", ride.getRideTitle());
//            v.getContext().startActivity(intent);
//        });

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Ride")
                    .setMessage("Are you sure you want to delete this ride?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Ride törlése az adatbázisból
                        deleteRideFromDatabase(ride, position);
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    private void deleteRideFromDatabase(DashboardActivity.Ride ride, int position) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(userId).child("rides");

        rideRef.orderByChild("rideTitle").equalTo(ride.getRideTitle()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().removeValue();
                }
                rides.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, rides.size());

                //updateTotalDistanceForBike(ride.getSelectedBicycle());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ItemAdapter", "Error deleting ride: ", databaseError.toException());
            }
        });
    }

    @Override
    public int getItemCount() { return rides.size(); }

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