package com.example.kelimeoyunu.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// BURASI ÇOK ÖNEMLİ: JokerEntity.class listeye eklendi ve version 2 yapıldı!
@Database(entities = {UserEntity.class, GameEntity.class, JokerEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AppDao appDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "word_crush_db")
                            .allowMainThreadQueries()
                            // Tablo yapısında değişiklik yaptığımız için hata vermemesi adına eski şemayı sıfırlayıp güncelini kurar
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}