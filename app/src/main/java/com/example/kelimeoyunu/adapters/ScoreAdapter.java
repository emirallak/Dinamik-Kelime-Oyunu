package com.example.kelimeoyunu.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kelimeoyunu.database.GameEntity;

import java.util.ArrayList;
import java.util.List;

public class ScoreAdapter extends RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder> {

    private List<GameEntity> gameList;

    public ScoreAdapter(List<GameEntity> gameList) {
        this.gameList = new ArrayList<>(gameList);
    }

    public void updateData(List<GameEntity> newGames) {
        this.gameList.clear();
        this.gameList.addAll(newGames);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        CardView cardView = new CardView(context);
        RecyclerView.LayoutParams cardParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginCard = dpToPx(context, 8);
        int marginHorizontal = dpToPx(context, 16);
        cardParams.setMargins(marginHorizontal, marginCard, marginHorizontal, marginCard);
        cardView.setLayoutParams(cardParams);
        cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
        cardView.setRadius(dpToPx(context, 20));
        cardView.setCardElevation(dpToPx(context, 4));

        LinearLayout cardLayout = new LinearLayout(context);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingCard = dpToPx(context, 16);
        cardLayout.setPadding(paddingCard, paddingCard, paddingCard, paddingCard);

        LinearLayout topLayout = new LinearLayout(context);
        topLayout.setOrientation(LinearLayout.HORIZONTAL);
        topLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvNumber = new TextView(context);
        LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvNumber.setLayoutParams(numParams);
        tvNumber.setTextColor(Color.parseColor("#E91E63"));
        tvNumber.setTextSize(20f);
        tvNumber.setTypeface(Typeface.DEFAULT_BOLD);
        topLayout.addView(tvNumber);

        TextView tvDate = new TextView(context);
        tvDate.setTextColor(Color.parseColor("#757575"));
        tvDate.setTextSize(14f);
        topLayout.addView(tvDate);

        cardLayout.addView(topLayout);

        View divider = new View(context);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 2));
        divParams.setMargins(0, dpToPx(context, 8), 0, dpToPx(context, 8));
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(Color.parseColor("#FFCC80"));
        cardLayout.addView(divider);

        LinearLayout statsLayout = new LinearLayout(context);
        statsLayout.setOrientation(LinearLayout.HORIZONTAL);
        statsLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout leftCol = new LinearLayout(context);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        leftCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvGrid = new TextView(context); tvGrid.setTextColor(Color.parseColor("#1A237E")); tvGrid.setTypeface(Typeface.DEFAULT_BOLD); leftCol.addView(tvGrid);
        TextView tvScore = new TextView(context); tvScore.setTextColor(Color.parseColor("#1A237E")); leftCol.addView(tvScore);
        TextView tvTime = new TextView(context); tvTime.setTextColor(Color.parseColor("#1A237E")); leftCol.addView(tvTime);
        statsLayout.addView(leftCol);

        LinearLayout rightCol = new LinearLayout(context);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvWordCount = new TextView(context); tvWordCount.setTextColor(Color.parseColor("#1A237E")); rightCol.addView(tvWordCount);
        TextView tvLongest = new TextView(context); tvLongest.setTextColor(Color.parseColor("#4CAF50")); tvLongest.setTypeface(Typeface.DEFAULT_BOLD); rightCol.addView(tvLongest);
        statsLayout.addView(rightCol);

        cardLayout.addView(statsLayout);
        cardView.addView(cardLayout);

        return new ScoreViewHolder(cardView, tvNumber, tvDate, tvGrid, tvScore, tvTime, tvWordCount, tvLongest);
    }

    @Override
    public void onBindViewHolder(@NonNull ScoreViewHolder holder, int position) {
        GameEntity game = gameList.get(position);

        holder.tvGameNumber.setText("Oyun " + game.gameId);
        holder.tvGameDate.setText(game.date != null ? game.date : "");
        holder.tvGameGrid.setText("Grid: " + game.gridSize + "x" + game.gridSize);
        holder.tvGameScore.setText("Puan: " + game.score);
        holder.tvGameWordCount.setText("Kelime Sayısı: " + game.wordCount);

        String longest = (game.longestWord != null && !game.longestWord.isEmpty()) ? game.longestWord.toUpperCase() : "-";
        holder.tvGameLongestWord.setText("En Uzun: \"" + longest + "\"");

        // Süreyi saniyeden formata dönüştür
        int minutes = game.duration / 60;
        int seconds = game.duration % 60;
        holder.tvGameTime.setText("Süre: " + minutes + " dk " + (seconds > 0 ? seconds + " sn" : ""));
    }

    @Override
    public int getItemCount() {
        return gameList == null ? 0 : gameList.size();
    }

    private int dpToPx(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static class ScoreViewHolder extends RecyclerView.ViewHolder {
        TextView tvGameNumber, tvGameDate, tvGameGrid, tvGameScore, tvGameWordCount, tvGameLongestWord, tvGameTime;

        public ScoreViewHolder(@NonNull View itemView, TextView num, TextView date, TextView grid, TextView score, TextView time, TextView words, TextView longest) {
            super(itemView);
            tvGameNumber = num;
            tvGameDate = date;
            tvGameGrid = grid;
            tvGameScore = score;
            tvGameTime = time;
            tvGameWordCount = words;
            tvGameLongestWord = longest;
        }
    }
}