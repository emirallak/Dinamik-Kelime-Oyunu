package com.example.kelimeoyunu.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "games")
public class GameEntity {

    @PrimaryKey(autoGenerate = true)
    public int gameId;

    public String date;           // Örn: "15.05.2026"
    public String gridSize;       // Örn: "8x8"
    public int score;             // Toplam puan
    public int wordCount;         // Bulunan kelime sayısı
    public String longestWord;    // En uzun kelime
    public String duration;       // Örn: "6 dk"

    public GameEntity(String date, String gridSize, int score, int wordCount, String longestWord, String duration) {
        this.date = date;
        this.gridSize = gridSize;
        this.score = score;
        this.wordCount = wordCount;
        this.longestWord = longestWord;
        this.duration = duration;
    }
}