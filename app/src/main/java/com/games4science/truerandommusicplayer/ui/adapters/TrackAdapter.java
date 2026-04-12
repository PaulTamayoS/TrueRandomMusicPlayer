package com.games4science.truerandommusicplayer.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.games4science.truerandommusicplayer.R;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<String> trackUris;
    private OnTrackRemoveListener listener;

    public interface OnTrackRemoveListener {
        void onRemove(String uri, int position);
    }

    public TrackAdapter(List<String> trackUris, OnTrackRemoveListener listener) {
        this.trackUris = trackUris;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        String uriString = trackUris.get(position);

        // Simple display: extract name from URI (You can improve this later with MediaMetadataRetriever)
        String fileName = android.net.Uri.parse(uriString).getLastPathSegment();
        holder.tvTrackName.setText(fileName);

        holder.btnRemove.setOnClickListener(v -> listener.onRemove(uriString, position));
    }

    @Override
    public int getItemCount() {
        return trackUris.size();
    }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView tvTrackName;
        View btnRemove;
        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTrackName = itemView.findViewById(R.id.tvTrackName);
            btnRemove = itemView.findViewById(R.id.btnRemoveTrack);
        }
    }
}