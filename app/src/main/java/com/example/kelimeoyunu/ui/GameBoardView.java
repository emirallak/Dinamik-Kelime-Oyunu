package com.example.kelimeoyunu.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class GameBoardView extends View {

    private int gridSize = 8;
    private GridCell[][] cells;
    private List<GridCell> currentPath = new ArrayList<>();

    private Paint cellPaint, textPaint, selectedPaint, powerPaint, linePaint;
    private float cellSize;

    private DictionaryManager dictionaryManager;
    private GameListener gameListener;
    private Random random = new Random();

    // ANİMASYON DURUMLARI
    private boolean isAnimating = false;
    private float explosionProgress = 0f;
    private List<GridCell> explodingCells = new ArrayList<>();

    private boolean isShuffling = false;
    private float shuffleProgress = 1f;

    // YENİ JOKER MODLARI
    public enum JokerMode { NONE, LOLLIPOP, WHEEL, SWAP_1, SWAP_2 }
    private JokerMode currentJokerMode = JokerMode.NONE;
    private GridCell swapTarget1 = null;

    // SWAP (Takas) Animasyonu
    private boolean isSwapping = false;
    private float swapProgress = 1f;
    private GridCell swapTarget2 = null;

    public interface GameListener {
        void onWordFound(int scoreToAdd, String word);
        void onInvalidWord();
        void onGridAnalyzed(int availableWordCount);
        void onJokerUsed(String jokerName);
    }

    public GameBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cellPaint = new Paint(); cellPaint.setColor(Color.WHITE); cellPaint.setStyle(Paint.Style.FILL);
        selectedPaint = new Paint(); selectedPaint.setColor(Color.parseColor("#FFCA28")); selectedPaint.setStyle(Paint.Style.FILL);
        textPaint = new Paint(); textPaint.setColor(Color.BLACK); textPaint.setTextSize(60f); textPaint.setTextAlign(Paint.Align.CENTER); textPaint.setAntiAlias(true);
        powerPaint = new Paint(); powerPaint.setColor(Color.RED); powerPaint.setTextSize(40f); powerPaint.setTextAlign(Paint.Align.RIGHT);
        linePaint = new Paint(); linePaint.setColor(Color.parseColor("#80FFCA28")); linePaint.setStrokeWidth(15f); linePaint.setStrokeJoin(Paint.Join.ROUND); linePaint.setStyle(Paint.Style.STROKE);
    }

    public void setupGame(int size, DictionaryManager dict, GameListener listener) {
        this.gridSize = size;
        this.dictionaryManager = dict;
        this.gameListener = listener;
        this.cells = new GridCell[size][size];

        boolean isGridPlayable = false;
        int wordCount = 0;

        while (!isGridPlayable) {
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    cells[r][c] = new GridCell(' ', r, c);
                }
            }

            // Test için banko 7 harfli kelime
            String testWord = "DENEMEK";
            int middleRow = size / 2;
            int startCol = (size - 7) / 2;
            if (size >= 7) {
                for (int i = 0; i < 7; i++) cells[middleRow][startCol + i].letter = testWord.charAt(i);
            }

            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (cells[r][c].letter == ' ') {
                        cells[r][c].letter = LetterGenerator.getRandomLetter();
                    }
                }
            }

            wordCount = calculateAvailableWords();
            if (wordCount >= 3) isGridPlayable = true;
        }
        if (gameListener != null) gameListener.onGridAnalyzed(wordCount);
        invalidate();
    }

    // --- ANLIK ÇİZİM VE EFEKTLER (UI) ---
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cells == null) return;
        cellSize = (float) getWidth() / gridSize;

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                GridCell cell = cells[r][c];

                boolean isExploding = isAnimating && explodingCells.contains(cell);
                boolean isCellSwapping = isSwapping && (cell == swapTarget1 || cell == swapTarget2);

                float currentCellSize = cellSize;
                int currentAlpha = 255;
                float left = c * cellSize;
                float top = r * cellSize;

                if (isExploding) {
                    currentCellSize = cellSize * (1f - explosionProgress);
                    currentAlpha = (int) (255 * (1f - explosionProgress));
                } else if (isShuffling) {
                    currentCellSize = cellSize * shuffleProgress;
                    currentAlpha = (int) (255 * shuffleProgress);
                } else if (isCellSwapping) {
                    currentCellSize = cellSize * swapProgress;
                }

                left += (cellSize - currentCellSize) / 2;
                top += (cellSize - currentCellSize) / 2;

                cellPaint.setAlpha(currentAlpha);
                textPaint.setAlpha(currentAlpha);
                powerPaint.setAlpha(currentAlpha);

                // Joker Moduna Göre Arkaplan Rengi (Görsel Geri Bildirim)
                Paint bgPaint = cellPaint;
                if (cell.isSelected || cell == swapTarget1) {
                    bgPaint = selectedPaint;
                } else if (currentJokerMode == JokerMode.LOLLIPOP) {
                    cellPaint.setColor(Color.parseColor("#FFF3E0")); // Turuncu
                } else if (currentJokerMode == JokerMode.WHEEL) {
                    cellPaint.setColor(Color.parseColor("#F3E5F5")); // Mor
                } else if (currentJokerMode == JokerMode.SWAP_1 || currentJokerMode == JokerMode.SWAP_2) {
                    cellPaint.setColor(Color.parseColor("#E3F2FD")); // Mavi
                } else {
                    cellPaint.setColor(Color.WHITE);
                }

                canvas.drawRect(left + 2, top + 2, left + currentCellSize - 2, top + currentCellSize - 2, bgPaint);
                float textY = top + (currentCellSize / 2) - ((textPaint.descent() + textPaint.ascent()) / 2);
                canvas.drawText(String.valueOf(cell.letter), left + (currentCellSize / 2), textY, textPaint);

                if (cell.power != GridCell.SpecialPower.NONE) {
                    canvas.drawText(getPowerSymbol(cell.power), left + currentCellSize - 10, top + 40 * (currentCellSize/cellSize), powerPaint);
                }

                cellPaint.setAlpha(255);
                textPaint.setAlpha(255);
                powerPaint.setAlpha(255);
            }
        }

        if (currentPath.size() > 1 && currentJokerMode == JokerMode.NONE) {
            for (int i = 0; i < currentPath.size() - 1; i++) {
                GridCell start = currentPath.get(i), end = currentPath.get(i + 1);
                canvas.drawLine(start.col * cellSize + (cellSize / 2), start.row * cellSize + (cellSize / 2),
                        end.col * cellSize + (cellSize / 2), end.row * cellSize + (cellSize / 2), linePaint);
            }
        }
    }

    private String getPowerSymbol(GridCell.SpecialPower power) {
        switch (power) { case ROW_CLEAR: return "⇆"; case AREA_EXPLODE: return "✹"; case COLUMN_CLEAR: return "⇅"; case MEGA_EXPLODE: return "✪"; default: return ""; }
    }

    // --- DOKUNMATİK EKRAN VE JOKER HEDEFLERİ ---
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cells == null || isAnimating || isShuffling || isSwapping) return false;

        float x = event.getX(), y = event.getY();
        int col = (int) (x / cellSize), row = (int) (y / cellSize);
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) return true;

        GridCell touchedCell = cells[row][col];

        // EĞER BİR JOKER MODU AKTİFSE NORMAL KELİME SEÇİMİNİ İPTAL ET
        if (currentJokerMode != JokerMode.NONE) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                handleJokerTouch(touchedCell);
            }
            return true;
        }

        // Normal Oyun Akışı
        float cellCenterX = (col * cellSize) + (cellSize / 2);
        float cellCenterY = (row * cellSize) + (cellSize / 2);
        float distanceToCenter = (float) Math.hypot(x - cellCenterX, y - cellCenterY);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clearSelection();
                touchedCell.isSelected = true;
                currentPath.add(touchedCell);
                break;
            case MotionEvent.ACTION_MOVE:
                if (distanceToCenter < cellSize * 0.45f) continueSelection(touchedCell);
                break;
            case MotionEvent.ACTION_UP:
                finishSelection();
                break;
        }
        invalidate();
        return true;
    }

    // --- JOKER YÖNETİMİ ---

    public void enableJokerMode(JokerMode mode) {
        this.currentJokerMode = mode;
        this.swapTarget1 = null;
        invalidate();
    }

    private void handleJokerTouch(GridCell cell) {
        if (currentJokerMode == JokerMode.LOLLIPOP) {
            currentJokerMode = JokerMode.NONE;
            List<GridCell> exp = new ArrayList<>();
            exp.add(cell);
            startExplosionAnimation(exp);
            if (gameListener != null) gameListener.onJokerUsed("Lolipop Kırıcı");

        } else if (currentJokerMode == JokerMode.WHEEL) {
            currentJokerMode = JokerMode.NONE;
            List<GridCell> exp = new ArrayList<>();
            for (int i = 0; i < gridSize; i++) {
                exp.add(cells[cell.row][i]); // Satır
                if (i != cell.col) exp.add(cells[i][cell.col]); // Sütun
            }
            startExplosionAnimation(exp);
            if (gameListener != null) gameListener.onJokerUsed("Tekerlek");

        } else if (currentJokerMode == JokerMode.SWAP_1) {
            swapTarget1 = cell;
            currentJokerMode = JokerMode.SWAP_2;
            invalidate(); // 1. harfi vurgula

        } else if (currentJokerMode == JokerMode.SWAP_2) {
            if (cell == swapTarget1) {
                currentJokerMode = JokerMode.NONE; // İptal et
                swapTarget1 = null;
                invalidate();
            } else if (Math.abs(cell.row - swapTarget1.row) <= 1 && Math.abs(cell.col - swapTarget1.col) <= 1) {
                currentJokerMode = JokerMode.NONE;
                startSwapAnimation(swapTarget1, cell); // Takas animasyonunu başlat
                if (gameListener != null) gameListener.onJokerUsed("Serbest Değiştirme");
            } else {
                swapTarget1 = cell; // Yeni ilk hedef yap
                invalidate();
            }
        }
    }

    // 🐟 Balık Jokeri (5 Rastgele)
    public void useFishJoker() {
        if (isAnimating) return;
        List<GridCell> exp = new ArrayList<>();
        int count = 0;
        while (count < 5) { // 5 harf patlatır
            int r = random.nextInt(gridSize);
            int c = random.nextInt(gridSize);
            if (!exp.contains(cells[r][c])) {
                exp.add(cells[r][c]);
                count++;
            }
        }
        startExplosionAnimation(exp);
    }

    // 🎉 Parti Güçlendirici (Tüm Tahta)
    public void usePartyBooster() {
        if (isAnimating) return;
        List<GridCell> exp = new ArrayList<>();
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                exp.add(cells[r][c]);
            }
        }
        startExplosionAnimation(exp);
    }

    // 🔄 Takas Animasyonu
    private void startSwapAnimation(GridCell cell1, GridCell cell2) {
        isSwapping = true;
        swapTarget1 = cell1;
        swapTarget2 = cell2;

        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f, 1f);
        animator.setDuration(400);
        final boolean[] swapped = {false};

        animator.addUpdateListener(animation -> {
            swapProgress = (float) animation.getAnimatedValue();
            if (swapProgress < 0.1f && animation.getCurrentPlayTime() > 150 && !swapped[0]) {
                // Harfler küçücükken verileri takas et
                char tempL = cell1.letter; GridCell.SpecialPower tempP = cell1.power;
                cell1.letter = cell2.letter; cell1.power = cell2.power;
                cell2.letter = tempL; cell2.power = tempP;
                swapped[0] = true;
            }
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isSwapping = false;
                swapTarget1 = null; swapTarget2 = null;
                if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
            }
        });
        animator.start();
    }

    // --- ESKİ MANTIK VE ANİMASYONLAR (Değişmedi) ---

    private void continueSelection(GridCell cell) {
        if (currentPath.isEmpty()) return;
        GridCell lastCell = currentPath.get(currentPath.size() - 1);
        if (currentPath.size() > 1 && cell == currentPath.get(currentPath.size() - 2)) {
            lastCell.isSelected = false;
            currentPath.remove(currentPath.size() - 1);
            return;
        }
        if (!cell.isSelected) {
            if (Math.abs(cell.row - lastCell.row) <= 1 && Math.abs(cell.col - lastCell.col) <= 1) {
                cell.isSelected = true;
                currentPath.add(cell);
            }
        }
    }

    private void finishSelection() {
        if (currentPath.size() < 3) {
            clearSelection();
            if (gameListener != null) gameListener.onInvalidWord();
            return;
        }

        StringBuilder wordBuilder = new StringBuilder();
        for (GridCell cell : currentPath) wordBuilder.append(cell.letter);
        String word = wordBuilder.toString();

        if (dictionaryManager.isWordValid(word)) {
            int score = ScoreCalculator.calculateComboScore(word, dictionaryManager);
            GridCell lastCell = currentPath.get(currentPath.size() - 1);

            List<GridCell> triggeredPowerCells = new ArrayList<>();
            boolean[][] cellsToClear = new boolean[gridSize][gridSize];

            GridCell.SpecialPower powerToLeave = GridCell.SpecialPower.NONE;
            if (word.length() == 4) powerToLeave = GridCell.SpecialPower.ROW_CLEAR;
            else if (word.length() == 5) powerToLeave = GridCell.SpecialPower.AREA_EXPLODE;
            else if (word.length() == 6) powerToLeave = GridCell.SpecialPower.COLUMN_CLEAR;
            else if (word.length() >= 7) powerToLeave = GridCell.SpecialPower.MEGA_EXPLODE;

            for (GridCell cell : currentPath) {
                cellsToClear[cell.row][cell.col] = true;
                if (cell.power != GridCell.SpecialPower.NONE) {
                    triggeredPowerCells.add(cell);
                }
            }

            for (GridCell cell : triggeredPowerCells) {
                markPowerAffectedCells(cell.power, cell.row, cell.col, cellsToClear);
            }

            List<GridCell> cellsToExplode = new ArrayList<>();
            for (int r = 0; r < gridSize; r++) {
                for (int c = 0; c < gridSize; c++) {
                    if (cellsToClear[r][c] && cells[r][c].letter != ' ') {
                        cellsToExplode.add(cells[r][c]);
                    }
                }
            }

            if (powerToLeave != GridCell.SpecialPower.NONE) {
                cellsToExplode.remove(lastCell);
                lastCell.power = powerToLeave;
            }

            if (gameListener != null) gameListener.onWordFound(score, word);
            clearSelection();
            startExplosionAnimation(cellsToExplode);
        } else {
            if (gameListener != null) gameListener.onInvalidWord();
            clearSelection();
        }
    }

    private void startExplosionAnimation(List<GridCell> cellsToExplode) {
        if (cellsToExplode.isEmpty()) return;
        isAnimating = true;
        explodingCells.clear();
        explodingCells.addAll(cellsToExplode);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(450);
        animator.addUpdateListener(animation -> {
            explosionProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
                for (GridCell cell : explodingCells) {
                    cell.letter = ' ';
                    cell.power = GridCell.SpecialPower.NONE;
                }
                applyGravity();
                if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
            }
        });
        animator.start();
    }

    public void shuffleBoard() {
        if (cells == null || isShuffling) return;
        isShuffling = true;
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f, 1f);
        animator.setDuration(600);
        animator.addUpdateListener(animation -> {
            shuffleProgress = (float) animation.getAnimatedValue();
            if (shuffleProgress <= 0.05f && animation.getCurrentPlayTime() > 250) {
                List<Character> letters = new ArrayList<>();
                List<GridCell.SpecialPower> powers = new ArrayList<>();
                for (int r = 0; r < gridSize; r++) {
                    for (int c = 0; c < gridSize; c++) {
                        letters.add(cells[r][c].letter);
                        powers.add(cells[r][c].power);
                    }
                }
                Collections.shuffle(letters, random); Collections.shuffle(powers, random);
                int index = 0;
                for (int r = 0; r < gridSize; r++) {
                    for (int c = 0; c < gridSize; c++) {
                        cells[r][c].letter = letters.get(index);
                        cells[r][c].power = powers.get(index);
                        index++;
                    }
                }
                clearSelection();
            }
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isShuffling = false;
                if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
            }
        });
        animator.start();
    }

    private void markPowerAffectedCells(GridCell.SpecialPower power, int centerRow, int centerCol, boolean[][] cellsToClear) {
        switch (power) {
            case ROW_CLEAR: for (int c = 0; c < gridSize; c++) cellsToClear[centerRow][c] = true; break;
            case COLUMN_CLEAR: for (int r = 0; r < gridSize; r++) cellsToClear[r][centerCol] = true; break;
            case AREA_EXPLODE: markSquare(centerRow, centerCol, 1, cellsToClear); break;
            case MEGA_EXPLODE: markSquare(centerRow, centerCol, 2, cellsToClear); break;
            default: break;
        }
    }

    private void markSquare(int centerRow, int centerCol, int radius, boolean[][] cellsToClear) {
        for (int r = Math.max(0, centerRow - radius); r <= Math.min(gridSize - 1, centerRow + radius); r++) {
            for (int c = Math.max(0, centerCol - radius); c <= Math.min(gridSize - 1, centerCol + radius); c++) {
                cellsToClear[r][c] = true;
            }
        }
    }

    private void applyGravity() {
        for (int c = 0; c < gridSize; c++) {
            int emptyRow = gridSize - 1;
            for (int r = gridSize - 1; r >= 0; r--) {
                if (cells[r][c].letter != ' ') {
                    if (r != emptyRow) {
                        cells[emptyRow][c].letter = cells[r][c].letter;
                        cells[emptyRow][c].power = cells[r][c].power;
                        cells[r][c].letter = ' ';
                        cells[r][c].power = GridCell.SpecialPower.NONE;
                    }
                    emptyRow--;
                }
            }
            for (int r = emptyRow; r >= 0; r--) {
                cells[r][c].letter = LetterGenerator.getRandomLetter();
                cells[r][c].power = GridCell.SpecialPower.NONE;
            }
        }
        invalidate();
    }

    private void clearSelection() {
        for (GridCell cell : currentPath) cell.isSelected = false;
        currentPath.clear();
        invalidate();
    }

    private int calculateAvailableWords() {
        HashSet<String> foundWords = new HashSet<>();
        boolean[][] visited = new boolean[gridSize][gridSize];
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                findWordsDFS(r, c, "", visited, foundWords);
            }
        }
        return foundWords.size();
    }

    private void findWordsDFS(int r, int c, String currentWord, boolean[][] visited, HashSet<String> foundWords) {
        currentWord += cells[r][c].letter;
        if (!dictionaryManager.isPrefixValid(currentWord)) return;
        visited[r][c] = true;
        if (currentWord.length() >= 3 && dictionaryManager.isWordValid(currentWord)) {
            foundWords.add(currentWord);
        }
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < gridSize && nc >= 0 && nc < gridSize && !visited[nr][nc]) {
                    findWordsDFS(nr, nc, currentWord, visited, foundWords);
                }
            }
        }
        visited[r][c] = false;
    }
}