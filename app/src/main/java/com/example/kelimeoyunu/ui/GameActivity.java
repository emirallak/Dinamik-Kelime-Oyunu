package com.example.kelimeoyunu.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kelimeoyunu.R;
import com.example.kelimeoyunu.database.DatabaseHelper;
import com.example.kelimeoyunu.utils.DictionaryManager;

public class GameActivity extends AppCompatActivity implements GameBoardView.GameListener {

    private TextView tvScore, tvMoveCount, tvAvailableWords;
    private FrameLayout gameBoardContainer;
    private Button btnUseJoker, btnFinishGame;

    private GameBoardView gameBoardView;
    private DictionaryManager dictionaryManager;
    private DatabaseHelper dbHelper;

    private int currentScore = 0;
    private int remainingMoves = 15;
    private int gridSize = 6;

    private int foundWordCount = 0;
    private String longestWordFound = "";
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        dbHelper = new DatabaseHelper(this);

        tvScore = findViewById(R.id.tvScore);
        tvMoveCount = findViewById(R.id.tvMoveCount);
        tvAvailableWords = findViewById(R.id.tvAvailableWords);
        gameBoardContainer = findViewById(R.id.gameBoardContainer);
        btnUseJoker = findViewById(R.id.btnUseJoker);
        btnFinishGame = findViewById(R.id.btnFinishGame);

        gridSize = getIntent().getIntExtra("gridSize", 6);
        remainingMoves = getIntent().getIntExtra("moveCount", 15);
        startTime = System.currentTimeMillis();

        updateUI();
        tvAvailableWords.setText("Sözlük yükleniyor... Lütfen bekleyin.");

        new Thread(() -> {
            dictionaryManager = new DictionaryManager(this);
            runOnUiThread(() -> {
                tvAvailableWords.setText("Grid hazırlanıyor...");
                startGame();
            });
        }).start();

        if (btnUseJoker != null) {
            btnUseJoker.setOnClickListener(v -> {
                Toast.makeText(this, "Joker menüsü açılacak...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnFinishGame != null) {
            btnFinishGame.setOnClickListener(v -> showExitConfirmation());
        }
    }

    private void startGame() {
        gameBoardView = new GameBoardView(this, null);
        gameBoardContainer.addView(gameBoardView);
        gameBoardView.setupGame(gridSize, dictionaryManager, this);
        tvAvailableWords.setText("Oyun Başladı! Kelime bulabilirsin.");
    }

    @Override
    public void onWordFound(int scoreToAdd, String word) {
        currentScore += scoreToAdd;
        remainingMoves--;

        foundWordCount++;
        if (word.length() > longestWordFound.length()) {
            longestWordFound = word;
        }

        updateUI();
        Toast.makeText(this, word + " bulundu! +" + scoreToAdd + " Puan", Toast.LENGTH_SHORT).show();
        checkGameOver();
    }

    @Override
    public void onInvalidWord() {
        remainingMoves--;
        updateUI();
        Toast.makeText(this, "Geçersiz kelime! Hamle gitti.", Toast.LENGTH_SHORT).show();
        checkGameOver();
    }

    @Override
    public void onGridAnalyzed(int availableWordCount) {
        runOnUiThread(() -> {
            if (tvAvailableWords != null) {
                tvAvailableWords.setText("Gridde Oluşturulabilir Kelime Sayısı: " + availableWordCount);
            }
        });
    }

    private void updateUI() {
        if (tvScore != null) tvScore.setText("Puan: " + currentScore);
        if (tvMoveCount != null) tvMoveCount.setText("Hamle: " + remainingMoves);
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Oyunu Bitir")
                .setMessage("Mevcut skorunuz kaydedilecek ve oyundan çıkılacaktır. Emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> saveCurrentGameAndExit())
                .setNegativeButton("Hayır", null)
                .show();
    }

    private void saveCurrentGameAndExit() {
        if (currentScore > 0 || foundWordCount > 0) {
            int durationSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
            dbHelper.saveGame(gridSize, currentScore, foundWordCount, longestWordFound, durationSeconds);
            Toast.makeText(this, "Skorunuz kaydedildi!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void checkGameOver() {
        if (remainingMoves <= 0) {
            if (currentScore > 0 || foundWordCount > 0) {
                int durationSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
                dbHelper.saveGame(gridSize, currentScore, foundWordCount, longestWordFound, durationSeconds);
            }
            Toast.makeText(this, "Hamle bitti! Skor kaydedildi.", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}