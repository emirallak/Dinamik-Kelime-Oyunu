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
import android.view.animation.BounceInterpolator;

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

    // ====================== MEVCUT ANİMASYON DURUMLARI ======================
    private boolean isAnimating = false;
    private float explosionProgress = 0f;
    private List<GridCell> explodingCells = new ArrayList<>();

    private boolean isShuffling = false;
    private float shuffleProgress = 1f;
    private float shuffleRotation = 0f; // Vortex dönüşü

    // YENİ JOKER MODLARI
    public enum JokerMode { NONE, LOLLIPOP, WHEEL, SWAP_1, SWAP_2 }
    private JokerMode currentJokerMode = JokerMode.NONE;
    private GridCell swapTarget1 = null;

    // SWAP (Takas) Animasyonu
    private boolean isSwapping = false;
    private float swapProgress = 1f;
    private GridCell swapTarget2 = null;

    // ====================== YENİ ANİMASYON SİSTEMLERİ ======================

    // 1. PARÇACIK (CONFETTI) SİSTEMİ
    private static class Particle {
        float x, y;    // Pozisyon
        float vx, vy;  // Hız
        float size;    // Boyut
        int alpha;     // Şeffaflık (0-255)
        int color;     // Renk
    }
    private final List<Particle> particles = new ArrayList<>();
    private static final int[] CONFETTI_COLORS = {
        Color.parseColor("#FF6B6B"),
        Color.parseColor("#FFE66D"),
        Color.parseColor("#4ECDC4"),
        Color.parseColor("#45B7D1"),
        Color.parseColor("#96CEB4"),
        Color.parseColor("#DDA0DD"),
        Color.parseColor("#FF8C69"),
    };
    private Paint particlePaint;
    private ValueAnimator particleAnimator;

    // 2. TİTREME (SHAKE) VE KIRMIZI FLAŞ
    private float shakeOffset = 0f;
    private float flashAlpha = 0f;
    private final List<GridCell> shakeCells = new ArrayList<>();
    private Paint flashPaint;
    private boolean isShaking = false;

    // 3. DÜŞME + SEKME (FALL / BOUNCE) ANİMASYONU
    private static class FallingCell {
        int col;
        int targetRow;
        float startVisualRow; // Başlangıç görsel satırı (negatif = yeni harf)
    }
    private final List<FallingCell> fallingCells = new ArrayList<>();
    private float fallProgress = 1f;
    private boolean isFalling = false;

    // 4. LAZER EFEKTİ (Satır/Sütun Temizleme)
    private static class LaserAnim {
        boolean isRow;
        int index;       // Hangi satır/sütun
        float progress;  // 0 -> 1
    }
    private final List<LaserAnim> laserAnimations = new ArrayList<>();
    private Paint laserPaint;

    // 5. ŞOK DALGASI (Bomba Patlaması)
    private static class ShockwaveAnim {
        float cx, cy;
        float progress; // 0 -> 1
    }
    private final List<ShockwaveAnim> shockwaveAnimations = new ArrayList<>();
    private Paint shockwavePaint;

    // 6. EKRAN SARSINTISI (Camera Shake)
    private float cameraShakeX = 0f, cameraShakeY = 0f;

    // 7. YÜZEN PUAN METNİ (Floating Score)
    private static class FloatingText {
        String text;
        float x, y;
        float alpha;
        float scale;
    }
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private Paint floatingTextPaint;

    // 8. BALIK ANİMASYONU
    private boolean isFishActive = false;
    private float fishX = 0f, fishY = 0f;
    private final List<GridCell> fishTargets = new ArrayList<>();
    private Paint fishTextPaint;

    // ====================== INTERFACE ======================
    public interface GameListener {
        void onWordFound(int scoreToAdd, String word);
        void onInvalidWord();
        void onGridAnalyzed(int availableWordCount);
        void onJokerUsed(String jokerName);
    }

    // ====================== CONSTRUCTOR ======================
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

        particlePaint = new Paint(); particlePaint.setAntiAlias(true);
        flashPaint = new Paint(); flashPaint.setColor(Color.RED); flashPaint.setStyle(Paint.Style.FILL);
        laserPaint = new Paint(); laserPaint.setStyle(Paint.Style.FILL); laserPaint.setAntiAlias(true);
        shockwavePaint = new Paint(); shockwavePaint.setStyle(Paint.Style.STROKE); shockwavePaint.setStrokeWidth(8f); shockwavePaint.setAntiAlias(true);
        floatingTextPaint = new Paint(); floatingTextPaint.setColor(Color.parseColor("#FFD700")); floatingTextPaint.setTextSize(72f); floatingTextPaint.setTextAlign(Paint.Align.CENTER); floatingTextPaint.setAntiAlias(true); floatingTextPaint.setFakeBoldText(true);
        fishTextPaint = new Paint(); fishTextPaint.setTextSize(80f); fishTextPaint.setAntiAlias(true); fishTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    // ====================== SETUP ======================
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

    // ====================== onDraw ======================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cells == null) return;
        cellSize = (float) getWidth() / gridSize;

        // Ekran Sarsıntısı uygula
        canvas.save();
        canvas.translate(cameraShakeX, cameraShakeY);

        // Karıştırma sırasında vortex döndürmesi
        if (isShuffling && shuffleProgress < 0.6f) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            canvas.rotate(shuffleRotation, cx, cy);
        }

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                GridCell cell = cells[r][c];

                boolean isExploding = isAnimating && explodingCells.contains(cell);
                boolean isCellSwapping = isSwapping && (cell == swapTarget1 || cell == swapTarget2);
                boolean isCellShaking = isShaking && shakeCells.contains(cell);

                float currentCellSize = cellSize;
                int currentAlpha = 255;
                float left = c * cellSize;
                float top = r * cellSize;

                // Düşme animasyonu – hücre görsel pozisyonunu ayarla
                if (isFalling) {
                    for (FallingCell fc : fallingCells) {
                        if (fc.col == c && fc.targetRow == r) {
                            float visualRow = fc.startVisualRow + (fc.targetRow - fc.startVisualRow) * fallProgress;
                            top = visualRow * cellSize;
                            break;
                        }
                    }
                }

                if (isExploding) {
                    currentCellSize = cellSize * (1f - explosionProgress);
                    currentAlpha = (int) (255 * (1f - explosionProgress));
                } else if (isShuffling) {
                    currentCellSize = cellSize * shuffleProgress;
                    currentAlpha = (int) (255 * shuffleProgress);
                } else if (isCellSwapping) {
                    currentCellSize = cellSize * swapProgress;
                } else if (isCellShaking) {
                    left += shakeOffset;
                }

                left += (cellSize - currentCellSize) / 2;
                top += (cellSize - currentCellSize) / 2;

                cellPaint.setAlpha(currentAlpha);
                textPaint.setAlpha(currentAlpha);
                powerPaint.setAlpha(currentAlpha);

                // Joker Moduna Göre Arkaplan Rengi
                Paint bgPaint = cellPaint;
                if (cell.isSelected || cell == swapTarget1) {
                    bgPaint = selectedPaint;
                } else if (currentJokerMode == JokerMode.LOLLIPOP) {
                    cellPaint.setColor(Color.parseColor("#FFF3E0"));
                } else if (currentJokerMode == JokerMode.WHEEL) {
                    cellPaint.setColor(Color.parseColor("#F3E5F5"));
                } else if (currentJokerMode == JokerMode.SWAP_1 || currentJokerMode == JokerMode.SWAP_2) {
                    cellPaint.setColor(Color.parseColor("#E3F2FD"));
                } else {
                    cellPaint.setColor(Color.WHITE);
                }

                canvas.drawRect(left + 2, top + 2, left + currentCellSize - 2, top + currentCellSize - 2, bgPaint);
                float textY = top + (currentCellSize / 2) - ((textPaint.descent() + textPaint.ascent()) / 2);
                canvas.drawText(String.valueOf(cell.letter), left + (currentCellSize / 2), textY, textPaint);

                if (cell.power != GridCell.SpecialPower.NONE) {
                    canvas.drawText(getPowerSymbol(cell.power), left + currentCellSize - 10, top + 40 * (currentCellSize / cellSize), powerPaint);
                }

                // Kırmızı flaş efekti (titreşim sırasında)
                if (isCellShaking && flashAlpha > 0) {
                    flashPaint.setAlpha((int) flashAlpha);
                    canvas.drawRect(left + 2, top + 2, left + currentCellSize - 2, top + currentCellSize - 2, flashPaint);
                }

                cellPaint.setAlpha(255);
                textPaint.setAlpha(255);
                powerPaint.setAlpha(255);
            }
        }

        // Seçim çizgisi
        if (currentPath.size() > 1 && currentJokerMode == JokerMode.NONE) {
            for (int i = 0; i < currentPath.size() - 1; i++) {
                GridCell start = currentPath.get(i), end = currentPath.get(i + 1);
                float startX = start.col * cellSize + (cellSize / 2) + (isShaking ? shakeOffset : 0);
                float endX = end.col * cellSize + (cellSize / 2) + (isShaking ? shakeOffset : 0);
                canvas.drawLine(startX, start.row * cellSize + (cellSize / 2),
                        endX, end.row * cellSize + (cellSize / 2), linePaint);
            }
        }

        // Lazer efektleri
        drawLaserEffects(canvas);

        // Şok dalgası efektleri
        drawShockwaveEffects(canvas);

        // Parçacıklar
        drawParticles(canvas);

        // Balık animasyonu
        if (isFishActive) {
            fishTextPaint.setAlpha(255);
            canvas.drawText("🐟", fishX, fishY, fishTextPaint);
        }

        // Yüzen puan metni
        drawFloatingTexts(canvas);

        canvas.restore();
    }

    private void drawParticles(Canvas canvas) {
        for (Particle p : particles) {
            particlePaint.setColor(p.color);
            particlePaint.setAlpha(p.alpha);
            canvas.drawRect(p.x - p.size, p.y - p.size, p.x + p.size, p.y + p.size, particlePaint);
        }
    }

    private void drawLaserEffects(Canvas canvas) {
        List<LaserAnim> snapshot = new ArrayList<>(laserAnimations);
        for (LaserAnim laser : snapshot) {
            float sweepProgress = laser.progress;
            laserPaint.setAlpha(200);
            if (laser.isRow) {
                float beamHeight = cellSize * 0.8f;
                float beamTop = laser.index * cellSize + (cellSize - beamHeight) / 2f;
                float sweepEnd = sweepProgress * getWidth();
                // Sarı lazer gövdesi
                laserPaint.setColor(Color.parseColor("#FFFF00"));
                canvas.drawRect(0, beamTop, sweepEnd, beamTop + beamHeight, laserPaint);
                // Parlak beyaz ön kenar
                if (sweepProgress < 1.0f) {
                    laserPaint.setColor(Color.WHITE);
                    laserPaint.setAlpha(255);
                    canvas.drawRect(sweepEnd - 20, beamTop, sweepEnd, beamTop + beamHeight, laserPaint);
                }
            } else {
                float beamWidth = cellSize * 0.8f;
                float beamLeft = laser.index * cellSize + (cellSize - beamWidth) / 2f;
                float sweepEnd = sweepProgress * getHeight();
                laserPaint.setColor(Color.parseColor("#FFFF00"));
                canvas.drawRect(beamLeft, 0, beamLeft + beamWidth, sweepEnd, laserPaint);
                if (sweepProgress < 1.0f) {
                    laserPaint.setColor(Color.WHITE);
                    laserPaint.setAlpha(255);
                    canvas.drawRect(beamLeft, sweepEnd - 20, beamLeft + beamWidth, sweepEnd, laserPaint);
                }
            }
        }
    }

    private void drawShockwaveEffects(Canvas canvas) {
        float maxRadius = Math.max(getWidth(), getHeight()) * 0.7f;
        List<ShockwaveAnim> snapshot = new ArrayList<>(shockwaveAnimations);
        for (ShockwaveAnim sw : snapshot) {
            float radius = sw.progress * maxRadius;
            int alpha = (int) (255 * (1f - sw.progress));
            shockwavePaint.setColor(Color.parseColor("#FFA500"));
            shockwavePaint.setAlpha(alpha);
            shockwavePaint.setStrokeWidth(Math.max(3f, 20f * (1f - sw.progress)));
            canvas.drawCircle(sw.cx, sw.cy, radius, shockwavePaint);
        }
    }

    private void drawFloatingTexts(Canvas canvas) {
        List<FloatingText> snapshot = new ArrayList<>(floatingTexts);
        for (FloatingText ft : snapshot) {
            floatingTextPaint.setAlpha((int) ft.alpha);
            canvas.save();
            canvas.scale(ft.scale, ft.scale, ft.x, ft.y);
            canvas.drawText(ft.text, ft.x, ft.y, floatingTextPaint);
            canvas.restore();
        }
    }

    private String getPowerSymbol(GridCell.SpecialPower power) {
        switch (power) {
            case ROW_CLEAR: return "⇆";
            case AREA_EXPLODE: return "✹";
            case COLUMN_CLEAR: return "⇅";
            case MEGA_EXPLODE: return "✪";
            default: return "";
        }
    }

    // ====================== TOUCH ======================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cells == null || isAnyAnimationActive()) return false;

        float x = event.getX(), y = event.getY();
        int col = (int) (x / cellSize), row = (int) (y / cellSize);
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) return true;

        GridCell touchedCell = cells[row][col];

        if (currentJokerMode != JokerMode.NONE) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                handleJokerTouch(touchedCell);
            }
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

    // ====================== JOKER YÖNETİMİ ======================
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
            startExplosionAnimation(exp, false);
            if (gameListener != null) gameListener.onJokerUsed("Lolipop Kırıcı");

        } else if (currentJokerMode == JokerMode.WHEEL) {
            currentJokerMode = JokerMode.NONE;
            List<GridCell> exp = new ArrayList<>();
            for (int i = 0; i < gridSize; i++) {
                exp.add(cells[cell.row][i]);
                if (i != cell.col) exp.add(cells[i][cell.col]);
            }
            startLaserForRowCol(cell.row, cell.col);
            startExplosionAnimation(exp, false);
            if (gameListener != null) gameListener.onJokerUsed("Tekerlek");

        } else if (currentJokerMode == JokerMode.SWAP_1) {
            swapTarget1 = cell;
            currentJokerMode = JokerMode.SWAP_2;
            invalidate();

        } else if (currentJokerMode == JokerMode.SWAP_2) {
            if (cell == swapTarget1) {
                currentJokerMode = JokerMode.NONE;
                swapTarget1 = null;
                invalidate();
            } else if (Math.abs(cell.row - swapTarget1.row) <= 1 && Math.abs(cell.col - swapTarget1.col) <= 1) {
                currentJokerMode = JokerMode.NONE;
                startSwapAnimation(swapTarget1, cell);
                if (gameListener != null) gameListener.onJokerUsed("Serbest Değiştirme");
            } else {
                swapTarget1 = cell;
                invalidate();
            }
        }
    }

    // 🐟 Balık Jokeri – balık yüzerek harfleri "yer"
    public void useFishJoker() {
        if (isAnimating || isFishActive) return;

        fishTargets.clear();
        int count = 0;
        while (count < 5) {
            int r = random.nextInt(gridSize);
            int c = random.nextInt(gridSize);
            boolean alreadyAdded = false;
            for (GridCell t : fishTargets) {
                if (t.row == r && t.col == c) { alreadyAdded = true; break; }
            }
            if (!alreadyAdded) {
                fishTargets.add(cells[r][c]);
                count++;
            }
        }
        // Balık soldan sağa gittiği için sütuna göre sırala
        Collections.sort(fishTargets, (a, b) -> a.col - b.col);
        startFishAnimation();
        if (gameListener != null) gameListener.onJokerUsed("Balık");
    }

    private void startFishAnimation() {
        isFishActive = true;
        final List<GridCell> eatenCells = new ArrayList<>();

        ValueAnimator fishAnimator = ValueAnimator.ofFloat(0f, 1f);
        fishAnimator.setDuration(1800);
        fishAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            // Soldan sağa sinüs dalgası hareketi
            fishX = progress * (getWidth() + cellSize * 2) - cellSize;
            fishY = getHeight() / 2f + (float) Math.sin(progress * Math.PI * 3) * cellSize * 1.5f;

            // Balık bir hedef hücrenin üzerinden geçiyor mu?
            for (GridCell target : fishTargets) {
                if (!eatenCells.contains(target)) {
                    float cellCenterX = target.col * cellSize + cellSize / 2f;
                    if (fishX > cellCenterX - cellSize * 0.4f && fishX < cellCenterX + cellSize * 0.4f) {
                        eatenCells.add(target);
                        spawnParticlesAt(cellCenterX, target.row * cellSize + cellSize / 2f, 6);
                        target.letter = ' ';
                        target.power = GridCell.SpecialPower.NONE;
                    }
                }
            }
            invalidate();
        });
        fishAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isFishActive = false;
                applyGravityWithBounce();
                if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
            }
        });
        fishAnimator.start();
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
        startShockwaveAnimation(getWidth() / 2f, getHeight() / 2f);
        startCameraShake();
        startExplosionAnimation(exp, true);
    }

    // 🔄 Takas Animasyonu (Kavisli küçülüp büyüme)
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

    // ====================== SEÇİM MANTIĞI ======================
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
            // Çok kısa – titret
            startShakeAnimation();
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
            boolean hasBombPower = false;

            GridCell.SpecialPower powerToLeave = GridCell.SpecialPower.NONE;
            if (word.length() == 4) powerToLeave = GridCell.SpecialPower.ROW_CLEAR;
            else if (word.length() == 5) powerToLeave = GridCell.SpecialPower.AREA_EXPLODE;
            else if (word.length() == 6) powerToLeave = GridCell.SpecialPower.COLUMN_CLEAR;
            else if (word.length() >= 7) powerToLeave = GridCell.SpecialPower.MEGA_EXPLODE;

            for (GridCell cell : currentPath) {
                cellsToClear[cell.row][cell.col] = true;
                if (cell.power != GridCell.SpecialPower.NONE) {
                    triggeredPowerCells.add(cell);
                    if (cell.power == GridCell.SpecialPower.AREA_EXPLODE
                            || cell.power == GridCell.SpecialPower.MEGA_EXPLODE) {
                        hasBombPower = true;
                    }
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

            // Güç efektleri
            if (hasBombPower
                    || powerToLeave == GridCell.SpecialPower.AREA_EXPLODE
                    || powerToLeave == GridCell.SpecialPower.MEGA_EXPLODE) {
                startShockwaveAnimation(lastCell.col * cellSize + cellSize / 2f,
                        lastCell.row * cellSize + cellSize / 2f);
                startCameraShake();
            }

            // Lazer efekti (tetiklenen satır/sütun güçleri)
            for (GridCell pwCell : triggeredPowerCells) {
                if (pwCell.power == GridCell.SpecialPower.ROW_CLEAR) {
                    addAndAnimateLaser(true, pwCell.row);
                } else if (pwCell.power == GridCell.SpecialPower.COLUMN_CLEAR) {
                    addAndAnimateLaser(false, pwCell.col);
                }
            }
            // Kelime uzunluğuna göre lazer
            if (powerToLeave == GridCell.SpecialPower.ROW_CLEAR) {
                addAndAnimateLaser(true, lastCell.row);
            } else if (powerToLeave == GridCell.SpecialPower.COLUMN_CLEAR) {
                addAndAnimateLaser(false, lastCell.col);
            }

            // Puan metni
            if (score > 0) {
                startFloatingText("+" + score,
                        lastCell.col * cellSize + cellSize / 2f,
                        lastCell.row * cellSize);
            }

            clearSelection();
            startExplosionAnimation(cellsToExplode, hasBombPower);
        } else {
            // Geçersiz kelime – titret + kırmızı flaş
            startShakeAnimation();
            if (gameListener != null) gameListener.onInvalidWord();
            // clearSelection() titreme animasyonunun sonunda çağrılır
        }
    }

    // ====================== ANİMASYON METODLARI ======================

    // 1. PARÇACIK OLUŞTURMA
    private void spawnParticlesAt(float cx, float cy, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = cx;
            p.y = cy;
            double angle = random.nextDouble() * Math.PI * 2;
            float speed = 3f + random.nextFloat() * 8f;
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed - 4f;
            p.size = 4f + random.nextFloat() * 8f;
            p.alpha = 255;
            p.color = CONFETTI_COLORS[random.nextInt(CONFETTI_COLORS.length)];
            particles.add(p);
        }
        // Eğer animasyon çalışmıyorsa (örn. balık jokeri) otomatik başlat
        if (particleAnimator == null || !particleAnimator.isRunning()) {
            startParticleAnimation();
        }
    }

    private void startParticleAnimation() {
        if (particleAnimator != null && particleAnimator.isRunning()) {
            particleAnimator.cancel();
        }
        particleAnimator = ValueAnimator.ofFloat(0f, 1f);
        particleAnimator.setDuration(700);
        particleAnimator.addUpdateListener(animation -> {
            for (Particle p : particles) {
                p.x += p.vx;
                p.y += p.vy;
                p.vy += 0.5f;   // Yerçekimi
                p.vx *= 0.95f;  // Sürtünme
                p.alpha = Math.max(0, p.alpha - 4);
                p.size = Math.max(1f, p.size * 0.97f);
            }
            invalidate();
        });
        particleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                particles.clear();
                invalidate();
            }
        });
        particleAnimator.start();
    }

    // 2. TİTREME + KIRMIZI FLAŞ (Geçersiz kelime)
    private void startShakeAnimation() {
        if (currentPath.isEmpty()) {
            clearSelection();
            return;
        }
        isShaking = true;
        shakeCells.clear();
        shakeCells.addAll(currentPath);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(400);
        animator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            // Sönümlü sinüs – sağa sola titreşim
            shakeOffset = (float) (Math.sin(t * Math.PI * 5) * cellSize * 0.15f * (1f - t));
            // Kırmızı flaş: önce belirgin, sonra söner
            flashAlpha = 120f * (1f - t);
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isShaking = false;
                shakeOffset = 0f;
                flashAlpha = 0f;
                shakeCells.clear();
                clearSelection();
                invalidate();
            }
        });
        animator.start();
    }

    // 3. PATLAMA ANİMASYONU (Parçacıklarla)
    private void startExplosionAnimation(List<GridCell> cellsToExplode, boolean isBombExplosion) {
        if (cellsToExplode.isEmpty()) {
            applyGravityWithBounce();
            return;
        }
        isAnimating = true;
        explodingCells.clear();
        explodingCells.addAll(cellsToExplode);

        // Her patlayan hücreden parçacık oluştur
        for (GridCell cell : cellsToExplode) {
            float cx = cell.col * cellSize + cellSize / 2f;
            float cy = cell.row * cellSize + cellSize / 2f;
            spawnParticlesAt(cx, cy, isBombExplosion ? 14 : 8);
        }
        startParticleAnimation();

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
                    cell.letter = ' ';
                    cell.power = GridCell.SpecialPower.NONE;
                }
                applyGravityWithBounce();
                if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
            }
        });
        animator.start();
    }

    // 4. LAZER EFEKTİ – satır/sütun temizleme
    private void startLaserForRowCol(int row, int col) {
        addAndAnimateLaser(true, row);
        addAndAnimateLaser(false, col);
    }

    private void addAndAnimateLaser(boolean isRow, int index) {
        LaserAnim laser = new LaserAnim();
        laser.isRow = isRow;
        laser.index = index;
        laser.progress = 0f;
        laserAnimations.add(laser);

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(350);
        anim.addUpdateListener(animation -> {
            laser.progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                laserAnimations.remove(laser);
                invalidate();
            }
        });
        anim.start();
    }

    // 5. ŞOK DALGASI
    private void startShockwaveAnimation(float cx, float cy) {
        ShockwaveAnim sw = new ShockwaveAnim();
        sw.cx = cx;
        sw.cy = cy;
        sw.progress = 0f;
        shockwaveAnimations.add(sw);

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(500);
        anim.addUpdateListener(animation -> {
            sw.progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                shockwaveAnimations.remove(sw);
                invalidate();
            }
        });
        anim.start();
    }

    // 6. KAMERA SARSINTISI
    private void startCameraShake() {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(300);
        anim.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            float intensity = 12f * (1f - t);
            cameraShakeX = (float) (Math.sin(t * Math.PI * 8) * intensity);
            cameraShakeY = (float) (Math.cos(t * Math.PI * 7) * intensity * 0.7f);
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cameraShakeX = 0f;
                cameraShakeY = 0f;
                invalidate();
            }
        });
        anim.start();
    }

    // 7. YÜZEN PUAN METNİ
    private void startFloatingText(String text, float x, float y) {
        FloatingText ft = new FloatingText();
        ft.text = text;
        ft.x = x;
        ft.y = y;
        ft.alpha = 255f;
        ft.scale = 1.5f;
        floatingTexts.add(ft);

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(800);
        anim.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            ft.y = y - t * cellSize * 2f;
            ft.alpha = 255f * (1f - t);
            ft.scale = 1.5f - 0.5f * t;
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                floatingTexts.remove(ft);
                invalidate();
            }
        });
        anim.start();
    }

    // 8. YERÇEKIMI + SEKME (Bounce) ANİMASYONU
    private void applyGravityWithBounce() {
        fallingCells.clear();

        // Her sütun için düşen ve yeni hücreleri hesapla
        for (int c = 0; c < gridSize; c++) {
            // Boş olmayan satırları bul
            List<Integer> nonEmptyRows = new ArrayList<>();
            for (int r = 0; r < gridSize; r++) {
                if (cells[r][c].letter != ' ') nonEmptyRows.add(r);
            }

            int newEmptyCount = gridSize - nonEmptyRows.size();

            // Düşen mevcut hücreler
            for (int i = 0; i < nonEmptyRows.size(); i++) {
                int oldRow = nonEmptyRows.get(i);
                int newRow = newEmptyCount + i;
                if (oldRow != newRow) {
                    FallingCell fc = new FallingCell();
                    fc.col = c;
                    fc.targetRow = newRow;
                    fc.startVisualRow = oldRow;
                    fallingCells.add(fc);
                }
            }

            // Üstten gelen yeni hücreler
            for (int i = 0; i < newEmptyCount; i++) {
                FallingCell fc = new FallingCell();
                fc.col = c;
                fc.targetRow = i;
                fc.startVisualRow = i - newEmptyCount; // Negatif = ekranın üstü
                fallingCells.add(fc);
            }
        }

        // Veriyi anında güncelle (görsel animasyon ayrı)
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

        if (fallingCells.isEmpty()) {
            invalidate();
            return;
        }

        isFalling = true;
        fallProgress = 0f;

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(600);
        anim.setInterpolator(new BounceInterpolator());
        anim.addUpdateListener(animation -> {
            fallProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isFalling = false;
                fallingCells.clear();
                fallProgress = 1f;
                invalidate();
            }
        });
        anim.start();
    }

    // ====================== SHUFFLE (VORTEX) ======================
    public void shuffleBoard() {
        if (cells == null || isShuffling) return;
        isShuffling = true;
        shuffleRotation = 0f;
        final boolean[] shuffled = {false}; // karıştırma tam bir kez yapılsın

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(700);
        animator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            if (t < 0.5f) {
                shuffleProgress = 1f - t * 2f;       // Küçül
                shuffleRotation = t * 720f;           // Döndür
            } else {
                shuffleProgress = (t - 0.5f) * 2f;   // Büyü
                shuffleRotation = (1f - t) * 720f;   // Geri döndür
            }
            // Animasyonun tam ortasında (ilk kez) harfleri karıştır
            if (t >= 0.5f && !shuffled[0]) {
                shuffled[0] = true;
                List<Character> letters = new ArrayList<>();
                List<GridCell.SpecialPower> powers = new ArrayList<>();
                for (int r = 0; r < gridSize; r++) {
                    for (int c = 0; c < gridSize; c++) {
                        letters.add(cells[r][c].letter);
                        powers.add(cells[r][c].power);
                    }
                }
                Collections.shuffle(letters, random);
                Collections.shuffle(powers, random);
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
                shuffleRotation = 0f;
                shuffleProgress = 1f;
                if (gameListener != null) gameListener.onGridAnalyzed(calculateAvailableWords());
            }
        });
        animator.start();
    }

    // ====================== YARDIMCI METODLAR ======================

    /** Herhangi bir animasyon aktif mi? Dokunmatik girişi engeller. */
    private boolean isAnyAnimationActive() {
        return isAnimating || isShuffling || isSwapping || isFalling || isShaking || isFishActive;
    }

    private void markPowerAffectedCells(GridCell.SpecialPower power, int centerRow, int centerCol, boolean[][] cellsToClear) {
        switch (power) {
            case ROW_CLEAR:    for (int c = 0; c < gridSize; c++) cellsToClear[centerRow][c] = true; break;
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