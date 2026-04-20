package com.example.kelimeoyunu.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
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

        // Kullanıcı adını ekrana yazdır
        tvUsername.setText(prefsHelper.getUsername());

        // Sol üstteki isme tıklayınca isim değiştirme mantığı (örneğin bir Dialog açılabilir)
        tvUsername.setOnClickListener(v -> {
            // TODO: İsim değiştirme Dialog'u (Açılır pencere) eklenecek
        });

        Button btnYeniOyun = findViewById(R.id.btnYeniOyun);
        Button btnSkorTablosu = findViewById(R.id.btnSkorTablosu);
        Button btnMarket = findViewById(R.id.btnMarket);

        btnYeniOyun.setOnClickListener(v -> {
            // TODO: GameSettingsActivity'ye (Grid boyutu seçimi) geçiş yapılacak
        });


        btnMarket.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, MarketActivity.class);
            startActivity(intent);
        });

        // Diğer butonların tıklanma olayları...
    }
}