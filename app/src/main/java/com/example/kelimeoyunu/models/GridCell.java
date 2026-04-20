package com.example.kelimeoyunu.models;

public class GridCell {
    public char letter;
    public int row;
    public int col;
    public boolean isSelected;
    public SpecialPower power;

    // Dökümandaki 4, 5, 6 ve 7+ harf uzunluğuna göre bırakılacak güçler
    public enum SpecialPower {
        NONE,           // Normal harf
        ROW_CLEAR,      // 4 harf: Satır Temizleme (A ↔)
        AREA_EXPLODE,   // 5 harf: Alan Patlatma (A *)
        COLUMN_CLEAR,   // 6 harf: Sütun Temizleme (A ↕)
        MEGA_EXPLODE    // 7+ harf: Mega Patlatma (A ✪)
    }

    public GridCell(char letter, int row, int col) {
        this.letter = letter;
        this.row = row;
        this.col = col;
        this.isSelected = false;
        this.power = SpecialPower.NONE;
    }
}