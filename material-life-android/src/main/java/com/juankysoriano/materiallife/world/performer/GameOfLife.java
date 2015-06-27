package com.juankysoriano.materiallife.world.performer;

import android.view.MotionEvent;

import com.juankysoriano.materiallife.ContextRetriever;
import com.juankysoriano.materiallife.R;
import com.juankysoriano.rainbow.core.drawing.RainbowDrawer;
import com.juankysoriano.rainbow.core.event.RainbowInputController;
import com.juankysoriano.rainbow.core.graphics.RainbowImage;
import com.openca.Automata;
import com.openca.bi.OnCellUpdatedCallback2D;
import com.openca.bi.discrete.AutomataDiscrete2D;

public class GameOfLife implements RainbowInputController.RainbowInteractionListener, OnCellUpdatedCallback2D, RainbowDrawer.PointDetectedListener {
    private static final int ALIVE_COLOR = ContextRetriever.INSTANCE.getApplicationContext().getResources().getColor(R.color.alive);
    private static final int DEAD_COLOR = ContextRetriever.INSTANCE.getApplicationContext().getResources().getColor(R.color.dead);
    private static final int SCALE_FACTOR = 10;
    private static final int ALPHA = 70;
    private static final int ALIVE_CELL_THRESHOLD = 128;
    private static final int THREE = 3;
    private static final int ALIVE = 1;
    private static final int DEAD = 0;
    private static final float OPAQUE = 255;
    private final AutomataDiscrete2D gameOfLife;
    private final RainbowDrawer rainbowDrawer;
    private final RainbowInputController rainbowInputController;
    private boolean editing;
    private int[][] cellsBackup;

    public static GameOfLife newInstance(RainbowDrawer rainbowDrawer, RainbowInputController rainbowInputController) {
        AutomataDiscrete2D automata = initAutomata(rainbowDrawer.getWidth() / SCALE_FACTOR, rainbowDrawer.getHeight() / SCALE_FACTOR);
        GameOfLife gameOfLife = new GameOfLife(automata, rainbowDrawer, rainbowInputController);
        rainbowInputController.setRainbowInteractionListener(gameOfLife);

        configure(rainbowDrawer);

        return new GameOfLife(automata, rainbowDrawer, rainbowInputController);
    }

    private static void configure(RainbowDrawer rainbowDrawer) {
        rainbowDrawer.noStroke();
        rainbowDrawer.smooth();
        rainbowDrawer.vSync();
        rainbowDrawer.fill(ALIVE_COLOR);
    }

    private static AutomataDiscrete2D initAutomata(int width, int height) {
        Automata.Builder builder = new Automata.Builder();
        AutomataDiscrete2D automata = builder.width(width)
                .height(height)
                .radius(1)
                .states(2)
                .rule("8-24")
                .type(Automata.Type.OUTER_TOTALISTIC)
                .domain(Automata.Domain.DISCRETE)
                .dimension(Automata.Dimension.BIDIMENSIONAL)
                .build();
        automata.randomiseConfiguration();
        return automata;
    }

    protected GameOfLife(AutomataDiscrete2D gameOfLife, RainbowDrawer rainbowDrawer, RainbowInputController rainbowInputController) {
        this.gameOfLife = gameOfLife;
        this.rainbowDrawer = rainbowDrawer;
        this.rainbowInputController = rainbowInputController;
    }

    public void doStep() {
        paintBackground(ALPHA);
        if (editing) {
            paintCellsWithoutEvolution();
        } else {
            paintCellsAndEvolve();
        }
    }

    private void paintCellsWithoutEvolution() {
        for (int i = 0; i < gameOfLife.getWidth(); i++) {
            for (int j = 0; j < gameOfLife.getHeight(); j++) {
                onCellDetected(i, j, gameOfLife.getCells()[i][j]);
            }
        }
    }

    private void paintCellsAndEvolve() {
        gameOfLife.evolve(this);
    }

