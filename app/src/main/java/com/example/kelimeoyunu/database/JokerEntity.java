package com.example.kelimeoyunu.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_jokers")
public class JokerEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId; // Hangi kullanıcıya ait?
    public String jokerName; // Jokerin ismi (Balık, Tekerlek vb.)
    public int quantity; // Kaç tane var?

    public JokerEntity(int userId, String jokerName, int quantity) {
        this.userId = userId;
        this.jokerName = jokerName;
        this.quantity = quantity;
    }
}