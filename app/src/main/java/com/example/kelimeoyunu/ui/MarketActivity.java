package com.example.kelimeoyunu.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kelimeoyunu.R;
import com.example.kelimeoyunu.adapters.MarketAdapter;
import com.example.kelimeoyunu.database.AppDatabase;
import com.example.kelimeoyunu.database.JokerEntity;
import com.example.kelimeoyunu.database.UserEntity;
import com.example.kelimeoyunu.models.JokerItem;
import com.example.kelimeoyunu.utils.SharedPrefsHelper;

import java.util.ArrayList;
import java.util.List;

public class MarketActivity extends AppCompatActivity {

    private TextView tvGoldAmount;
    private RecyclerView rvJokers;

    private AppDatabase db;
    private UserEntity currentUser;

    private MarketAdapter adapter;
    private List<JokerItem> jokerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market);

        tvGoldAmount = findViewById(R.id.tvGoldAmount);
        rvJokers = findViewById(R.id.rvJokers);

        db = AppDatabase.getInstance(this);
        SharedPrefsHelper prefs = new SharedPrefsHelper(this);

        String username = prefs.getUsername();
        if (username != null) {
            currentUser = db.appDao().getUserByName(username);
        }

        if (currentUser == null) {
            Toast.makeText(this, "Kullanıcı bilgisi bulunamadı!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        updateGoldText();

        rvJokers.setLayoutManager(new LinearLayoutManager(this));

        // Listeyi başlat ve veritabanıyla birleştir
        jokerList = new ArrayList<>();
        loadJokerData();

        adapter = new MarketAdapter(jokerList, this::buyJoker);
        rvJokers.setAdapter(adapter);
    }

    private void buyJoker(JokerItem item) {
        if (currentUser.gold >= item.price) {
            currentUser.gold -= item.price;
            db.appDao().updateUser(currentUser);

            JokerEntity ownedJoker = db.appDao().getSpecificJoker(currentUser.id, item.name);

            if (ownedJoker == null) {
                db.appDao().insertJoker(new JokerEntity(currentUser.id, item.name, 1));
            } else {
                ownedJoker.quantity += 1;
                db.appDao().updateJoker(ownedJoker);
            }

            updateGoldText();

            // YENİ: Verileri baştan yükle ve adaptöre ekrandaki rozetleri güncellemesini söyle
            loadJokerData();
            adapter.notifyDataSetChanged();

        } else {
            Toast.makeText(this, "Yeterli altınınız yok!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateGoldText() {
        tvGoldAmount.setText("💰 Altın: " + currentUser.gold);
    }

    /**
     * Veritabanından kullanıcının jokerlerini çeker ve arayüzdeki listeye sayılarla birlikte aktarır.
     */
    private void loadJokerData() {
        jokerList.clear();

        // Kullanıcının veritabanındaki joker kayıtlarını al
        List<JokerEntity> ownedJokers = db.appDao().getUserJokers(currentUser.id);

        addJokerToList("Balık", "Rastgele harfleri yok eder.", 100, "🐟", ownedJokers);
        addJokerToList("Tekerlek", "Seçili satır ve sütunu yok eder.", 200, "🎡", ownedJokers);
        addJokerToList("Lolipop Kırıcı", "Seçilen bir harfi yok eder.", 75, "🍭", ownedJokers);
        addJokerToList("Serbest Değiştirme", "Temas eden iki harfin yerini değiştirir.", 125, "🔄", ownedJokers);
        addJokerToList("Harf Karıştırma", "Griddeki harfleri rastgele karıştırır.", 300, "🔀", ownedJokers);
        addJokerToList("Parti Güçlendiricisi", "Tüm harfleri yok edip yenilerini düşürür.", 400, "🎉", ownedJokers);
    }

    private void addJokerToList(String name, String desc, int price, String icon, List<JokerEntity> ownedJokers) {
        int quantity = 0;
        // DB listesinde bu isimde joker var mı kontrol et, varsa miktarını al
        for (JokerEntity entity : ownedJokers) {
            if (entity.jokerName.equals(name)) {
                quantity = entity.quantity;
                break;
            }
        }
        jokerList.add(new JokerItem(name, desc, price, icon, quantity));
    }
}