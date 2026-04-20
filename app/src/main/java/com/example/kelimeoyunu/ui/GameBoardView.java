package com.example.kelimeoyunu.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.kelimeoyunu.models.GridCell;
import com.example.kelimeoyunu.utils.DictionaryManager;
import com.example.kelimeoyunu.utils.LetterGenerator;
import com.example.kelimeoyunu.utils.ScoreCalculator;

import java.util.ArrayList;
import java.util.List;

public class GameBoardView extends View {

    private int gridSize = 8;
    private GridCell[][] cells;
    private List<GridCell> currentPath = new ArrayList<>();

    private Paint cellPaint, textPaint, selectedPaint, powerPaint, linePaint;
    private float cellSize;

    private DictionaryManager dictionaryManager;
    private GameListener gameListener;

    // GameActivity ile haberleşmek için arayüz (Interface)
    public interface GameListener {
        void onWordFound(int scoreToAdd, String word);
        void onInvalidWord();
    }

    public GameBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Çizim fırçalarını hazırla
        cellPaint = new Paint();
        cellPaint.setColor(Color.WHITE);
        cellPaint.setStyle(Paint.Style.FILL);

        selectedPaint = new Paint();
        selectedPaint.setColor(Color.parseColor("#FFCA28")); // Seçili hücre rengi (Sarı)
        selectedPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(60f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        powerPaint = new Paint();
        powerPaint.setColor(Color.RED);
        powerPaint.setTextSize(30f);
        powerPaint.setTextAlign(Paint.Align.RIGHT);

        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#80FFCA28"));
        linePaint.setStrokeWidth(15f);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    public void setupGame(int size, DictionaryManager dict, GameListener listener) {
        this.gridSize = size;
        this.dictionaryManager = dict;
        this.gameListener = listener;
        this.cells = new GridCell[size][size];

        // Tahtayı rastgele harflerle doldur
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                cells[r][c] = new GridCell(LetterGenerator.getRandomLetter(), r, c);
            }
        }
        invalidate(); // Ekranı çiz
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cells == null) return;

        cellSize = (float) getWidth() / gridSize;

        // 1. Hücreleri ve Harfleri Çiz
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                GridCell cell = cells[r][c];
                float left = c * cellSize;
                float top = r * cellSize;

                // Arkaplan boyası
                Paint currentBgPaint = cell.isSelected ? selectedPaint : cellPaint;
                canvas.drawRect(left + 2, top + 2, left + cellSize - 2, top + cellSize - 2, currentBgPaint);

                // Harfi çiz
                float textY = top + (cellSize / 2) - ((textPaint.descent() + textPaint.ascent()) / 2);
                canvas.drawText(String.valueOf(cell.letter), left + (cellSize / 2), textY, textPaint);

                // Özel güç varsa köşeye küçük simgesini çiz
                if (cell.power != GridCell.SpecialPower.NONE) {
                    String powerSymbol = getPowerSymbol(cell.power);
                    canvas.drawText(powerSymbol, left + cellSize - 10, top + 40, powerPaint);
                }
            }
        }

        // 2. Seçilen harfler arasına bağlantı çizgisi çek
        if (currentPath.size() > 1) {
            for (int i = 0; i < currentPath.size() - 1; i++) {
                GridCell start = currentPath.get(i);
                GridCell end = currentPath.get(i + 1);

                float startX = start.col * cellSize + (cellSize / 2);
                float startY = start.row * cellSize + (cellSize / 2);
                float endX = end.col * cellSize + (cellSize / 2);
                float endY = end.row * cellSize + (cellSize / 2);

                canvas.drawLine(startX, startY, endX, endY, linePaint);
            }
        }
    }

    private String getPowerSymbol(GridCell.SpecialPower power) {
        switch (power) {
            case ROW_CLEAR: return "↔";
            case AREA_EXPLODE: return "✱";
            case COLUMN_CLEAR: return "↕";
            case MEGA_EXPLODE: return "✪";
            default: return "";
        }
    }

    // --- DOKUNMATİK EKRAN VE 8 YÖNLÜ SEÇİM KONTROLLERİ ---
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cells == null) return false;

        float x = event.getX();
        float y = event.getY();
        int col = (int) (x / cellSize);
        int row = (int) (y / cellSize);

        // Ekran sınırları dışına çıkıldıysa
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) return true;

        GridCell touchedCell = cells[row][col];

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startSelection(touchedCell);
                break;
            case MotionEvent.ACTION_MOVE:
                continueSelection(touchedCell);
                break;
            case MotionEvent.ACTION_UP:
                finishSelection();
                break;
        }
        invalidate(); // Çizimi güncelle
        return true;
    }

    private void startSelection(GridCell cell) {
        clearSelection();
        cell.isSelected = true;
        currentPath.add(cell);
    }

    private void continueSelection(GridCell cell) {
        if (!cell.isSelected && !currentPath.isEmpty()) {
            GridCell lastCell = currentPath.get(currentPath.size() - 1);

            // 8 Yönlü Komşuluk Kontrolü (Satır veya Sütun farkı en fazla 1 olmalı)
            int rowDiff = Math.abs(cell.row - lastCell.row);
            int colDiff = Math.abs(cell.col - lastCell.col);

            if (rowDiff <= 1 && colDiff <= 1) {
                cell.isSelected = true;
                currentPath.add(cell);
            }
        }
    }

    // --- KELİME DOĞRULAMA, PATLATMA VE YERÇEKİMİ ---
    private void finishSelection() {
        if (currentPath.size() < 3) {
            clearSelection();
            if (gameListener != null) gameListener.onInvalidWord();
            return;
        }

        // Seçilen harflerden kelimeyi oluştur
        StringBuilder wordBuilder = new StringBuilder();
        for (GridCell cell : currentPath) {
            wordBuilder.append(cell.letter);
        }
        String word = wordBuilder.toString();

        // Kelime sözlükte var mı?
        if (dictionaryManager.isWordValid(word)) {
            // 1. Combo puanı hesapla
            int score = ScoreCalculator.calculateComboScore(word, dictionaryManager);

            // 2. Dökümandaki özel güç kuralı (Son harfin yerine bırakılacak güç)
            GridCell lastCell = currentPath.get(currentPath.size() - 1);
            GridCell.SpecialPower powerToLeave = GridCell.SpecialPower.NONE;

            if (word.length() == 4) powerToLeave = GridCell.SpecialPower.ROW_CLEAR;
            else if (word.length() == 5) powerToLeave = GridCell.SpecialPower.AREA_EXPLODE;
            else if (word.length() == 6) powerToLeave = GridCell.SpecialPower.COLUMN_CLEAR;
            else if (word.length() >= 7) powerToLeave = GridCell.SpecialPower.MEGA_EXPLODE;

            // TODO: Eğer patlayan hücrelerde önceden bırakılmış bir güç varsa onları tetikle.

            // 3. Hücreleri patlat (Boşalt)
            for (GridCell cell : currentPath) {
                cell.letter = ' '; // Boşluk karakteri atıyoruz
            }

            // 4. Son hücreye özel gücü bırak ve yeni bir harf ver
            if (powerToLeave != GridCell.SpecialPower.NONE) {
                cells[lastCell.row][lastCell.col].letter = LetterGenerator.getRandomLetter();
                cells[lastCell.row][lastCell.col].power = powerToLeave;
            }

            // 5. Yerçekimini uygula
            applyGravity();

            // Puanı UI'a gönder
            if (gameListener != null) gameListener.onWordFound(score, word);

        } else {
            // Kelime sözlükte yoksa iptal et
            if (gameListener != null) gameListener.onInvalidWord();
        }

        clearSelection();
    }

    private void applyGravity() {
        // Her sütunu kendi içinde aşağı kaydır
        for (int c = 0; c < gridSize; c++) {
            int emptyRow = gridSize - 1; // En alttan başla

            for (int r = gridSize - 1; r >= 0; r--) {
                if (cells[r][c].letter != ' ') {
                    // Eğer harf varsa ve aşağısı boşsa onu aşağı taşı
                    if (r != emptyRow) {
                        cells[emptyRow][c].letter = cells[r][c].letter;
                        cells[emptyRow][c].power = cells[r][c].power;

                        cells[r][c].letter = ' ';
                        cells[r][c].power = GridCell.SpecialPower.NONE;
                    }
                    emptyRow--;
                }
            }

            // Üstte kalan boşlukları yeni rastgele harflerle doldur
            for (int r = emptyRow; r >= 0; r--) {
                cells[r][c].letter = LetterGenerator.getRandomLetter();
                cells[r][c].power = GridCell.SpecialPower.NONE;
            }
        }
        invalidate(); // Animasyonsuz anında çiz
    }

    private void clearSelection() {
        for (GridCell cell : currentPath) {
            cell.isSelected = false;
        }
        currentPath.clear();
        invalidate();
    }
}