    @Override
    public void onCellDetected(int x, int y, int state) {
        if (isCellAlive(state)) {
            paintCellAt(x, y);
        }
    }

    private boolean isCellAlive(int state) {
        return state == ALIVE;
    }

    private void paintCellAt(int x, int y) {
        rainbowDrawer.rect(x * SCALE_FACTOR, y * SCALE_FACTOR, SCALE_FACTOR, SCALE_FACTOR);
    }

    private void paintBackground(float ALPHA) {
        rainbowDrawer.background(DEAD_COLOR, ALPHA);
    }

    @Override
    public void onSketchTouched(MotionEvent motionEvent, RainbowDrawer rainbowDrawer) {
        onPointDetected(rainbowInputController.getPreviousX(),
                rainbowInputController.getPreviousY(),
                rainbowInputController.getX(),
                rainbowInputController.getY(), rainbowDrawer);
    }

    @Override
    public void onSketchReleased(MotionEvent motionEvent, RainbowDrawer rainbowDrawer) {
        //no-op
    }

    @Override
    public void onFingerDragged(MotionEvent motionEvent, RainbowDrawer rainbowDrawer) {
        int x = (int) rainbowInputController.getSmoothX();
        int y = (int) rainbowInputController.getSmoothY();
        int previousX = (int) rainbowInputController.getPreviousSmoothX();
        int previousY = (int) rainbowInputController.getPreviousSmoothY();

        rainbowDrawer.exploreLine(previousX, previousY, x, y, RainbowDrawer.Precision.HIGH, this);
    }

    @Override
    public void onPointDetected(float px, float py, float x, float y, RainbowDrawer rainbowDrawer) {
        if (x >= 0 && x < rainbowDrawer.getWidth()
                && y > 0 && y < rainbowDrawer.getHeight()) {
            gameOfLife.getCells()[((int) x / SCALE_FACTOR)][((int) y / SCALE_FACTOR)] = ALIVE;
        }
    }

    @Override
    public void onMotionEvent(MotionEvent motionEvent, RainbowDrawer rainbowDrawer) {

    }

    public void startEdition() {
        if (!editing) {
            editing = true;
            doCellsBackup();
        }
    }

    private void doCellsBackup() {
        cellsBackup = new int[gameOfLife.getWidth()][];
        for (int i = 0; i < gameOfLife.getWidth(); i++) {
            int[] row = gameOfLife.getCells()[i];
            cellsBackup[i] = new int[row.length];
            System.arraycopy(row, 0, cellsBackup[i], 0, row.length);
        }
    }

    public void endEdition() {
        editing = false;
    }

    public void clear() {
        for (int i = 0; i < gameOfLife.getWidth(); i++) {
            for (int j = 0; j < gameOfLife.getHeight(); j++) {
                gameOfLife.getCells()[i][j] = 0;
            }
        }
    }

    public void restoreLastWorld() {
        for (int i = 0; i < gameOfLife.getWidth(); i++) {
            System.arraycopy(cellsBackup[i], 0, gameOfLife.getCells()[i], 0, gameOfLife.getHeight());
        }
    }

    public void loadWorldFrom(RainbowImage image) {
        for (int i = 0; i < gameOfLife.getWidth(); i++) {
            for (int j = 0; j < gameOfLife.getHeight(); j++) {
                gameOfLife.getCells()[i][j] = extrapolateCellStateFrom(image.get(i * SCALE_FACTOR, j * SCALE_FACTOR));
            }
        }
    }

    private int extrapolateCellStateFrom(int color) {
        int red = (int) rainbowDrawer.red(color);
        int green = (int) rainbowDrawer.green(color);
        int blue = (int) rainbowDrawer.blue(color);
        int grey = (red + green + blue) / THREE;

        return grey < ALIVE_CELL_THRESHOLD ? DEAD : ALIVE;
    }

    public void fastClear() {
        paintBackground(OPAQUE);
        clear();
    }
}
