package com.example.kelimeoyunu.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Kendi oluşturduğun mevcut veritabanı adı
    private static final String DATABASE_NAME = "word_crush_db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tablo zaten Room veya senin tarafından oluşturulmuş,
        // ancak güvence olması adına IF NOT EXISTS ile şemanı buraya ekliyoruz.
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS games ("
                + "gameId INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date TEXT,"
                + "gridSize INTEGER,"
                + "score INTEGER,"
                + "wordCount INTEGER,"
                + "longestWord TEXT,"
                + "duration INTEGER"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Mevcut yapıyı bozmamak için boş bırakıyoruz
    }

    public void saveGame(int gridSize, int score, int wordCount, String longestWord, int duration) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentDate = sdf.format(new Date());

            values.put("date", currentDate);
            values.put("gridSize", gridSize);
            values.put("score", score);
            values.put("wordCount", wordCount);
            values.put("longestWord", longestWord);
            values.put("duration", duration);

            long rowId = db.insert("games", null, values);
            db.close();

            // Logcat üzerinden kontrol için:
            android.util.Log.d("DB_LOG", "Oyun kaydedildi. Satır ID: " + rowId);
        } catch (Exception e) {
            android.util.Log.e("DB_LOG", "Hata oluştu: " + e.getMessage());
        }
    }
}