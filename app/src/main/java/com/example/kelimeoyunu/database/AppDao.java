package com.example.kelimeoyunu.database;



import androidx.room.Dao;

import androidx.room.Insert;

import androidx.room.Query;

import androidx.room.Update;



import java.util.List;



@Dao

public interface AppDao {



// --- User İşlemleri ---



// insertUser metodunun dönüş tipini void yerine long yapmak iyi bir pratiktir,

// böylece yeni eklenen satırın ID'sini döndürür.

    @Insert

    long insertUser(UserEntity user);



    @Query("SELECT * FROM users LIMIT 1")

    UserEntity getUser();



// Yeni: Kullanıcıyı ismine göre veritabanında arama

    @Query("SELECT * FROM users WHERE username = :name LIMIT 1")

    UserEntity getUserByName(String name);



    @Update

    void updateUser(UserEntity user);



// --- Game İşlemleri ---

    @Insert

    void insertGame(GameEntity game);



// Skor tablosu için oyunları tarihe/ID'ye göre en yeniden eskiye sıralayarak getir

    @Query("SELECT * FROM games ORDER BY gameId DESC")

    List<GameEntity> getAllGamesDesc();



// İstatistikler için toplam puanı getir vs. (Gerektikçe SQL sorguları eklenebilir)

    @Query("SELECT SUM(score) FROM games")

    int getTotalScore();



// --- Joker İşlemleri (Yeni Eklenenler) ---



// Bir kullanıcının sahip olduğu tüm jokerleri getirir

    @Query("SELECT * FROM user_jokers WHERE userId = :uId")

    List<JokerEntity> getUserJokers(int uId);



// Kullanıcının belirli bir jokeri (örn: "Balık") daha önce alıp almadığını kontrol eder

    @Query("SELECT * FROM user_jokers WHERE userId = :uId AND jokerName = :jName LIMIT 1")

    JokerEntity getSpecificJoker(int uId, String jName);



    @Insert

    void insertJoker(JokerEntity joker);



    @Update

    void updateJoker(JokerEntity joker);

}