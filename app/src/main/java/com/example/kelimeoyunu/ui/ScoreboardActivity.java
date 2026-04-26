package com.example.kelimeoyunu.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kelimeoyunu.database.AppDatabase;
import com.example.kelimeoyunu.database.GameEntity;
import com.example.kelimeoyunu.adapters.ScoreAdapter;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardActivity extends AppCompatActivity {

    private ScoreAdapter adapter;
    private List<GameEntity> allGames = new ArrayList<>();

    private TextView tvTotalGames, tvHighScore, tvAvgScore, tvTotalWords, tvLongestWord, tvTotalTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#FFE0B2"));
        mainLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#FF9800"));
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        Button btnBack = new Button(this);
        btnBack.setText("❮ GERİ");
        btnBack.setTextColor(Color.WHITE);
        btnBack.setBackgroundColor(Color.TRANSPARENT);
        btnBack.setTypeface(Typeface.DEFAULT_BOLD);
        btnBack.setOnClickListener(v -> finish());
        topBar.addView(btnBack);

        TextView titleText = new TextView(this);
        titleText.setText("SKOR TABLOSU");
        titleText.setTextSize(20f);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(Color.WHITE);
        titleText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleText.setLayoutParams(titleParams);
        topBar.addView(titleText);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(btnBack.getLayoutParams().width, 1));
        topBar.addView(spacer);
        mainLayout.addView(topBar);

        CardView summaryCard = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = dpToPx(16);
        cardParams.setMargins(margin, margin, margin, margin);
        summaryCard.setLayoutParams(cardParams);
        summaryCard.setCardBackgroundColor(Color.WHITE);
        summaryCard.setRadius(dpToPx(15));
        summaryCard.setCardElevation(dpToPx(6));

        LinearLayout summaryLayout = new LinearLayout(this);
        summaryLayout.setOrientation(LinearLayout.VERTICAL);
        summaryLayout.setPadding(margin, margin, margin, margin);

        TextView summaryTitle = new TextView(this);
        summaryTitle.setText("Genel Performans");
        summaryTitle.setTextColor(Color.parseColor("#FF5722"));
        summaryTitle.setTextSize(18f);
        summaryTitle.setTypeface(Typeface.DEFAULT_BOLD);
        summaryLayout.addView(summaryTitle);

        tvTotalGames = createSummaryTextView(); summaryLayout.addView(tvTotalGames);
        tvHighScore = createSummaryTextView(); summaryLayout.addView(tvHighScore);
        tvAvgScore = createSummaryTextView(); summaryLayout.addView(tvAvgScore);
        tvTotalWords = createSummaryTextView(); summaryLayout.addView(tvTotalWords);
        tvLongestWord = createSummaryTextView(); tvLongestWord.setTextColor(Color.parseColor("#4CAF50")); tvLongestWord.setTypeface(Typeface.DEFAULT_BOLD); summaryLayout.addView(tvLongestWord);
        tvTotalTime = createSummaryTextView(); summaryLayout.addView(tvTotalTime);

        summaryCard.addView(summaryLayout);
        mainLayout.addView(summaryCard);

        LinearLayout chipLayout = new LinearLayout(this);
        chipLayout.setOrientation(LinearLayout.HORIZONTAL);
        chipLayout.setGravity(Gravity.CENTER);
        chipLayout.setPadding(0, 0, 0, dpToPx(8));

        chipLayout.addView(createSortButton("Tarih", "date"));
        chipLayout.addView(createSortButton("Skor", "score"));
        chipLayout.addView(createSortButton("Kelime Uz.", "word"));

        mainLayout.addView(chipLayout);

        RecyclerView rvScoreboard = new RecyclerView(this);
        LinearLayout.LayoutParams rvParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        rvParams.weight = 1;
        rvScoreboard.setLayoutParams(rvParams);
        rvScoreboard.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ScoreAdapter(new ArrayList<>());
        rvScoreboard.setAdapter(adapter);
        mainLayout.addView(rvScoreboard);

        setContentView(mainLayout);

        new Thread(() -> {
            allGames = AppDatabase.getInstance(this).appDao().getAllGamesByScore();
            calculateAndSetSummary(allGames);
            runOnUiThread(() -> sortBy("date"));
        }).start();
    }

    private Button createSortButton(String text, final String mode) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(12f);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#8D6E63"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        btn.setLayoutParams(params);
        btn.setOnClickListener(v -> sortBy(mode));
        return btn;
    }

    private void sortBy(String mode) {
        if (allGames == null || allGames.isEmpty()) return;
        List<GameEntity> sorted = new ArrayList<>(allGames);
        switch (mode) {
            case "score":
                sorted.sort((a, b) -> b.score - a.score);
                break;
            case "date":
                sorted.sort((a, b) -> b.gameId - a.gameId);
                break;
            case "word":
                sorted.sort((a, b) -> {
                    int la = a.longestWord != null ? a.longestWord.length() : 0;
                    int lb = b.longestWord != null ? b.longestWord.length() : 0;
                    return lb - la;
                });
                break;
        }
        adapter.updateData(sorted);
    }

    private TextView createSummaryTextView() {
        TextView tv = new TextView(this);
        tv.setTextColor(Color.parseColor("#333333"));
        tv.setTextSize(14f);
        tv.setPadding(0, dpToPx(2), 0, dpToPx(2));
        return tv;
    }

    private void calculateAndSetSummary(List<GameEntity> games) {
        if (games == null || games.isEmpty()) return;

        int totalGames = games.size();
        int highestScore = 0;
        int totalScore = 0;
        int totalWords = 0;
        int totalSeconds = 0;
        String overallLongestWord = "";

        for (GameEntity game : games) {
            if (game.score > highestScore) highestScore = game.score;
            totalScore += game.score;
            totalWords += game.wordCount;
            totalSeconds += game.duration; // Artık duration direkt INTEGER saniye olarak geliyor

            if (game.longestWord != null && game.longestWord.length() > overallLongestWord.length()) {
                overallLongestWord = game.longestWord;
            }
        }

        int avgScore = totalScore / totalGames;

        int totalHours = totalSeconds / 3600;
        int totalMinutes = (totalSeconds % 3600) / 60;
        String timeString = (totalHours > 0) ? totalHours + " saat " + totalMinutes + " dk" : totalMinutes + " dk";

        final int fTotalGames = totalGames;
        final int fHighestScore = highestScore;
        final int fAvgScore = avgScore;
        final int fTotalWords = totalWords;
        final String fLongestWord = overallLongestWord.isEmpty() ? "-" : overallLongestWord.toUpperCase();

        runOnUiThread(() -> {
            tvTotalGames.setText("Toplam Oyun: " + fTotalGames);
            tvHighScore.setText("En Yüksek Puan: " + fHighestScore);
            tvAvgScore.setText("Ortalama Puan: " + fAvgScore);
            tvTotalWords.setText("Toplam Kelime: " + fTotalWords);
            tvLongestWord.setText("En Uzun Kelime: \"" + fLongestWord + "\"");
            tvTotalTime.setText("Toplam Süre: " + timeString);
        });
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}