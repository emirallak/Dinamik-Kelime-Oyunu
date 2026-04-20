package com.example.kelimeoyunu.utils;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;

public class DictionaryManager {
    // HashSet, listede kelime aramak için en hızlı (O(1)) veri yapısıdır.
    private HashSet<String> dictionary;

    public DictionaryManager(Context context) {
        dictionary = new HashSet<>();
        loadDictionary(context);
    }

    private void loadDictionary(Context context) {
        try {
            // assets klasöründeki words.txt dosyasını oku
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("words.txt"), "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim().toUpperCase();
                if (word.length() >= 3) { // Kurallara göre min 3 harf
                    dictionary.add(word);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isWordValid(String word) {
        if (word == null || word.length() < 3) return false;
        return dictionary.contains(word.toUpperCase());
    }

    public int getDictionarySize() {
        return dictionary.size();
    }
}