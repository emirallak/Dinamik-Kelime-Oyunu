package com.example.kelimeoyunu.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "games")
public class GameEntity {

    @PrimaryKey(autoGenerate = true)
    public int gameId;

    public String date;           // Örn: "2026-05-15 14:30:00"
    public int gridSize;          // Örn: 8 (8x8 olduğunu belirtir)
    public int score;             // Toplam puan
    public int wordCount;         // Bulunan kelime sayısı
    public String longestWord;    // En uzun kelime
    public int duration;          // Saniye cinsinden süre (Örn: 150)

    public GameEntity(String date, int gridSize, int score, int wordCount, String longestWord, int duration) {
        this.date = date;
        this.gridSize = gridSize;
        this.score = score;
        this.wordCount = wordCount;
        this.longestWord = longestWord;
        this.duration = duration;
    }
}