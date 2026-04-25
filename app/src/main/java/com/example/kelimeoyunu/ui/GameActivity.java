package com.example.kelimeoyunu.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kelimeoyunu.database.JokerEntity;
import java.util.List;

import com.example.kelimeoyunu.R;
import com.example.kelimeoyunu.database.AppDatabase;
import com.example.kelimeoyunu.database.DatabaseHelper;
import com.example.kelimeoyunu.database.UserEntity;
import com.example.kelimeoyunu.utils.DictionaryManager;
import com.example.kelimeoyunu.utils.SharedPrefsHelper;

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

    // Oyun bitti mi kontrolü için
    private boolean isGameOver = false;

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
            btnUseJoker.setOnClickListener(v -> {
                if (!isGameOver) showJokerDialog();
            });
        }

        if (btnFinishGame != null) {
            btnFinishGame.setOnClickListener(v -> {
                if (!isGameOver) showExitConfirmation();
            });
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
        if (isGameOver) return;

        currentScore += scoreToAdd;
        remainingMoves--;

        foundWordCount++;
        if (word.length() > longestWordFound.length()) {
            longestWordFound = word;
        }
        updateUI();
        checkGameOver();
    }

    @Override
    public void onInvalidWord() {
        if (isGameOver) return;

        remainingMoves--;
        updateUI();
        Toast.makeText(this, "Geçersiz kelime! Hamle gitti.", Toast.LENGTH_SHORT).show();
        checkGameOver();
    }

    @Override
    public void onGridAnalyzed(int availableWordCount) {
        if (isGameOver) return;

        runOnUiThread(() -> {
            if (tvAvailableWords != null) {
                tvAvailableWords.setText("Oluşturulabilir Kelime Sayısı: " + availableWordCount);
            }
        });
    }

    @Override
    public void onJokerUsed(String jokerName) {
        if (isGameOver) return;
        Toast.makeText(this, jokerName + " kullanıldı!", Toast.LENGTH_SHORT).show();
    }

    // YENİ EKLENEN: Oyun sonu ekranındaki "Ana Menüye Dön" butonuna basıldığında çağrılır
    @Override
    public void onReturnToMenu() {
        finish();
    }

    private void updateUI() {
        if (tvScore != null) tvScore.setText("Puan: " + currentScore);
        if (tvMoveCount != null) tvMoveCount.setText("Hamle: " + remainingMoves);
    }

    // PDF DÖKÜMANI MADDE 11: GERİ TUŞU KONTROLÜ
    @Override
    public void onBackPressed() {
        if (isGameOver) {
            super.onBackPressed();
            return;
        }
        showExitConfirmation();
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Oyundan Çık")
                .setMessage("Oyundan çıkmak istediğinize emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    saveCurrentGame();
                    finish();
                })
                .setNegativeButton("Hayır", null)
                .show();
    }

    private void saveCurrentGame() {
        if (currentScore > 0 || foundWordCount > 0) {
            int durationSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
            dbHelper.saveGame(gridSize, currentScore, foundWordCount, longestWordFound, durationSeconds);
        }
    }

    // YENİ DÜZENLEME: Hamle bitince oyunu kaydet ve GameBoardView'a bitiş ekranını çizdir
    private void checkGameOver() {
        if (remainingMoves <= 0 && !isGameOver) {
            isGameOver = true;

            // Veritabanına kaydet
            saveCurrentGame();

            if (tvAvailableWords != null) {
                tvAvailableWords.setText("Oyun Bitti!");
            }

            // Oyun alanına kendi özel bitiş ekranını çizdiriyoruz
            if (gameBoardView != null) {
                gameBoardView.showGameOverScreen(currentScore, foundWordCount, longestWordFound);
            }
        }
    }

    // --- JOKER MENÜSÜ ---
    private void showJokerDialog() {
        if (gameBoardView == null || currentUser == null) {
            Toast.makeText(this, "Joker sistemi şu an yüklenemedi.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<JokerEntity> userJokers = appDatabase.appDao().getUserJokers(currentUser.id);
        int cLolipop = 0, cWheel = 0, cSwap = 0, cFish = 0, cShuffle = 0, cParty = 0;

        for (JokerEntity j : userJokers) {
            if (j.jokerName.equals("Lolipop Kırıcı")) cLolipop = j.quantity;
            else if (j.jokerName.equals("Tekerlek")) cWheel = j.quantity;
            else if (j.jokerName.equals("Serbest Değiştirme")) cSwap = j.quantity;
            else if (j.jokerName.equals("Balık")) cFish = j.quantity;
            else if (j.jokerName.equals("Harf Karıştırma")) cShuffle = j.quantity;
            else if (j.jokerName.equals("Parti Güçlendiricisi")) cParty = j.quantity;
        }

        String[] jokerNames = {
                "🍭 Lolipop Kırıcı (" + cLolipop + " Adet)",
                "🎡 Tekerlek (" + cWheel + " Adet)",
                "🔄 Serbest Değiştirme (" + cSwap + " Adet)",
                "🐟 Balık (" + cFish + " Adet)",
                "🌪️ Harf Karıştırma (" + cShuffle + " Adet)",
                "🎉 Parti Güçlendiricisi (" + cParty + " Adet)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Jokerlerin")
                .setItems(jokerNames, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            if (consumeJoker("Lolipop Kırıcı")) {
                                gameBoardView.enableJokerMode(GameBoardView.JokerMode.LOLLIPOP);
                                Toast.makeText(this, "Yok edilecek harfe dokun!", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 1:
                            if (consumeJoker("Tekerlek")) {
                                gameBoardView.enableJokerMode(GameBoardView.JokerMode.WHEEL);
                                Toast.makeText(this, "Patlatılacak merkeze dokun!", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 2:
                            if (consumeJoker("Serbest Değiştirme")) {
                                gameBoardView.enableJokerMode(GameBoardView.JokerMode.SWAP_1);
                                Toast.makeText(this, "Değişecek İLK harfe dokun!", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 3:
                            if (consumeJoker("Balık")) gameBoardView.useFishJoker();
                            break;
                        case 4:
                            if (consumeJoker("Harf Karıştırma")) gameBoardView.shuffleBoard();
                            break;
                        case 5:
                            if (consumeJoker("Parti Güçlendiricisi")) gameBoardView.usePartyBooster();
                            break;
                    }
                })
                .show();
    }

    private boolean consumeJoker(String jokerName) {
        if (currentUser == null) return false;

        JokerEntity joker = appDatabase.appDao().getSpecificJoker(currentUser.id, jokerName);

        if (joker == null || joker.quantity <= 0) {
            Toast.makeText(this, "Elinizde hiç " + jokerName + " kalmamış! Altınla satın almalısınız.", Toast.LENGTH_LONG).show();
            return false;
        }

        joker.quantity -= 1;
        appDatabase.appDao().updateJoker(joker);
        return true;
    }
}