package com.example.kelimeoyunu.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kelimeoyunu.R;
import com.example.kelimeoyunu.database.AppDatabase;
import com.example.kelimeoyunu.database.UserEntity;
import com.example.kelimeoyunu.utils.SharedPrefsHelper;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private Button btnStart;
    private SharedPrefsHelper prefsHelper;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Yardımcı sınıfları ve veritabanını başlat
        prefsHelper = new SharedPrefsHelper(this);
        db = AppDatabase.getInstance(this);

        // 1. KONTROL: Kullanıcı daha önce giriş yapmış mı?
        String savedUsername = prefsHelper.getUsername();
        if (savedUsername != null) {
            // Eğer isim varsa doğrudan ana menüye git
            goToMainMenu();
            return;
        }

        // Görsel bileşenleri bağla
        etUsername = findViewById(R.id.etUsername);
        btnStart = findViewById(R.id.btnStart);

        // 2. KAYIT VE GİRİŞ: Butona tıklandığında yapılacak işlemler
        btnStart.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "Lütfen bir kullanıcı adı girin!", Toast.LENGTH_SHORT).show();
            } else {
                handleLogin(username);
            }
        });
    }

    /**
     * Kullanıcıyı hem veritabanında kontrol eder hem de SharedPrefs'e kaydeder.
     */
    private void handleLogin(String username) {
        // Veritabanında bu isimde biri var mı bak
        UserEntity existingUser = db.appDao().getUserByName(username);

        if (existingUser == null) {
            // YENİ KULLANICI: Veritabanına 5000 başlangıç altını ile ekle
            UserEntity newUser = new UserEntity(username, 5000);
            db.appDao().insertUser(newUser);
            Toast.makeText(this, "Hoş geldin " + username + "! 5000 altın hediye edildi.", Toast.LENGTH_LONG).show();
        } else {
            // MEVCUT KULLANICI: Sadece hoş geldin mesajı göster
            Toast.makeText(this, "Tekrar hoş geldin " + username + "!", Toast.LENGTH_SHORT).show();
        }

        // İsim bilgisini cihaz hafızasına (SharedPrefs) kaydet
        prefsHelper.saveUsername(username);

        // Ana menüye yönlendir
        goToMainMenu();
    }

    private void goToMainMenu() {
        Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
        startActivity(intent);
        finish(); // Giriş ekranını kapat ki geri basınca buraya dönmesin
    }
}