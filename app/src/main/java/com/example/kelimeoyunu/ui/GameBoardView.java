package com.example.kelimeoyunu.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;

import com.example.kelimeoyunu.models.GridCell;
import com.example.kelimeoyunu.utils.DictionaryManager;
import com.example.kelimeoyunu.utils.LetterGenerator;
import com.example.kelimeoyunu.utils.ScoreCalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameBoardView extends View {

    private int gridSize = 8;
    private GridCell[][] cells;
    private List<GridCell> currentPath = new ArrayList<>();

    private int[][] dropOffsets;

    private Paint cellPaint, textPaint, selectedPaint, powerPaint, linePaint, particlePaint, effectPaint, floatingTextPaint;
    private Paint errorCellPaint;
    private float cellSize;

    private DictionaryManager dictionaryManager;
    private GameListener gameListener;
    private Random random = new Random();

    // ANİMASYON VE EFEKT DURUMLARI
    private boolean isAnimating = false;
    private float explosionProgress = 0f;
    private List<GridCell> explodingCells = new ArrayList<>();

    // Harf Karıştırma Jokeri Animasyonu
    private boolean isShuffling = false;
    private float shuffleProgress = 0f;

    // Hata Titremesi
    private boolean isErrorShaking = false;
    private float errorShakeProgress = 0f;

    // Özel Efektler
    private List<Particle> particles = new ArrayList<>();
    private List<FloatingText> floatingTexts = new ArrayList<>();
    private List<Effect> activeEffects = new ArrayList<>();
    private float cameraShakeOffsetX = 0f;
    private float cameraShakeOffsetY = 0f;

    // Yerçekimi / Düşme
    private boolean isGravityFalling = false;
    private float gravityProgress = 1f;

    // JOKER MODLARI
    public enum JokerMode { NONE, LOLLIPOP, WHEEL, SWAP_1, SWAP_2 }
    private JokerMode currentJokerMode = JokerMode.NONE;
    private GridCell swapTarget1 = null;
    private GridCell swapTarget2 = null;

    private boolean isSwapping = false;
    private float swapProgress = 1f;

    private boolean isFishEating = false;
    private float fishProgress = 0f;
    private float fishX = -100f, fishY = -100f;

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

        errorCellPaint = new Paint(); errorCellPaint.setColor(Color.parseColor("#FF5252")); errorCellPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(); textPaint.setColor(Color.BLACK); textPaint.setTextSize(60f); textPaint.setTextAlign(Paint.Align.CENTER); textPaint.setAntiAlias(true);
        powerPaint = new Paint(); powerPaint.setColor(Color.RED); powerPaint.setTextSize(40f); powerPaint.setTextAlign(Paint.Align.RIGHT);
        linePaint = new Paint(); linePaint.setColor(Color.parseColor("#80FFCA28")); linePaint.setStrokeWidth(15f); linePaint.setStrokeJoin(Paint.Join.ROUND); linePaint.setStyle(Paint.Style.STROKE);

        particlePaint = new Paint(); particlePaint.setStyle(Paint.Style.FILL);
        effectPaint = new Paint(); effectPaint.setStyle(Paint.Style.FILL); effectPaint.setAntiAlias(true);

        floatingTextPaint = new Paint(); floatingTextPaint.setColor(Color.parseColor("#4CAF50"));
        floatingTextPaint.setTextSize(80f); floatingTextPaint.setTextAlign(Paint.Align.CENTER);
        floatingTextPaint.setFakeBoldText(true); floatingTextPaint.setShadowLayer(5, 2, 2, Color.BLACK);
    }

    public void setupGame(int size, DictionaryManager dict, GameListener listener) {
        this.gridSize = size;
        this.dictionaryManager = dict;
        this.gameListener = listener;
        this.cells = new GridCell[size][size];
        this.dropOffsets = new int[size][size];

        boolean isGridPlayable = false;
        int wordCount = 0;

        while (!isGridPlayable) {
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    cells[r][c] = new GridCell(' ', r, c);
                }
            }

            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    if (cells[r][c].letter == ' ') cells[r][c].letter = LetterGenerator.getRandomLetter();
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

        canvas.save();
        canvas.translate(cameraShakeOffsetX, cameraShakeOffsetY);

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                GridCell cell = cells[r][c];

                boolean isExploding = isAnimating && explodingCells.contains(cell);
                boolean isCellSwapping = isSwapping && (cell == swapTarget1 || cell == swapTarget2);

                float currentCellSize = cellSize;
                int currentAlpha = 255;
                float left = c * cellSize;
                float top = r * cellSize;

                float shakeOffsetX = 0;
                if (isErrorShaking && currentPath.contains(cell)) {
                    shakeOffsetX = (float) Math.sin(errorShakeProgress * Math.PI * 6) * 15f;
                    left += shakeOffsetX;
                }

                if (isCellSwapping) {
                    float cx1 = swapTarget1.col * cellSize;
                    float cy1 = swapTarget1.row * cellSize;
                    float cx2 = swapTarget2.col * cellSize;
                    float cy2 = swapTarget2.row * cellSize;

                    float midX = (cx1 + cx2) / 2f;
                    float midY = (cy1 + cy2) / 2f;
                    float radius = (float) Math.hypot(cx1 - cx2, cy1 - cy2) / 2f;

                    float angleOffset = (float) Math.PI * swapProgress;
                    if (cell == swapTarget1) {
                        left = midX + radius * (float)Math.cos(angleOffset) - cellSize/2f + cellSize/2f;
                        top = midY + radius * (float)Math.sin(angleOffset) - cellSize/2f + cellSize/2f;
                    } else {
                        left = midX + radius * (float)Math.cos(angleOffset + Math.PI) - cellSize/2f + cellSize/2f;
                        top = midY + radius * (float)Math.sin(angleOffset + Math.PI) - cellSize/2f + cellSize/2f;
                    }
                }
                else if (isGravityFalling && dropOffsets[r][c] > 0) {
                    float targetTop = r * cellSize;
                    float startTop = targetTop - (dropOffsets[r][c] * cellSize);
                    top = startTop + (targetTop - startTop) * gravityProgress;
                }
                else if (isExploding) {
                    currentCellSize = cellSize * (1f - explosionProgress);
                    currentAlpha = (int) (255 * (1f - explosionProgress));
                }
                else if (isShuffling) {
                    float scale = shuffleProgress < 0.5f
                            ? 1f - (shuffleProgress * 2f)
                            : (shuffleProgress - 0.5f) * 2f;
                    currentCellSize = cellSize * scale;
                    currentAlpha = (int) (255 * scale);
                }

                float inset = (cellSize - currentCellSize) / 2;
                left += inset; top += inset;

                cellPaint.setAlpha(currentAlpha); textPaint.setAlpha(currentAlpha); powerPaint.setAlpha(currentAlpha);

                Paint bgPaint = cellPaint;
                if (isErrorShaking && currentPath.contains(cell)) bgPaint = errorCellPaint;
                else if (cell.isSelected || cell == swapTarget1) bgPaint = selectedPaint;
                else if (currentJokerMode == JokerMode.LOLLIPOP) cellPaint.setColor(Color.parseColor("#FFF3E0"));
                else if (currentJokerMode == JokerMode.WHEEL) cellPaint.setColor(Color.parseColor("#FCE4EC")); // Hafif pembe hedef rengi
                else if (currentJokerMode == JokerMode.SWAP_1 || currentJokerMode == JokerMode.SWAP_2) cellPaint.setColor(Color.parseColor("#E3F2FD"));
                else cellPaint.setColor(Color.WHITE);

                canvas.drawRoundRect(new RectF(left + 2, top + 2, left + currentCellSize - 2, top + currentCellSize - 2), 15, 15, bgPaint);

                if (bgPaint == errorCellPaint) textPaint.setColor(Color.WHITE);
                else textPaint.setColor(Color.BLACK);

                float textY = top + (currentCellSize / 2) - ((textPaint.descent() + textPaint.ascent()) / 2);
                canvas.drawText(String.valueOf(cell.letter), left + (currentCellSize / 2), textY, textPaint);

                if (cell.power != GridCell.SpecialPower.NONE) {
                    canvas.drawText(getPowerSymbol(cell.power), left + currentCellSize - 10, top + 40 * (currentCellSize/cellSize), powerPaint);
                }

                cellPaint.setAlpha(255); textPaint.setAlpha(255); powerPaint.setAlpha(255);
            }
        }

        if (currentPath.size() > 1 && currentJokerMode == JokerMode.NONE && !isErrorShaking) {
            for (int i = 0; i < currentPath.size() - 1; i++) {
                GridCell start = currentPath.get(i), end = currentPath.get(i + 1);
                if (isErrorShaking) linePaint.setColor(Color.parseColor("#80FF5252"));
                else linePaint.setColor(Color.parseColor("#80FFCA28"));

                canvas.drawLine(start.col * cellSize + (cellSize / 2), start.row * cellSize + (cellSize / 2),
                        end.col * cellSize + (cellSize / 2), end.row * cellSize + (cellSize / 2), linePaint);
            }
        }

        Iterator<Particle> pIter = particles.iterator();
        while (pIter.hasNext()) {
            Particle p = pIter.next();
            p.update();
            particlePaint.setColor(p.color);
            particlePaint.setAlpha((int)(255 * p.life));
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint);
            if (p.life <= 0) pIter.remove();
        }

        Iterator<Effect> eIter = activeEffects.iterator();
        while (eIter.hasNext()) {
            Effect e = eIter.next();
            e.update();
            effectPaint.setColor(e.color);
            effectPaint.setAlpha((int)(255 * e.life));
            if (e.type == EffectType.LASER_H) {
                canvas.drawRect(0, e.y - 10, getWidth(), e.y + 10, effectPaint);
            } else if (e.type == EffectType.LASER_V) {
                canvas.drawRect(e.x - 10, 0, e.x + 10, getHeight(), effectPaint);
            } else if (e.type == EffectType.SHOCKWAVE) {
                effectPaint.setStyle(Paint.Style.STROKE);
                effectPaint.setStrokeWidth(10f);
                canvas.drawCircle(e.x, e.y, e.radius, effectPaint);
                effectPaint.setStyle(Paint.Style.FILL);
            }
            if (e.life <= 0) eIter.remove();
        }

        if (isFishEating) {
            Paint fishPaint = new Paint(); fishPaint.setColor(Color.parseColor("#FF9800"));
            canvas.drawCircle(fishX, fishY, 40, fishPaint);
        }

        Iterator<FloatingText> tIter = floatingTexts.iterator();
        while (tIter.hasNext()) {
            FloatingText ft = tIter.next();
            ft.update();
            floatingTextPaint.setAlpha((int)(255 * ft.life));
            canvas.drawText(ft.text, ft.x, ft.y, floatingTextPaint);
            if (ft.life <= 0) tIter.remove();
        }

        canvas.restore();

        if (!particles.isEmpty() || !floatingTexts.isEmpty() || !activeEffects.isEmpty() || isShuffling) {
            invalidate();
        }
    }

    private String getPowerSymbol(GridCell.SpecialPower power) {
        switch (power) { case ROW_CLEAR: return "⇆"; case AREA_EXPLODE: return "✹"; case COLUMN_CLEAR: return "⇅"; case MEGA_EXPLODE: return "✪"; default: return ""; }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cells == null || isAnimating || isShuffling || isSwapping || isGravityFalling || isErrorShaking) return false;

        float x = event.getX(), y = event.getY();
        int col = (int) (x / cellSize), row = (int) (y / cellSize);
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) return true;

        GridCell touchedCell = cells[row][col];

        if (currentJokerMode != JokerMode.NONE) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) handleJokerTouch(touchedCell);
            return true;
        }

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

    public void enableJokerMode(JokerMode mode) {
        this.currentJokerMode = mode;
        this.swapTarget1 = null;
        invalidate();
    }

    private void handleJokerTouch(GridCell cell) {
        if (currentJokerMode == JokerMode.LOLLIPOP) {
            currentJokerMode = JokerMode.NONE;
            List<GridCell> exp = new ArrayList<>(); exp.add(cell);
            spawnParticles(cell.col * cellSize + cellSize/2, cell.row * cellSize + cellSize/2, 20);
            startExplosionAnimation(exp);
            if (gameListener != null) gameListener.onJokerUsed("Lolipop Kırıcı");

        } else if (currentJokerMode == JokerMode.WHEEL) {
            currentJokerMode = JokerMode.NONE;

            // Tabloya göre satır ve sütundaki tüm harfler yok edilecek
            List<GridCell> exp = new ArrayList<>();
            for(int i = 0; i < gridSize; i++) {
                if(!exp.contains(cells[cell.row][i])) exp.add(cells[cell.row][i]);
                if(!exp.contains(cells[i][cell.col])) exp.add(cells[i][cell.col]);
            }

            float cx = cell.col * cellSize + cellSize/2;
            float cy = cell.row * cellSize + cellSize/2;

            // Pembe renkli çarpı (lazer) animasyonu
            activeEffects.add(new Effect(EffectType.LASER_H, cx, cy, Color.parseColor("#E91E63")));
            activeEffects.add(new Effect(EffectType.LASER_V, cx, cy, Color.parseColor("#E91E63")));
            spawnParticles(cx, cy, 30);
            startCameraShake(15f);

            startExplosionAnimation(exp);
            if (gameListener != null) gameListener.onJokerUsed("Tekerlek");

        } else if (currentJokerMode == JokerMode.SWAP_1) {
            swapTarget1 = cell;
            currentJokerMode = JokerMode.SWAP_2;
            invalidate();

        } else if (currentJokerMode == JokerMode.SWAP_2) {
            if (cell == swapTarget1) {
                currentJokerMode = JokerMode.NONE; swapTarget1 = null; invalidate();
            } else if (Math.abs(cell.row - swapTarget1.row) <= 1 && Math.abs(cell.col - swapTarget1.col) <= 1) {
                currentJokerMode = JokerMode.NONE;
                startSwapAnimation(swapTarget1, cell);
                if (gameListener != null) gameListener.onJokerUsed("Serbest Değiştirme");
            } else {
                swapTarget1 = cell; invalidate();
            }
        }
    }

    // --- JOKER VE ANİMASYON METOTLARI ---

    // HARF KARIŞTIRMA JOKERİ
    public void shuffleBoard() {
        if (cells == null || isShuffling) return;
        isShuffling = true;
        final boolean[] isDataShuffled = {false};

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600);
        animator.addUpdateListener(animation -> {
            shuffleProgress = (float) animation.getAnimatedValue();

            if (shuffleProgress >= 0.5f && !isDataShuffled[0]) {
                isDataShuffled[0] = true;
                List<Character> letters = new ArrayList<>();
                List<GridCell.SpecialPower> powers = new ArrayList<>();
                for (int r = 0; r < gridSize; r++) {
                    for (int c = 0; c < gridSize; c++) {
                        letters.add(cells[r][c].letter); powers.add(cells[r][c].power);
                    }
                }
                Collections.shuffle(letters, random); Collections.shuffle(powers, random);
                int index = 0;
                for (int r = 0; r < gridSize; r++) {
                    for (int c = 0; c < gridSize; c++) {
                        cells[r][c].letter = letters.get(index);
                        cells[r][c].power = powers.get(index); index++;
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
                shuffleProgress = 0f;
                if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
            }
        });
        animator.start();
    }

    public void useFishJoker() {
        if (isAnimating) return;
        List<GridCell> exp = new ArrayList<>();
        int count = 0;
        while (count < 5) {
            int r = random.nextInt(gridSize); int c = random.nextInt(gridSize);
            if (!exp.contains(cells[r][c])) { exp.add(cells[r][c]); count++; }
        }
        startFishAnimation(exp);
    }

    // PARTİ GÜÇLENDİRİCİSİ (Tabloya göre tüm haritayı sıfırlar)
    public void usePartyBooster() {
        if (cells == null || isAnimating) return;

        List<GridCell> exp = new ArrayList<>();
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                if (cells[r][c].letter != ' ') {
                    exp.add(cells[r][c]);
                }
            }
        }

        // Bolca kutlama efekti
        for (int i = 0; i < 8; i++) {
            spawnParticles(random.nextInt(getWidth()), random.nextInt(getHeight()), 20);
        }
        startCameraShake(25f);

        startExplosionAnimation(exp);

        if (gameListener != null) gameListener.onJokerUsed("Parti Güçlendiricisi");
    }

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
                cell.isSelected = true; currentPath.add(cell);
            }
        }
    }

    private void finishSelection() {
        if (currentPath.size() < 3) {
            startErrorShakeAnimation();
            return;
        }

        StringBuilder wordBuilder = new StringBuilder();
        for (GridCell cell : currentPath) wordBuilder.append(cell.letter);
        String word = wordBuilder.toString();

        if (dictionaryManager.isWordValid(word)) {
            int score = ScoreCalculator.calculateComboScore(word, dictionaryManager);

            GridCell lastCell = currentPath.get(currentPath.size() - 1);
            floatingTexts.add(new FloatingText("+" + score + (score > 10 ? " KOMBO!" : ""),
                    lastCell.col * cellSize + cellSize/2, lastCell.row * cellSize));

            List<GridCell> triggeredPowerCells = new ArrayList<>();
            boolean[][] cellsToClear = new boolean[gridSize][gridSize];

            GridCell.SpecialPower powerToLeave = GridCell.SpecialPower.NONE;
            if (word.length() == 4) powerToLeave = GridCell.SpecialPower.ROW_CLEAR;
            else if (word.length() == 5) powerToLeave = GridCell.SpecialPower.AREA_EXPLODE;
            else if (word.length() == 6) powerToLeave = GridCell.SpecialPower.COLUMN_CLEAR;
            else if (word.length() >= 7) powerToLeave = GridCell.SpecialPower.MEGA_EXPLODE;

            for (GridCell cell : currentPath) {
                cellsToClear[cell.row][cell.col] = true;
                if (cell.power != GridCell.SpecialPower.NONE) triggeredPowerCells.add(cell);
                spawnParticles(cell.col * cellSize + cellSize/2, cell.row * cellSize + cellSize/2, 10);
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
            startErrorShakeAnimation();
        }
    }

    private void startErrorShakeAnimation() {
        isErrorShaking = true;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(400);
        animator.addUpdateListener(animation -> {
            errorShakeProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isErrorShaking = false;
                clearSelection();
                if (gameListener != null) gameListener.onInvalidWord();
            }
        });
        animator.start();
    }

    private void startExplosionAnimation(List<GridCell> cellsToExplode) {
        if (cellsToExplode.isEmpty()) return;
        isAnimating = true;
        explodingCells.clear();
        explodingCells.addAll(cellsToExplode);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(400);
        animator.addUpdateListener(animation -> {
            explosionProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
                for (GridCell cell : explodingCells) {
                    cell.letter = ' '; cell.power = GridCell.SpecialPower.NONE;
                }
                startGravityAnimation();
            }
        });
        animator.start();
    }

    private void startGravityAnimation() {
        applyGravityLogic();
        isGravityFalling = true;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(500);

        animator.setInterpolator(new BounceInterpolator());

        animator.addUpdateListener(animation -> {
            gravityProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isGravityFalling = false;
                if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
            }
        });
        animator.start();
    }

    private void startFishAnimation(List<GridCell> exp) {
        isFishEating = true;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1500);
        animator.addUpdateListener(animation -> {
            fishProgress = (float) animation.getAnimatedValue();
            fishX = getWidth() * fishProgress;
            fishY = getHeight() / 2f + (float)Math.sin(fishProgress * Math.PI * 4) * 100f;
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isFishEating = false;
                startExplosionAnimation(exp);
            }
        });
        animator.start();
    }

    private void startSwapAnimation(GridCell cell1, GridCell cell2) {
        isSwapping = true; swapTarget1 = cell1; swapTarget2 = cell2;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(500);
        final boolean[] swapped = {false};

        animator.addUpdateListener(animation -> {
            swapProgress = (float) animation.getAnimatedValue();
            if (swapProgress >= 0.5f && !swapped[0]) {
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
                isSwapping = false; swapTarget1 = null; swapTarget2 = null;
                if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
            }
        });
        animator.start();
    }

    private void markPowerAffectedCells(GridCell.SpecialPower power, int centerRow, int centerCol, boolean[][] cellsToClear) {
        float cx = centerCol * cellSize + cellSize/2;
        float cy = centerRow * cellSize + cellSize/2;

        switch (power) {
            case ROW_CLEAR:
                for (int c = 0; c < gridSize; c++) cellsToClear[centerRow][c] = true;
                activeEffects.add(new Effect(EffectType.LASER_H, cx, cy, Color.CYAN));
                break;
            case COLUMN_CLEAR:
                for (int r = 0; r < gridSize; r++) cellsToClear[r][centerCol] = true;
                activeEffects.add(new Effect(EffectType.LASER_V, cx, cy, Color.MAGENTA));
                break;
            case AREA_EXPLODE:
                markSquare(centerRow, centerCol, 1, cellsToClear);
                activeEffects.add(new Effect(EffectType.SHOCKWAVE, cx, cy, Color.YELLOW));
                startCameraShake(15f);
                break;
            case MEGA_EXPLODE:
                markSquare(centerRow, centerCol, 2, cellsToClear);
                activeEffects.add(new Effect(EffectType.SHOCKWAVE, cx, cy, Color.RED));
                startCameraShake(30f);
                break;
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

    private void applyGravityLogic() {
        dropOffsets = new int[gridSize][gridSize];

        for (int c = 0; c < gridSize; c++) {
            int emptySpacesCount = 0;

            int emptyRow = gridSize - 1;
            for (int r = gridSize - 1; r >= 0; r--) {
                if (cells[r][c].letter != ' ') {
                    if (r != emptyRow) {
                        cells[emptyRow][c].letter = cells[r][c].letter;
                        cells[emptyRow][c].power = cells[r][c].power;
                        cells[r][c].letter = ' ';
                        cells[r][c].power = GridCell.SpecialPower.NONE;

                        dropOffsets[emptyRow][c] = emptyRow - r;
                    }
                    emptyRow--;
                } else {
                    emptySpacesCount++;
                }
            }

            for (int r = emptyRow; r >= 0; r--) {
                cells[r][c].letter = LetterGenerator.getRandomLetter();
                cells[r][c].power = GridCell.SpecialPower.NONE;
                dropOffsets[r][c] = emptySpacesCount;
            }
        }
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

    private void spawnParticles(float x, float y, int count) {
        int[] colors = {Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA};
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, colors[random.nextInt(colors.length)]));
        }
    }

    private void startCameraShake(float intensity) {
        ValueAnimator animator = ValueAnimator.ofFloat(intensity, 0f);
        animator.setDuration(400);
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            cameraShakeOffsetX = (random.nextFloat() - 0.5f) * val * 2;
            cameraShakeOffsetY = (random.nextFloat() - 0.5f) * val * 2;
            invalidate();
        });
        animator.start();
    }

    private class Particle {
        float x, y, vx, vy, life = 1f, radius;
        int color;
        Particle(float x, float y, int color) {
            this.x = x; this.y = y; this.color = color;
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = random.nextDouble() * 15 + 5;
            vx = (float) (Math.cos(angle) * speed);
            vy = (float) (Math.sin(angle) * speed);
            radius = random.nextFloat() * 8 + 4;
        }
        void update() {
            x += vx; y += vy;
            vy += 0.5f;
            life -= 0.03f;
        }
    }

    private class FloatingText {
        float x, y, life = 1f; String text;
        FloatingText(String text, float x, float y) { this.text = text; this.x = x; this.y = y; }
        void update() { y -= 2f; life -= 0.02f; }
    }

    enum EffectType { LASER_H, LASER_V, SHOCKWAVE }
    private class Effect {
        EffectType type; float x, y, life = 1f, radius = 0f; int color;
        Effect(EffectType type, float x, float y, int color) {
            this.type = type; this.x = x; this.y = y; this.color = color;
        }
        void update() {
            life -= 0.05f;
            if (type == EffectType.SHOCKWAVE) radius += 20f;
        }
    }
}