package com.example.kelimeoyunu.utils;

import java.util.Random;

public class LetterGenerator {

    // Dökümandaki Kurallar:
    // Yüksek: A, E, İ, L, R, N (Ağırlık: 8)
    // Orta: K, M, T, S, Y, D (Ağırlık: 4)
    // Düşük: J, Ğ, F, V (Ağırlık: 1)
    // Diğerleri: Tabloda olup kuralda yazmayanlar (Ağırlık: 2)

    private static final String HIGH = "AEİLRN";
    private static final String MED = "KMTSYD";
    private static final String LOW = "JĞFV";
    private static final String OTHER = "BCÇOÖPUÜHGIZ";

    private static final String ALL_LETTERS;
    private static final Random random = new Random();

    // Sınıf ilk yüklendiğinde ağırlıklı havuzu 1 kere oluşturur
    static {
        StringBuilder sb = new StringBuilder();
        for (char c : HIGH.toCharArray()) { for (int i = 0; i < 8; i++) sb.append(c); }
        for (char c : MED.toCharArray()) { for (int i = 0; i < 4; i++) sb.append(c); }
        for (char c : LOW.toCharArray()) { for (int i = 0; i < 1; i++) sb.append(c); }
        for (char c : OTHER.toCharArray()) { for (int i = 0; i < 2; i++) sb.append(c); }
        ALL_LETTERS = sb.toString();
    }

    public static char getRandomLetter() {
        return ALL_LETTERS.charAt(random.nextInt(ALL_LETTERS.length()));
    }
}