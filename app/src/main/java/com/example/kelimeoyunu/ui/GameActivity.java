package com.example.kelimeoyunu.ui;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kelimeoyunu.database.JokerEntity;
import java.util.List;
import java.util.Locale;

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
    private boolean isGameOver = false;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private int currentDurationSeconds = 0;

    // SES EFEKTLERİ VE MÜZİK
    private MediaPlayer errorSound;
    private MediaPlayer bgMusic; // Arka plan müziği oynatıcısı

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // SES DOSYALARINI YÜKLE (Dosya isimlerini kendine göre düzelt)
        errorSound = MediaPlayer.create(this, R.raw.yanlis_hamle);
        bgMusic = MediaPlayer.create(this, R.raw.arka_plan_muzik);

        // ARKA PLAN MÜZİĞİ AYARLARI
        if (bgMusic != null) {
            bgMusic.setLooping(true); // Sürekli başa sarsın
            bgMusic.setVolume(0.12f, 0.12f); // Ses seviyesi %15 (Sakin ve derinden)
            bgMusic.start(); // Oyuna girer girmez başlasın
        }

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

        // MODERN GERİ TUŞU KONTROLÜ
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isGameOver) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                } else {
                    showExitConfirmation();
                }
            }
        });

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isGameOver) {
                    currentDurationSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
                    updateUI();
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };

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

    // --- UYGULAMA ALTA ALININCA MÜZİĞİ SUSTUR ---
    @Override
    protected void onPause() {
        super.onPause();
        if (bgMusic != null && bgMusic.isPlaying()) {
            bgMusic.pause();
        }
    }

    // --- OYUNA DÖNÜLÜNCE MÜZİĞİ DEVAM ETTİR ---
    @Override
    protected void onResume() {
        super.onResume();
        if (bgMusic != null && !bgMusic.isPlaying() && !isGameOver) {
            bgMusic.start();
        }
    }

    private void startGame() {
        gameBoardView = new GameBoardView(this, null);
        gameBoardContainer.addView(gameBoardView);
        gameBoardView.setupGame(gridSize, dictionaryManager, this);
        tvAvailableWords.setText("Oyun Başladı! Kelime bulabilirsin.");

        startTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
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

        // HATA SESİNİ ÇAL
        if (errorSound != null) {
            if (errorSound.isPlaying()) {
                errorSound.seekTo(0);
            }
            errorSound.start();
        }

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

    @Override
    public void onReturnToMenu() {
        finish();
    }

    private void updateUI() {
        if (tvScore != null) tvScore.setText("Puan: " + currentScore);

        if (tvMoveCount != null) {
            int minutes = currentDurationSeconds / 60;
            int seconds = currentDurationSeconds % 60;
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
            tvMoveCount.setText("Hamle: " + remainingMoves + "   ⏱ " + timeStr);
        }
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Oyunu Bitir")
                .setMessage("Mevcut hamleleriniz iptal edilecek ve oyun bitirilecek. Emin misiniz?")
                .setPositiveButton("Bitir", (dialog, which) -> {
                    isGameOver = true;
                    showGameSummaryAndExit();
                })
                .setNegativeButton("Devam Et", null)
                .show();
    }

    private void saveCurrentGame() {
        if (currentScore > 0 || foundWordCount > 0) {
            dbHelper.saveGame(gridSize, currentScore, foundWordCount, longestWordFound, currentDurationSeconds);
        }
    }

    private void checkGameOver() {
        if (remainingMoves <= 0 && !isGameOver) {
            isGameOver = true;

            // 1. ZAMANI DURDUR
            if (timerHandler != null && timerRunnable != null) {
                timerHandler.removeCallbacks(timerRunnable);
            }

            // 2. MÜZİĞİ SUSTUR
            if (bgMusic != null && bgMusic.isPlaying()) {
                bgMusic.pause();
            }

            // 3. EKRANDAKİ YAZIYI GÜNCELLE
            if (tvAvailableWords != null) {
                tvAvailableWords.setText("Hamleniz Bitti!");
            }

            // 4. ESKİ gameBoardView.showGameOverScreen YERİNE ÖZET EKRANINI ÇAĞIR
            showGameSummaryAndExit();
        }
    }

    private void showGameSummaryAndExit() {
        // 5. OYUNU VERİTABANINA KAYDET
        saveCurrentGame();

        // 6. SÜREYİ HESAPLA
        int minutes = currentDurationSeconds / 60;
        int seconds = currentDurationSeconds % 60;
        String timeStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);

        // 7. ÖZET METNİNİ HAZIRLA VE GÖSTER
        String summaryMessage = "🎯 Skor: " + currentScore + "\n"
                + "📝 Bulunan Kelime: " + foundWordCount + "\n"
                + "👑 En Uzun Kelime: " + (longestWordFound.isEmpty() ? "-" : longestWordFound) + "\n"
                + "⏱ Geçen Süre: " + timeStr;

        new android.app.AlertDialog.Builder(this)
                .setTitle("Oyun Bitti! 🎉")
                .setMessage(summaryMessage)
                .setCancelable(false) // Oyuncu boşluğa tıklayıp kapatamasın
                .setPositiveButton("Ana Menüye Dön", (dialog, which) -> {
                    returnToMainMenu();
                })
                .show();
    }

    // BELLEK YÖNETİMİ: EKRAN KAPANIRKEN SESLERİ TEMİZLE
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        if (errorSound != null) {
            errorSound.release();
            errorSound = null;
        }

        if (bgMusic != null) {
            bgMusic.release();
            bgMusic = null;
        }
    }

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
                    String selectedJoker = "";
                    switch (which) {
                        case 0: selectedJoker = "Lolipop Kırıcı"; break;
                        case 1: selectedJoker = "Tekerlek"; break;
                        case 2: selectedJoker = "Serbest Değiştirme"; break;
                        case 3: selectedJoker = "Balık"; break;
                        case 4: selectedJoker = "Harf Karıştırma"; break;
                        case 5: selectedJoker = "Parti Güçlendiricisi"; break;
                    }
                    handleJokerSelection(selectedJoker, which);
                })
                .show();
    }

    private void handleJokerSelection(String jokerName, int jokerIndex) {
        JokerEntity joker = appDatabase.appDao().getSpecificJoker(currentUser.id, jokerName);

        if (joker != null && joker.quantity > 0) {
            joker.quantity -= 1;
            appDatabase.appDao().updateJoker(joker);
            activateJokerEffect(jokerIndex);
        } else {
            showBuyJokerDialog(jokerName, jokerIndex);
        }
    }

    private void showBuyJokerDialog(String jokerName, int index) {
        int cost = getJokerCost(jokerName);

        // HATA ÇÖZÜMÜ: Artık SharedPrefs değil, direkt gerçek hesaptaki (currentUser) altını çekiyoruz
        int currentGold = currentUser != null ? currentUser.gold : 0;

        new AlertDialog.Builder(this)
                .setTitle("Joker Satın Al")
                .setMessage(jokerName + " jokerini " + cost + " altına satın almak istiyor musunuz?\n\nMevcut Altınınız: " + currentGold)
                .setPositiveButton("Satın Al ve Kullan", (dialog, which) -> {
                    if (currentUser != null && currentUser.gold >= cost) {
                        // 1. Parayı gerçek hesaptan düş ve veritabanını güncelle
                        currentUser.gold -= cost;
                        appDatabase.appDao().updateUser(currentUser);

                        // 2. Jokeri veritabanına ekle veya sayısını artır
                        JokerEntity joker = appDatabase.appDao().getSpecificJoker(currentUser.id, jokerName);

                        if (joker == null) {
                            joker = new JokerEntity(currentUser.id, jokerName, 1);
                            appDatabase.appDao().insertJoker(joker);
                        } else {
                            joker.quantity += 1;
                            appDatabase.appDao().updateJoker(joker);
                        }

                        Toast.makeText(this, "Satın alındı! Kalan Altın: " + currentUser.gold, Toast.LENGTH_SHORT).show();

                        // 3. Jokeri hemen kullan
                        handleJokerSelection(jokerName, index);
                    } else {
                        Toast.makeText(this, "Yeterli altınınız yok! Lütfen ana menüden markete gidip altın alın.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    private int getJokerCost(String jokerName) {
        switch (jokerName) {
            case "Balık": return 100;
            case "Tekerlek": return 200;
            case "Lolipop Kırıcı": return 75;
            case "Serbest Değiştirme": return 125;
            case "Harf Karıştırma": return 300;
            case "Parti Güçlendiricisi": return 400;
            default: return 0;
        }
    }

    private void activateJokerEffect(int jokerIndex) {
        switch (jokerIndex) {
            case 0:
                gameBoardView.enableJokerMode(GameBoardView.JokerMode.LOLLIPOP);
                Toast.makeText(this, "Yok edilecek harfe dokun!", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                gameBoardView.enableJokerMode(GameBoardView.JokerMode.WHEEL);
                Toast.makeText(this, "Patlatılacak merkeze dokun!", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                gameBoardView.enableJokerMode(GameBoardView.JokerMode.SWAP_1);
                Toast.makeText(this, "Değişecek İLK harfe dokun!", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                gameBoardView.useFishJoker();
                break;
            case 4:
                gameBoardView.shuffleBoard();
                break;
            case 5:
                gameBoardView.usePartyBooster();
                break;
        }
    }


    private void returnToMainMenu() {
        // MainActivity yazan yeri senin Ana Menü sınıfının adıyla aynı yap (Eğer farklıysa)
        android.content.Intent intent = new android.content.Intent(GameActivity.this, MainMenuActivity.class);
        // Aradaki "Yeni Oyun Kurulum" ekranlarını hafızadan siler atar:
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}