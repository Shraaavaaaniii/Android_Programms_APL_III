package com.example.geoagri.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoagri.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {

    private final List<FarmItem> farms;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public FarmAdapter(List<FarmItem> farms) {
        this.farms = farms;
    }

    @NonNull
    @Override
    public FarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_farm, parent, false);
        return new FarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FarmViewHolder holder, int position) {
        FarmItem farm = farms.get(position);
        String name = farm.getFarmName() == null || farm.getFarmName().trim().isEmpty()
                ? "Unnamed Farm"
                : farm.getFarmName().trim();

        holder.farmName.setText(name);
        holder.coordinates.setText(String.format(Locale.getDefault(), "%.6f, %.6f", farm.getLatitude(), farm.getLongitude()));

        if (farm.getTimestamp() != null) {
            holder.dateAdded.setText(dateFormat.format(farm.getTimestamp().toDate()));
        } else {
            holder.dateAdded.setText("N/A");
        }
    }

    @Override
    public int getItemCount() {
        return farms.size();
    }

    static class FarmViewHolder extends RecyclerView.ViewHolder {
        final TextView farmName;
        final TextView coordinates;
        final TextView dateAdded;

        FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            farmName = itemView.findViewById(R.id.textFarmName);
            coordinates = itemView.findViewById(R.id.textFarmCoordinates);
            dateAdded = itemView.findViewById(R.id.textFarmDateAdded);
        }
    }
}
