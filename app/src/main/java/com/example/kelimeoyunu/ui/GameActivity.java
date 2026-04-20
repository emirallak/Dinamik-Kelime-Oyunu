package com.example.kelimeoyunu.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kelimeoyunu.R;

public class GameActivity extends AppCompatActivity {

    private int gridSize;
    private int moveCount;
    private int score = 0;

    private TextView tvScore;
    private TextView tvMovesLeft;
    private TextView tvCurrentWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tvScore = findViewById(R.id.tvScore);
        tvMovesLeft = findViewById(R.id.tvMovesLeft);
        tvCurrentWord = findViewById(R.id.tvCurrentWord);

        // Ayarlar ekranından (GameSettingsActivity) gönderilen verileri al
        gridSize = getIntent().getIntExtra("GRID_SIZE", 8); // Bulamazsa varsayılan 8x8
        moveCount = getIntent().getIntExtra("MOVE_COUNT", 20); // Bulamazsa varsayılan 20 hamle

        // Ekrana yazdır
        tvMovesLeft.setText("Hamle: " + moveCount);

        Toast.makeText(this, gridSize + "x" + gridSize + " oyun başlıyor!", Toast.LENGTH_SHORT).show();

        // TODO: Oyun Grid'ini (GameBoardView) oluştur ve gameBoardContainer içine ekle
    }
}