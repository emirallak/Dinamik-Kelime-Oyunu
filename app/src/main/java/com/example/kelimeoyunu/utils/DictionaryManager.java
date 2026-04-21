package com.example.kelimeoyunu.utils;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DictionaryManager {
    private HashSet<String> dictionary;
    private HashSet<String> prefixes;
    private List<String> longWords;
    private Random random;

    // YENİ: Türkçe dil kurallarını tanımlıyoruz
    private final Locale trLocale = new Locale("tr", "TR");

    public DictionaryManager(Context context) {
        dictionary = new HashSet<>();
        prefixes = new HashSet<>();
        longWords = new ArrayList<>();
        random = new Random();
        loadDictionary(context);
    }

    private void loadDictionary(Context context) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("words.txt"), "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                // YENİ: Sözlükteki kelimeleri KESİNLİKLE Türkçe kurallarına göre büyüt
                String word = line.trim().toUpperCase(trLocale);
                if (word.length() >= 3) {
                    dictionary.add(word);

                    if (word.length() >= 5 && word.length() <= 7) {
                        longWords.add(word);
                    }

                    for (int i = 1; i <= word.length(); i++) {
                        prefixes.add(word.substring(0, i));
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isWordValid(String word) {
        if (word == null || word.length() < 3) return false;
        // YENİ: Gelen kelimeyi Türkçe kurallarına göre büyütüp kontrol et
        return dictionary.contains(word.toUpperCase(trLocale));
    }

    public boolean isPrefixValid(String prefix) {
        if (prefix == null) return false;
        return prefixes.contains(prefix.toUpperCase(trLocale));
    }

    public String getRandomLongWord() {
        if (longWords.isEmpty()) return "KELİME";
        return longWords.get(random.nextInt(longWords.size()));
    }
}