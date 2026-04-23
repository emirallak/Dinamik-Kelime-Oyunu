package com.example.kelimeoyunu.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kelimeoyunu.R;
import com.example.kelimeoyunu.database.AppDatabase;
import com.example.kelimeoyunu.database.DatabaseHelper;
import com.example.kelimeoyunu.database.JokerEntity;
import com.example.kelimeoyunu.database.UserEntity;
import com.example.kelimeoyunu.utils.DictionaryManager;
import com.example.kelimeoyunu.utils.SharedPrefsHelper;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity implements GameBoardView.GameListener {

    private TextView tvScore, tvMoveCount, tvAvailableWords;
    private FrameLayout gameBoardContainer;
    private Button btnUseJoker, btnFinishGame;

    private GameBoardView gameBoardView;
    private DictionaryManager dictionaryManager;
    private DatabaseHelper dbHelper;
    private AppDatabase appDatabase;
    private UserEntity currentUser;

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
        appDatabase = AppDatabase.getInstance(this);

        SharedPrefsHelper prefs = new SharedPrefsHelper(this);
        String username = prefs.getUsername();
        if (username != null) {
            currentUser = appDatabase.appDao().getUserByName(username);
        }

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
            btnUseJoker.setOnClickListener(v -> showJokerDialog());
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

    private void showJokerDialog() {
        if (gameBoardView == null || currentUser == null) {
            Toast.makeText(this, "Joker kullanımı şu anda hazır değil.", Toast.LENGTH_SHORT).show();
            return;
        }

        JokerEntity shuffleJoker = appDatabase.appDao().getSpecificJoker(currentUser.id, "Harf Karıştırma");
        JokerEntity changeLetterJoker = appDatabase.appDao().getSpecificJoker(currentUser.id, "Lolipop Kırıcı");
        int shuffleCount = shuffleJoker != null ? shuffleJoker.quantity : 0;
        int changeLetterCount = changeLetterJoker != null ? changeLetterJoker.quantity : 0;

        if (shuffleCount <= 0 && changeLetterCount <= 0) {
            Toast.makeText(this, "Kullanılabilir jokeriniz yok.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> itemNames = new ArrayList<>();
        List<String> jokerKeys = new ArrayList<>();
        if (shuffleCount > 0) {
            itemNames.add("Harf Karıştırma (" + shuffleCount + ")");
            jokerKeys.add("Harf Karıştırma");
        }
        if (changeLetterCount > 0) {
            itemNames.add("Lolipop Kırıcı (" + changeLetterCount + ")");
            jokerKeys.add("Lolipop Kırıcı");
        }

        new AlertDialog.Builder(this)
                .setTitle("Joker Seç")
                .setItems(itemNames.toArray(new String[0]), (dialog, which) -> {
                    String selectedJoker = jokerKeys.get(which);
                    if ("Harf Karıştırma".equals(selectedJoker)) {
                        if (consumeJoker(selectedJoker)) {
                            gameBoardView.shuffleBoard();
                            Toast.makeText(this, "Grid karıştırıldı.", Toast.LENGTH_SHORT).show();
                        }
                    } else if ("Lolipop Kırıcı".equals(selectedJoker)) {
                        showChangeLetterCellDialog(selectedJoker);
                    }
                })
                .show();
    }

    private void showChangeLetterCellDialog(String jokerName) {
        if (gameBoardView == null) return;

        List<String> cellItems = new ArrayList<>();
        List<int[]> coords = new ArrayList<>();
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                cellItems.add((r + 1) + ". Satır, " + (c + 1) + ". Sütun (" + gameBoardView.getLetterAt(r, c) + ")");
                coords.add(new int[]{r, c});
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Harf Değiştirilecek Hücre")
                .setItems(cellItems.toArray(new String[0]), (dialog, which) -> {
                    int[] selected = coords.get(which);
                    if (consumeJoker(jokerName)) {
                        gameBoardView.changeLetterAt(selected[0], selected[1]);
                        Toast.makeText(this, "Harf değiştirildi.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private boolean consumeJoker(String jokerName) {
        if (currentUser == null) return false;
        JokerEntity joker = appDatabase.appDao().getSpecificJoker(currentUser.id, jokerName);
        if (joker == null || joker.quantity <= 0) {
            Toast.makeText(this, "Bu joker tükenmiş.", Toast.LENGTH_SHORT).show();
            return false;
        }
        joker.quantity -= 1;
        appDatabase.appDao().updateJoker(joker);
        return true;
    }
}
