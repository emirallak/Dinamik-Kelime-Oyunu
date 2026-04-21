package com.example.kelimeoyunu.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kelimeoyunu.R;

public class GameSettingsActivity extends AppCompatActivity {

    private int selectedGridSize = 6; // Varsayılan
    private int selectedMoveCount = 15; // Varsayılan

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_settings);

        RadioGroup rgGridSize = findViewById(R.id.rgGridSize);
        RadioGroup rgMoveCount = findViewById(R.id.rgMoveCount);
        Button btnStart = findViewById(R.id.btnStartGame);

        // 1. Grid Boyutu Seçimi
        rgGridSize.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb6x6) selectedGridSize = 6;
            else if (checkedId == R.id.rb8x8) selectedGridSize = 8;
            else if (checkedId == R.id.rb10x10) selectedGridSize = 10;
        });

        // 2. Hamle Sayısı Seçimi
        rgMoveCount.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb15) selectedMoveCount = 15;
            else if (checkedId == R.id.rb20) selectedMoveCount = 20;
            else if (checkedId == R.id.rb25) selectedMoveCount = 25;
        });

        // 3. OYUNU BAŞLAT
        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(GameSettingsActivity.this, GameActivity.class);

            // KRİTİK: İsimlerin GameActivity'deki getIntExtra ile aynı olması lazım
            intent.putExtra("gridSize", selectedGridSize);
            intent.putExtra("moveCount", selectedMoveCount);

            startActivity(intent);
        });
    }
}