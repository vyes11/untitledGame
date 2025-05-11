package thegame.screens;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2f;

import com.google.gson.Gson;

import thegame.App;
import thegame.onScreenObjects.Button;
import thegame.utils.LevelConfig;
import thegame.utils.LevelData;
import thegame.Screen;

public class GameScreen implements Screen {
    private final int gridSize;
    private final LevelConfig.Cell[][] grid;
    private final LevelConfig.Cell[][] targetPattern;
    private final int maxMoves;
    private int movesUsed;
    private final String levelName;
    private boolean isDragging = false;
    private int dragStartRow = -1;
    private int dragStartCol = -1;
    private final App app;
    private final Button backButton;

    private static final float CELL_SIZE = 0.4f;
    private static final float GRID_SPACING = 0.05f;

    public GameScreen(App app, LevelConfig levelConfig) {
        this.app = app;
        this.gridSize = levelConfig.getSettings().getGridSize();
        this.grid = levelConfig.getGrid();
        this.targetPattern = levelConfig.getTargetPattern();
        this.maxMoves = levelConfig.getSettings().getMaxMoves();
        this.movesUsed = 0;
        this.levelName = levelConfig.getName();
        
        // Initialize back button
        backButton = new Button(
            app, 
            "Back", 
            -0.2f, 
            -0.8f, 
            0.4f, 
            0.15f, 
            () -> app.setCurrentScreen(new LevelSelectScreen(app))
        );
    }

    @Override
    public void render() {
        glClearColor(1.0f, 0.71f, 0.76f, 1.0f); // Pink background
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        renderGrid();
        backButton.render();
    }

    private void renderGrid() {
        float totalWidth = gridSize * (CELL_SIZE + GRID_SPACING) - GRID_SPACING;
        float startX = -totalWidth / 2;
        float startY = -totalWidth / 2;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                float x = startX + col * (CELL_SIZE + GRID_SPACING);
                float y = startY + row * (CELL_SIZE + GRID_SPACING);

                LevelConfig.Cell cell = grid[row][col];
                if (cell != null) {
                    glColor3f(cell.red, cell.green, cell.blue);
                    
                    glBegin(GL_QUADS);
                    glVertex2f(x, y);
                    glVertex2f(x + CELL_SIZE, y);
                    glVertex2f(x + CELL_SIZE, y + CELL_SIZE);
                    glVertex2f(x, y + CELL_SIZE);
                    glEnd();

                    // Draw center marker if this is a rotation center
                    if (cell.isCenter) {
                        glColor3f(1.0f - cell.red, 1.0f - cell.green, 1.0f - cell.blue);
                        glBegin(GL_LINES);
                        float centerX = x + CELL_SIZE / 2;
                        float centerY = y + CELL_SIZE / 2;
                        float markerSize = CELL_SIZE / 8;
                        glVertex2f(centerX - markerSize, centerY);
                        glVertex2f(centerX + markerSize, centerY);
                        glVertex2f(centerX, centerY - markerSize);
                        glVertex2f(centerX, centerY + markerSize);
                        glEnd();
                    }
                }
            }
        }
    }

    private boolean isBlackCell(LevelConfig.Cell cell) {
        return cell.red == 0.0f && cell.green == 0.0f && cell.blue == 0.0f;
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        // Check for remaining moves
        if (movesUsed >= maxMoves) {
            // Maybe show a "out of moves" message
            return;
        }

        int[] cellCoords = getCellFromCoordinates(mouseX, mouseY);
        if (cellCoords != null) {
            LevelConfig.Cell clickedCell = grid[cellCoords[0]][cellCoords[1]];
            if (clickedCell.isCenter && !isBlackCell(clickedCell)) {
                // Start rotation
                dragStartRow = cellCoords[0];
                dragStartCol = cellCoords[1];
                isDragging = true;
            }
        }

        // Check back button
        if (backButton.contains((float)mouseX, (float)mouseY)) {
            backButton.click();
        }
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        if (isDragging) {
            // Perform rotation and increment moves counter
            int[] cellPos = getCellFromCoordinates(mouseX, mouseY);
            if (cellPos != null && grid[cellPos[0]][cellPos[1]].isCenter) {
                LevelConfig.Cell temp = grid[dragStartRow][dragStartCol];
                grid[dragStartRow][dragStartCol] = grid[cellPos[0]][cellPos[1]];
                grid[cellPos[0]][cellPos[1]] = temp;
                checkVictory();
            }
            movesUsed++;
            isDragging = false;
            dragStartRow = -1;
            dragStartCol = -1;
        }
    }

    private void checkVictory() {
        // Temporarily disabled victory checking
        // Will be implemented later
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        backButton.setHovered(backButton.contains((float)mouseX, (float)mouseY));
    }

    private int[] getCellFromCoordinates(double glX, double glY) {
        float totalWidth = gridSize * (CELL_SIZE + GRID_SPACING) - GRID_SPACING;
        float startX = -totalWidth / 2;
        float startY = -totalWidth / 2;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                float x = startX + col * (CELL_SIZE + GRID_SPACING);
                float y = startY + row * (CELL_SIZE + GRID_SPACING);

                if (glX >= x && glX <= x + CELL_SIZE &&
                    glY >= y && glY <= y + CELL_SIZE) {
                    // Check if the cell is black (immovable)
                    if (isBlackCell(grid[row][col])) {
                        return null;
                    }
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }
}