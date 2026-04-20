package com.example.kelimeoyunu.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String username;


    public int gold;

    public UserEntity(String username, int gold) {
        this.username = username;
        this.gold = gold;
    }
}