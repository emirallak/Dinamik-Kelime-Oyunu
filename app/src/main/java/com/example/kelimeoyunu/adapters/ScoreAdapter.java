package com.example.kelimeoyunu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kelimeoyunu.R;
import com.example.kelimeoyunu.database.GameEntity;
import java.util.List;

public class ScoreAdapter extends RecyclerView.Adapter<ScoreAdapter.ViewHolder> {

    private List<GameEntity> games;

    public ScoreAdapter(List<GameEntity> games) {
        this.games = games;
    }

    public void updateData(List<GameEntity> newGames) {
        this.games = newGames;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_score_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GameEntity game = games.get(position);
        int rank = position + 1;

        holder.tvRank.setText(String.valueOf(rank));

        // Altın, gümüş, bronz renkleri
        if (rank == 1) holder.tvRank.setTextColor(0xFFBA7517);
        else if (rank == 2) holder.tvRank.setTextColor(0xFF888780);
        else if (rank == 3) holder.tvRank.setTextColor(0xFF993C1D);
        else holder.tvRank.setTextColor(0xFF534AB7);

        holder.tvGameLabel.setText("Oyun #" + game.gameId);
        holder.tvDate.setText(game.date);
        holder.tvScore.setText(game.score + " puan");
        holder.tvGrid.setText(game.gridSize);
        holder.tvWordCount.setText(String.valueOf(game.wordCount));
        holder.tvDuration.setText(game.duration);
        holder.tvLongestWord.setText(game.longestWord);
    }

    @Override
    public int getItemCount() {
        return games != null ? games.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvGameLabel, tvDate, tvScore;
        TextView tvGrid, tvWordCount, tvDuration, tvLongestWord;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank        = itemView.findViewById(R.id.tvRank);
            tvGameLabel   = itemView.findViewById(R.id.tvGameLabel);
            tvDate        = itemView.findViewById(R.id.tvDate);
            tvScore       = itemView.findViewById(R.id.tvScore);
            tvGrid        = itemView.findViewById(R.id.tvGrid);
            tvWordCount   = itemView.findViewById(R.id.tvWordCount);
            tvDuration    = itemView.findViewById(R.id.tvDuration);
            tvLongestWord = itemView.findViewById(R.id.tvLongestWord);
        }
    }
}