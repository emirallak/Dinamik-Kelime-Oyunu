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

    public interface GameListener {
        void onWordFound(int scoreToAdd, String word);
        void onInvalidWord();
        void onGridAnalyzed(int availableWordCount);
    }

    public GameBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cellPaint = new Paint(); cellPaint.setColor(Color.WHITE); cellPaint.setStyle(Paint.Style.FILL);
        selectedPaint = new Paint(); selectedPaint.setColor(Color.parseColor("#FFCA28")); selectedPaint.setStyle(Paint.Style.FILL);
        textPaint = new Paint(); textPaint.setColor(Color.BLACK); textPaint.setTextSize(60f); textPaint.setTextAlign(Paint.Align.CENTER); textPaint.setAntiAlias(true);
        powerPaint = new Paint(); powerPaint.setColor(Color.RED); powerPaint.setTextSize(30f); powerPaint.setTextAlign(Paint.Align.RIGHT);
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

            injectGuaranteedWord(dictionaryManager.getRandomLongWord());

            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (cells[r][c].letter == ' ') {
                        cells[r][c].letter = LetterGenerator.getRandomLetter();
                    }
                }
            }

            wordCount = calculateAvailableWords();
            if (wordCount >= 3) {
                isGridPlayable = true;
            }
        }

        if (gameListener != null) gameListener.onGridAnalyzed(wordCount);
        invalidate();
    }

    private void injectGuaranteedWord(String word) {
        int len = word.length();
        boolean horizontal = random.nextBoolean();

        if (horizontal && gridSize >= len) {
            int r = random.nextInt(gridSize);
            int c = random.nextInt(gridSize - len + 1);
            for (int i = 0; i < len; i++) cells[r][c + i].letter = word.charAt(i);
        } else if (!horizontal && gridSize >= len) {
            int r = random.nextInt(gridSize - len + 1);
            int c = random.nextInt(gridSize);
            for (int i = 0; i < len; i++) cells[r + i][c].letter = word.charAt(i);
        }
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cells == null) return;
        cellSize = (float) getWidth() / gridSize;

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                GridCell cell = cells[r][c];
                float left = c * cellSize, top = r * cellSize;
                canvas.drawRect(left + 2, top + 2, left + cellSize - 2, top + cellSize - 2, cell.isSelected ? selectedPaint : cellPaint);
                float textY = top + (cellSize / 2) - ((textPaint.descent() + textPaint.ascent()) / 2);
                canvas.drawText(String.valueOf(cell.letter), left + (cellSize / 2), textY, textPaint);
                if (cell.power != GridCell.SpecialPower.NONE) {
                    canvas.drawText(getPowerSymbol(cell.power), left + cellSize - 10, top + 40, powerPaint);
                }
            }
        }

        if (currentPath.size() > 1) {
            for (int i = 0; i < currentPath.size() - 1; i++) {
                GridCell start = currentPath.get(i), end = currentPath.get(i + 1);
                canvas.drawLine(start.col * cellSize + (cellSize / 2), start.row * cellSize + (cellSize / 2),
                        end.col * cellSize + (cellSize / 2), end.row * cellSize + (cellSize / 2), linePaint);
            }
        }
    }

    private String getPowerSymbol(GridCell.SpecialPower power) {
        switch (power) { case ROW_CLEAR: return "↔"; case AREA_EXPLODE: return "✱"; case COLUMN_CLEAR: return "↕"; case MEGA_EXPLODE: return "✪"; default: return ""; }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cells == null) return false;
        float x = event.getX(), y = event.getY();
        int col = (int) (x / cellSize), row = (int) (y / cellSize);
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) return true;

        GridCell touchedCell = cells[row][col];

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
                if (distanceToCenter < cellSize * 0.45f) {
                    continueSelection(touchedCell);
                }
                break;
            case MotionEvent.ACTION_UP:
                finishSelection();
                break;
        }
        invalidate();
        return true;
    }

    private void continueSelection(GridCell cell) {
        if (currentPath.isEmpty()) return;

        GridCell lastCell = currentPath.get(currentPath.size() - 1);

        if (currentPath.size() > 1) {
            GridCell previousCell = currentPath.get(currentPath.size() - 2);
            if (cell == previousCell) {
                lastCell.isSelected = false;
                currentPath.remove(currentPath.size() - 1);
                return;
            }
        }

        if (!cell.isSelected) {
            int rowDiff = Math.abs(cell.row - lastCell.row);
            int colDiff = Math.abs(cell.col - lastCell.col);

            if (rowDiff <= 1 && colDiff <= 1) {
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
            GridCell.SpecialPower powerToLeave = GridCell.SpecialPower.NONE;
            List<GridCell> triggeredPowerCells = new ArrayList<>();
            boolean[][] selectedCells = new boolean[gridSize][gridSize];
            boolean[][] cellsToClear = new boolean[gridSize][gridSize];

            if (word.length() == 4) powerToLeave = GridCell.SpecialPower.ROW_CLEAR;
            else if (word.length() == 5) powerToLeave = GridCell.SpecialPower.AREA_EXPLODE;
            else if (word.length() == 6) powerToLeave = GridCell.SpecialPower.COLUMN_CLEAR;
            else if (word.length() >= 7) powerToLeave = GridCell.SpecialPower.MEGA_EXPLODE; // <- Hata buradaydı, düzeltildi.

            for (GridCell cell : currentPath) {
                selectedCells[cell.row][cell.col] = true;
                cellsToClear[cell.row][cell.col] = true;
                if (cell.power != GridCell.SpecialPower.NONE) {
                    triggeredPowerCells.add(cell);
                }
            }

            for (GridCell cell : triggeredPowerCells) {
                markPowerAffectedCells(cell.power, cell.row, cell.col, cellsToClear);
            }

            int extraScore = clearMarkedCells(cellsToClear, selectedCells);
            score += extraScore;

            if (powerToLeave != GridCell.SpecialPower.NONE) {
                cells[lastCell.row][lastCell.col].letter = LetterGenerator.getRandomLetter();
                cells[lastCell.row][lastCell.col].power = powerToLeave;
            }

            applyGravity();

            int newWordCount = calculateAvailableWords();
            if (gameListener != null) {
                gameListener.onWordFound(score, word);
                gameListener.onGridAnalyzed(newWordCount);
            }

        } else {
            if (gameListener != null) gameListener.onInvalidWord();
        }
        clearSelection();
    }

    private void markPowerAffectedCells(GridCell.SpecialPower power, int centerRow, int centerCol, boolean[][] cellsToClear) {
        switch (power) {
            case ROW_CLEAR:
                for (int c = 0; c < gridSize; c++) cellsToClear[centerRow][c] = true;
                break;
            case COLUMN_CLEAR:
                for (int r = 0; r < gridSize; r++) cellsToClear[r][centerCol] = true;
                break;
            case AREA_EXPLODE:
                markSquare(centerRow, centerCol, 1, cellsToClear);
                break;
            case MEGA_EXPLODE:
                markSquare(centerRow, centerCol, 2, cellsToClear);
                break;
            default:
                break;
        }
    }

    private void markSquare(int centerRow, int centerCol, int radius, boolean[][] cellsToClear) {
        for (int r = Math.max(0, centerRow - radius); r <= Math.min(gridSize - 1, centerRow + radius); r++) {
            for (int c = Math.max(0, centerCol - radius); c <= Math.min(gridSize - 1, centerCol + radius); c++) {
                cellsToClear[r][c] = true;
            }
        }
    }

    private int clearMarkedCells(boolean[][] cellsToClear, boolean[][] selectedCells) {
        int extraScore = 0;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (!cellsToClear[r][c]) continue;
                if (!selectedCells[r][c] && cells[r][c].letter != ' ') {
                    extraScore += ScoreCalculator.getLetterScore(cells[r][c].letter);
                }
                cells[r][c].letter = ' ';
                cells[r][c].power = GridCell.SpecialPower.NONE;
            }
        }
        return extraScore;
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

    public void shuffleBoard() {
        if (cells == null) return;
        List<Character> letters = new ArrayList<>();
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                letters.add(cells[r][c].letter);
            }
        }
        Collections.shuffle(letters, random);
        int index = 0;
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                cells[r][c].letter = letters.get(index++);
            }
        }
        clearSelection();
        if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
        invalidate();
    }

    public void changeLetterAt(int row, int col) {
        if (cells == null) return;
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) return;
        cells[row][col].letter = LetterGenerator.getRandomLetter();
        clearSelection();
        if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
        invalidate();
    }

    public char getLetterAt(int row, int col) {
        if (cells == null) return ' ';
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) return ' ';
        return cells[row][col].letter;
    }
}
