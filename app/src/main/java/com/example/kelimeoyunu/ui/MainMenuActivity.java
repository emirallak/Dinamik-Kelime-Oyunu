package com.example.kelimeoyunu.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kelimeoyunu.R;
import com.example.kelimeoyunu.utils.SharedPrefsHelper;

public class MainMenuActivity extends AppCompatActivity {

    private TextView tvUsername;
    private SharedPrefsHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        prefsHelper = new SharedPrefsHelper(this);
        tvUsername = findViewById(R.id.tvUsername);

        // Kullanıcı adını sol üstte ekrana yazdır
        tvUsername.setText(prefsHelper.getUsername());

        // Arayüzdeki butonları Java'ya bağla
        Button btnYeniOyun = findViewById(R.id.btnYeniOyun);
        Button btnSkorTablosu = findViewById(R.id.btnSkorTablosu);
        Button btnMarket = findViewById(R.id.btnMarket);

        // 1. YENİ OYUN BUTONU (Ayarlar Ekranına Yönlendirir)
        btnYeniOyun.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, GameSettingsActivity.class);
            startActivity(intent);
        });

        // 2. MARKET BUTONU (Market Ekranına Yönlendirir)
        btnMarket.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, MarketActivity.class);
            startActivity(intent);
        });

        // 3. SKOR TABLOSU BUTONU (Şimdilik yapım aşamasında)
        btnSkorTablosu.setOnClickListener(v -> {
            // İleride SkorActivity'ye yönlendireceğiz
            Toast.makeText(MainMenuActivity.this, "Skor Tablosu henüz eklenmedi!", Toast.LENGTH_SHORT).show();
        });

        // EKSTRA: Sol üstteki kullanıcı ismine basılı tutunca veya tıklayınca hesaptan çıkış yapma
        tvUsername.setOnClickListener(v -> {
            prefsHelper.clear(); // SharedPreferences'tan ismi sil
            Toast.makeText(this, "Çıkış Yapıldı", Toast.LENGTH_SHORT).show();

            // Login ekranına geri dön
            Intent intent = new Intent(MainMenuActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Bu ekranı kapat
        });
    }
}