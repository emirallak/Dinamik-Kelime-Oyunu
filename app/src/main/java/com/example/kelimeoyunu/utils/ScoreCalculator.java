package com.example.kelimeoyunu.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScoreCalculator {
    private static final Map<Character, Integer> POINTS = new HashMap<>();
    private static final Locale TR_LOCALE = new Locale("tr", "TR"); // YENİ

    static {
        POINTS.put('A', 1);  POINTS.put('B', 3);  POINTS.put('C', 4);  POINTS.put('Ç', 4);
        POINTS.put('D', 3);  POINTS.put('E', 1);  POINTS.put('F', 7);  POINTS.put('G', 5);
        POINTS.put('Ğ', 8);  POINTS.put('H', 5);  POINTS.put('I', 2);  POINTS.put('İ', 1);
        POINTS.put('J', 10); POINTS.put('K', 1);  POINTS.put('L', 1);  POINTS.put('M', 2);
        POINTS.put('N', 1);  POINTS.put('O', 2);  POINTS.put('Ö', 7);  POINTS.put('P', 5);
        POINTS.put('R', 1);  POINTS.put('S', 2);  POINTS.put('Ş', 4);  POINTS.put('T', 1);
        POINTS.put('U', 2);  POINTS.put('Ü', 3);  POINTS.put('V', 7);  POINTS.put('Y', 3);
        POINTS.put('Z', 4);
    }

    public static int getLetterScore(char c) {
        // YENİ: Harfi Türkçe kuralına göre String olarak büyütüp ilk karakterini alıyoruz
        String upperStr = String.valueOf(c).toUpperCase(TR_LOCALE);
        return POINTS.getOrDefault(upperStr.charAt(0), 0);
    }

    public static int calculateBaseWordScore(String word) {
        int total = 0;
        // YENİ: Kelimeyi Türkçe kuralına göre büyüt
        for (char c : word.toUpperCase(TR_LOCALE).toCharArray()) {
            total += getLetterScore(c);
        }
        return total;
    }

    public static int calculateComboScore(String mainWord, DictionaryManager dict) {
        int totalComboScore = 0;
        int len = mainWord.length();

        for (int i = 0; i <= len - 3; i++) {
            for (int j = i + 3; j <= len; j++) {
                String subWord = mainWord.substring(i, j);
                if (dict.isWordValid(subWord)) {
                    totalComboScore += calculateBaseWordScore(subWord);
                }
            }
        }
        return totalComboScore;
    }
}