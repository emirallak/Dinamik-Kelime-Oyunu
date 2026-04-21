package com.example.kelimeoyunu.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.kelimeoyunu.R;
import com.example.kelimeoyunu.adapters.ScoreAdapter;
import com.example.kelimeoyunu.database.AppDatabase;
import com.example.kelimeoyunu.database.GameEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScoreboardActivity extends AppCompatActivity {

    private ScoreAdapter adapter;
    private List<GameEntity> allGames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoreboard);

        RecyclerView rv = findViewById(R.id.rvScoreboard);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScoreAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        // Geri butonu
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Chip tıklamaları
        findViewById(R.id.chipScore).setOnClickListener(v -> sortBy("score"));
        findViewById(R.id.chipDate).setOnClickListener(v  -> sortBy("date"));
        findViewById(R.id.chipWord).setOnClickListener(v  -> sortBy("word"));

        // Veritabanından yükle (arka planda)
        new Thread(() -> {
            allGames = AppDatabase.getInstance(this).appDao().getAllGamesByScore();
            runOnUiThread(() -> adapter.updateData(allGames));
        }).start();
    }

    private void sortBy(String mode) {
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
}