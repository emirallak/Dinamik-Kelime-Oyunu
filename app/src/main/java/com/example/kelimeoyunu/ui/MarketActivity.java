package com.example.kelimeoyunu.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
    private SharedPrefsHelper prefs;
    private MarketAdapter adapter;
    private List<JokerItem> jokerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market);

        tvGoldAmount = findViewById(R.id.tvGoldAmount);
        rvJokers = findViewById(R.id.rvJokers);
        db = AppDatabase.getInstance(this);
        prefs = new SharedPrefsHelper(this);

        rvJokers.setLayoutManager(new LinearLayoutManager(this));

        // Verileri arka planda güvenle yükle
        initData();

        if (tvGoldAmount != null) {
            tvGoldAmount.setOnClickListener(v -> showBuyGoldDialog());
        }
    }

    private void initData() {
        new Thread(() -> {
            String username = prefs.getUsername();
            if (username != null) {
                currentUser = db.appDao().getUserByName(username);

                if (currentUser != null) {
                    loadJokerDataFromDb();
                    runOnUiThread(() -> {
                        updateGoldText();
                        adapter = new MarketAdapter(jokerList, this::buyJoker);
                        rvJokers.setAdapter(adapter);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Kullanıcı bulunamadı!", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void showBuyGoldDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_buy_gold, null);
        AlertDialog paymentDialog = new AlertDialog.Builder(this).setView(dialogView).create();

        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmPurchase);
        btnConfirm.setOnClickListener(v -> {
            new Thread(() -> {
                if (currentUser != null) {
                    currentUser.gold += 1000;
                    db.appDao().updateUser(currentUser);
                    runOnUiThread(() -> {
                        updateGoldText();
                        Toast.makeText(this, "1000 Altın Eklendi! 💰", Toast.LENGTH_SHORT).show();
                        paymentDialog.dismiss();
                    });
                }
            }).start();
        });
        paymentDialog.show();
    }

    private void buyJoker(JokerItem item) {

        if (item.name.equals("1000 Altın Paketi"))
        {
            runOnUiThread(this::showBuyGoldDialog);
            return;
        }
        new Thread(() -> {
            if (currentUser != null && currentUser.gold >= item.price) {
                currentUser.gold -= item.price;
                db.appDao().updateUser(currentUser);

                JokerEntity ownedJoker = db.appDao().getSpecificJoker(currentUser.id, item.name);
                if (ownedJoker == null) {
                    db.appDao().insertJoker(new JokerEntity(currentUser.id, item.name, 1));
                } else {
                    ownedJoker.quantity += 1;
                    db.appDao().updateJoker(ownedJoker);
                }

                // Listeyi güncelle ve UI'ı tazele
                loadJokerDataFromDb();
                runOnUiThread(() -> {
                    updateGoldText();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, item.name + " alındı!", Toast.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Yetersiz Altın!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateGoldText() {
        if (currentUser != null) {
            tvGoldAmount.setText("💰 Altın: " + currentUser.gold);
        }
    }

    private void loadJokerDataFromDb() {
        jokerList.clear();
        List<JokerEntity> ownedJokers = db.appDao().getUserJokers(currentUser.id);

        // Mevcut jokerler...
        addToList("Balık", "Rastgele harfleri yok eder.", 100, "🐟", ownedJokers);
        addToList("Tekerlek", "Seçili satır ve sütunu yok eder.", 200, "🎡", ownedJokers);
        addToList("Lolipop Kırıcı", "Seçilen bir harfi yok eder.", 75, "🍭", ownedJokers);
        addToList("Serbest Değiştirme", "Temas eden iki harfin yerini değiştirir.", 125, "🔄", ownedJokers);
        addToList("Harf Karıştırma", "Griddeki harfleri rastgele karıştırır.", 300, "🔀", ownedJokers);
        addToList("Parti Güçlendiricisi", "Tüm harfleri yok edip yenilerini düşürür.", 400, "🎉", ownedJokers);

        // YENİ: En sona Altın Paketi ekliyoruz (Fiyatı 0 yapıyoruz çünkü bu bir satın alma tetikleyicisi)
        jokerList.add(new JokerItem("1000 Altın Paketi", "Hemen 1000 altın yükle!", 0, "💰", 0));
    }

    private void addToList(String name, String desc, int price, String icon, List<JokerEntity> owned) {
        int qty = 0;
        for (JokerEntity j : owned) {
            if (j.jokerName.equals(name)) { qty = j.quantity; break; }
        }
        jokerList.add(new JokerItem(name, desc, price, icon, qty));
    }
}