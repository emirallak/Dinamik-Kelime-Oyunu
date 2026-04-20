package com.example.kelimeoyunu.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kelimeoyunu.R;

public class GameSettingsActivity extends AppCompatActivity {

    private RadioGroup rgGridSize, rgMoveCount;
    private Button btnOyunuBaslat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_settings);

        rgGridSize = findViewById(R.id.rgGridSize);
        rgMoveCount = findViewById(R.id.rgMoveCount);
        btnOyunuBaslat = findViewById(R.id.btnOyunuBaslat);

        btnOyunuBaslat.setOnClickListener(v -> {
            int gridSize = 10; // Varsayılan değer
            int moveCount = 25; // Varsayılan değer

            // Hangi Grid seçildi?
            int selectedGridId = rgGridSize.getCheckedRadioButtonId();
            if (selectedGridId == R.id.rbGrid8) gridSize = 8;
            else if (selectedGridId == R.id.rbGrid6) gridSize = 6;

            // Hangi Hamle seçildi?
            int selectedMoveId = rgMoveCount.getCheckedRadioButtonId();
            if (selectedMoveId == R.id.rbMove20) moveCount = 20;
            else if (selectedMoveId == R.id.rbMove15) moveCount = 15;

            // Seçilen değerleri Oyun ekranına (GameActivity) gönder
            Intent intent = new Intent(GameSettingsActivity.this, GameActivity.class);
            intent.putExtra("GRID_SIZE", gridSize);
            intent.putExtra("MOVE_COUNT", moveCount);
            startActivity(intent);
            finish(); // Ayarlar ekranını kapat
        });
    }
}