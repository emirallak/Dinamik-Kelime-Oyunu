package com.example.kelimeoyunu.utils;

import java.util.Random;

public class LetterGenerator {

    // Dökümanda belirtilen frekanslara göre ağırlıklandırılmış havuz
    // Sık (A, E, İ, L, R, N) -> 5'er kez eklendi
    private static final String HIGH_FREQ = "AAAAAEEEEEİİİİİLLLLLRRRRRNNNNN";
    // Orta (K, M, T, S, Y, D) -> 3'er kez eklendi
    private static final String MED_FREQ = "KKKMMMTTTSSSYYYDDD";
    // Az (J, Ğ, F, V) -> 1'er kez eklendi
    private static final String LOW_FREQ = "JĞFV";
    // Tabloda olan ama dökümanda kategorisi verilmeyen diğer harfler (Denge için 2'şer kez)
    private static final String NORMAL_FREQ = "BBCCÇÇOOÖÖPPŞŞUUÜÜHHGGIIZZ";

    private static final String ALL_LETTERS = HIGH_FREQ + MED_FREQ + LOW_FREQ + NORMAL_FREQ;
    private static final Random random = new Random();

    public static char getRandomLetter() {
        int index = random.nextInt(ALL_LETTERS.length());
        return ALL_LETTERS.charAt(index);
    }
}