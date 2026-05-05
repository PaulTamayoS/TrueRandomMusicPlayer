package com.games4science.truerandommusicplayer.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.games4science.truerandommusicplayer.R;
import com.games4science.truerandommusicplayer.model.Track;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<Track> trackList;
    private OnTrackRemoveListener listener;

    public interface OnTrackRemoveListener {
        void onRemove(String uri, int position);
    }

    public TrackAdapter(List<Track> trackList, OnTrackRemoveListener listener) {
        this.trackList = trackList;
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
        try {
            Track track = trackList.get(position);
            holder.tvTrackName.setText(track.getName() + " - " + track.getArtist());
            holder.btnRemove.setOnClickListener(v -> listener.onRemove(track.getUriString(), position));
        } catch (Exception e) {
            holder.tvTrackName.setText("Error loading track info");
        }
    }

    @Override
    public int getItemCount() {
        // Return size of the correct list
        return trackList != null ? trackList.size() : 0;
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