package com.example.kelimeoyunu.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsHelper {

    private static final String PREFS_NAME = "WordCrushPrefs";
    private static final String KEY_USERNAME = "username";

    private SharedPreferences sharedPreferences;

    public SharedPrefsHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Kullanıcı adını kaydeder
    public void saveUsername(String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    // Kayıtlı kullanıcı adını getirir
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    // YENİ EKLENEN METOT: Tüm kayıtlı verileri (oturum dahil) siler
    public void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}