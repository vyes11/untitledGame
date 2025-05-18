package thegame.screens;

import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glRasterPos2f;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.flag.ImGuiWindowFlags;

import thegame.App;
import thegame.Screen;
import thegame.utils.ImGuiUtils;
import thegame.utils.LevelConfig;

public class GameScreen implements Screen {
    private static final boolean DEBUG = true;
    
    // Move type constants
    private static final int MOVE_SWAP = 0;
    private static final int MOVE_ROW = 1;
    private static final int MOVE_COLUMN = 2;
    private static final int MOVE_MULTIPLY = 3; // Only for number mode

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
    private boolean showingVictory = false;

    // Victory/failure states
    private boolean hasWon = false;
    private boolean hasLost = false;

    // Level tracking
    private final int currentLevelNumber;

    private boolean isNumberMode;
    private int currentMoveType = MOVE_SWAP;
    private int selectedRow = -1;
    private int selectedCol = -1;

    public GameScreen(App app, LevelConfig levelConfig) {
        if (DEBUG) System.out.println("Initializing GameScreen...");
        
        this.app = app;
        
        if (levelConfig == null) {
            throw new IllegalArgumentException("levelConfig cannot be null");
        }
        if (levelConfig.getSettings() == null) {
            throw new IllegalArgumentException("levelConfig settings cannot be null");
        }

        this.gridSize = levelConfig.getSettings().getGridSize();
        if (DEBUG) System.out.printf("Grid size: %d%n", this.gridSize);

        this.grid = levelConfig.getGrid();
        if (grid == null || grid.length != gridSize || grid[0].length != gridSize) {
            throw new IllegalArgumentException(
                String.format("Invalid grid dimensions. Expected %dx%d, got %dx%d", 
                    gridSize, gridSize,
                    grid == null ? 0 : grid.length,
                    grid == null ? 0 : grid[0].length)
            );
        }

        this.targetPattern = levelConfig.getTargetPattern();
        if (targetPattern == null || targetPattern.length != gridSize || targetPattern[0].length != gridSize) {
            throw new IllegalArgumentException("Invalid target pattern dimensions");
        }

        this.maxMoves = levelConfig.getSettings().getMaxMoves();
        this.movesUsed = 0;
        this.levelName = levelConfig.getName();
        this.currentLevelNumber = levelConfig.getLevelNumber();
        this.isNumberMode = levelConfig.getSettings().isNumberMode();
        
        if (DEBUG) {
            System.out.printf("Level loaded: %s%n", levelName);
            System.out.printf("Max moves: %d%n", maxMoves);
            debugPrintGrid();
        }
    }

    private void debugPrintGrid() {
        System.out.println("Current Grid State:");
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                LevelConfig.Cell cell = grid[i][j];
                System.out.printf("(%d,%d): RGB(%.1f,%.1f,%.1f) Center:%b | ",
                    i, j, cell.red, cell.green, cell.blue, cell.isCenter);
            }
            System.out.println();
        }
    }

    private void renderGrid(LevelConfig.Cell[][] gridToRender, float posX, float posY, float cellSize) {
        ImVec2 pos = ImGui.getCursorScreenPos();
        float gridWidth = gridSize * cellSize;
        float startX = pos.x + posX;
        float startY = pos.y + posY;
        
        // Draw grid background
        ImGui.getWindowDrawList().addRectFilled(
            startX - 5, 
            startY - 5, 
            startX + gridWidth + 5, 
            startY + gridWidth + 5, 
            ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1.0f)
        );

        // Draw cells
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                float x = startX + col * cellSize;
                float y = startY + row * cellSize;
                LevelConfig.Cell cell = gridToRender[row][col];
                
                // Draw cell background
                ImGui.getWindowDrawList().addRectFilled(
                    x, y, 
                    x + cellSize - 2, y + cellSize - 2, 
                    ImGui.colorConvertFloat4ToU32(cell.red, cell.green, cell.blue, 1.0f)
                );
                
                // Draw center marker
                if (cell.isCenter && gridToRender == grid) {
                    drawCenterMarker(x, y, cellSize, cell);
                }
                
                // Draw number if in number mode
                if (isNumberMode) {
                    int number = (int)(cell.red * 9);
                    if (number > 0) {
                        float textWidth = ImGui.calcTextSize(String.valueOf(number)).x;
                        ImGui.getWindowDrawList().addText(
                            x + (cellSize - textWidth) / 2, 
                            y + (cellSize - ImGui.getTextLineHeight()) / 2,
                            ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 1f),
                            String.valueOf(number)
                        );
                    }
                }
            }
        }
    }

    private void drawCenterMarker(float x, float y, float size, LevelConfig.Cell cell) {
        float centerX = x + size / 2;
        float centerY = y + size / 2;
        float markerSize = size / 4;
        
        // Draw cross in contrasting color
        int contrastColor = ImGui.colorConvertFloat4ToU32(
            1.0f - cell.red, 1.0f - cell.green, 1.0f - cell.blue, 1.0f
        );
        
        ImGui.getWindowDrawList().addLine(
            centerX - markerSize, centerY,
            centerX + markerSize, centerY,
            contrastColor, 2.0f
        );
        
        ImGui.getWindowDrawList().addLine(
            centerX, centerY - markerSize,
            centerX, centerY + markerSize,
            contrastColor, 2.0f
        );
    }

    @Override
    public void render() {
        glClearColor(0.2f, 0.8f, 0.2f, 1.0f); // Green background
        glClear(GL_COLOR_BUFFER_BIT);
        ImGui.text("Game Screen");
        float windowWidth = ImGui.getIO().getDisplaySizeX();
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        
        // Create a fullscreen window
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.begin("Game Screen", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | 
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar);
        
        // Calculate grid sizes
        float mainGridCellSize = 60.0f;
        float targetGridCellSize = 30.0f;
        
        // Calculate positions
        float mainGridX = windowWidth * 0.25f - (gridSize * mainGridCellSize) / 2;
        float mainGridY = windowHeight * 0.5f - (gridSize * mainGridCellSize) / 2;
        float targetGridX = windowWidth * 0.75f - (gridSize * targetGridCellSize) / 2;
        float targetGridY = windowHeight * 0.5f - (gridSize * targetGridCellSize) / 2;
        
        // Render level name
        ImGui.setCursorPos(windowWidth * 0.5f - ImGui.calcTextSize(levelName).x / 2, 20);
        ImGui.text(levelName);
        
        // Render move counter
        String movesText = String.format("Moves: %d/%d", movesUsed, maxMoves);
        ImGui.setCursorPos(windowWidth - ImGui.calcTextSize(movesText).x - 20, 20);
        ImGui.text(movesText);
        
        // Render grids
        renderGrid(grid, mainGridX, mainGridY, mainGridCellSize);
        
        // Draw "Target" text above target grid
        String targetText = "Target";
        ImGui.setCursorPos(targetGridX + (gridSize * targetGridCellSize) / 2 - ImGui.calcTextSize(targetText).x / 2, 
                          targetGridY - 30);
        ImGui.text(targetText);
        
        renderGrid(targetPattern, targetGridX, targetGridY, targetGridCellSize);
        
        // Render mode buttons at top
        ImGui.setCursorPos(20, 20);
        
        // Render move type buttons with different colors based on selection
        if (ImGui.button("Swap Mode", 120, 40)) {
            currentMoveType = MOVE_SWAP;
        }
        
        ImGui.sameLine();
        if (ImGui.button("Row Mode", 120, 40)) {
            currentMoveType = MOVE_ROW;
        }
        
        ImGui.sameLine();
        if (ImGui.button("Column Mode", 120, 40)) {
            currentMoveType = MOVE_COLUMN;
        }
        
        if (isNumberMode) {
            ImGui.sameLine();
            if (ImGui.button("Multiply Mode", 120, 40)) {
                currentMoveType = MOVE_MULTIPLY;
            }
        }
        
        // Highlight current mode
        ImGui.setCursorPos(20, 65);
        switch (currentMoveType) {
            case MOVE_SWAP:
                ImGui.textColored(1.0f, 1.0f, 0.0f, 1.0f, "Swap Mode: Click two center cells to swap their values");
                break;
            case MOVE_ROW:
                ImGui.textColored(1.0f, 1.0f, 0.0f, 1.0f, "Row Mode: Click two rows to swap them");
                break;
            case MOVE_COLUMN:
                ImGui.textColored(1.0f, 1.0f, 0.0f, 1.0f, "Column Mode: Click two columns to swap them");
                break;
            case MOVE_MULTIPLY:
                ImGui.textColored(1.0f, 1.0f, 0.0f, 1.0f, "Multiply Mode: Click to multiply values");
                break;
        }
        
        // Back button at bottom left
        ImGui.setCursorPos(20, windowHeight - 60);
        if (ImGui.button("Back to Level Select", 200, 40)) {
            app.setCurrentScreen(new LevelSelectScreen(app));
        }
        
        // Handle victory screen
        if (hasWon) {
            ImGui.setNextWindowPos(windowWidth / 2 - 200, windowHeight / 2 - 150);
            ImGui.setNextWindowSize(400, 300);
            ImGui.begin("Victory!", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | 
                    ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoScrollbar);
            
            ImGui.setCursorPosX(200 - ImGui.calcTextSize("Level Complete!").x / 2);
            ImGui.textColored(0.2f, 0.8f, 0.2f, 1.0f, "Level Complete!");
            
            ImGui.setCursorPosY(ImGui.getCursorPosY() + 20);
            
            ImGui.setCursorPosX(200 - 80);
            if (ImGui.button("Menu", 160, 40)) {
                app.setCurrentScreen(new LevelSelectScreen(app));
            }
            
            ImGui.setCursorPosX(200 - 80);
            if (ImGui.button("Restart", 160, 40)) {
                // Load the same level again
                try {
                    String levelPath = String.format("/levels/level%d.json", currentLevelNumber);
                    InputStream inputStream = GameScreen.class.getResourceAsStream(levelPath);
                    String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    LevelConfig levelConfig = new Gson().fromJson(jsonContent, LevelConfig.class);
                    app.setCurrentScreen(new GameScreen(app, levelConfig));
                } catch (Exception e) {
                    System.err.println("Error reloading level: " + e.getMessage());
                }
            }
            
            ImGui.setCursorPosX(200 - 80);
            if (ImGui.button("Next Level", 160, 40)) {
                // Load next level
                try {
                    String nextLevelPath = String.format("/levels/level%d.json", currentLevelNumber + 1);
                    InputStream inputStream = GameScreen.class.getResourceAsStream(nextLevelPath);
                    if (inputStream == null) {
                        System.err.println("No next level found");
                        app.setCurrentScreen(new LevelSelectScreen(app));
                        return;
                    }
                    String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    LevelConfig nextLevel = new Gson().fromJson(jsonContent, LevelConfig.class);
                    app.setCurrentScreen(new GameScreen(app, nextLevel));
                } catch (Exception e) {
                    System.err.println("Error loading next level: " + e.getMessage());
                    app.setCurrentScreen(new LevelSelectScreen(app));
                }
            }
            
            ImGui.end();
        }
        
        // Handle failure screen
        if (hasLost) {
            ImGui.setNextWindowPos(windowWidth / 2 - 200, windowHeight / 2 - 150);
            ImGui.setNextWindowSize(400, 300);
            ImGui.begin("Failed!", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | 
                    ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoScrollbar);
            
            ImGui.setCursorPosX(200 - ImGui.calcTextSize("Out of Moves!").x / 2);
            ImGui.textColored(0.8f, 0.2f, 0.2f, 1.0f, "Out of Moves!");
            
            ImGui.setCursorPosY(ImGui.getCursorPosY() + 20);
            
            ImGui.setCursorPosX(200 - 80);
            if (ImGui.button("Try Again", 160, 40)) {
                // Load the same level again
                try {
                    String levelPath = String.format("/levels/level%d.json", currentLevelNumber);
                    InputStream inputStream = GameScreen.class.getResourceAsStream(levelPath);
                    String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    LevelConfig levelConfig = new Gson().fromJson(jsonContent, LevelConfig.class);
                    app.setCurrentScreen(new GameScreen(app, levelConfig));
                } catch (Exception e) {
                    System.err.println("Error reloading level: " + e.getMessage());
                }
            }
            
            ImGui.setCursorPosX(200 - 80);
            if (ImGui.button("Menu", 160, 40)) {
                app.setCurrentScreen(new LevelSelectScreen(app));
            }
            
            ImGui.end();
        }
        
        ImGui.end();
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        // ImGui handles button clicks automatically
        
        // Only handle grid clicks manually
        if (!hasWon && !hasLost) {
            int[] cellCoords = getCellFromCoordinates(mouseX, mouseY);
            if (cellCoords != null) {
                handleMoveTypeClick(cellCoords[0], cellCoords[1]);
            }
        }
    }

    private void handleMoveTypeClick(int row, int col) {
        switch (currentMoveType) {
            case MOVE_SWAP:
                handleSwapMode(row, col);
                break;
            case MOVE_ROW:
                handleRowMode(row);
                break;
            case MOVE_COLUMN:
                handleColumnMode(col);
                break;
            case MOVE_MULTIPLY:
                if (isNumberMode) handleMultiplyMode(row, col);
                break;
        }
    }

    private void handleSwapMode(int row, int col) {
        LevelConfig.Cell clickedCell = grid[row][col];
        if (clickedCell.isCenter && !isBlackCell(clickedCell)) {
            dragStartRow = row;
            dragStartCol = col;
            isDragging = true;
        }
    }

    private void handleRowMode(int row) {
        if (selectedRow == -1) {
            selectedRow = row;
        } else {
            // Swap entire rows
            LevelConfig.Cell[] tempRow = grid[selectedRow].clone();
            grid[selectedRow] = grid[row].clone();
            grid[row] = tempRow;
            selectedRow = -1;
            movesUsed++;
            checkVictory();
        }
    }

    private void handleColumnMode(int col) {
        if (selectedCol == -1) {
            selectedCol = col;
        } else {
            // Swap entire columns
            for (int i = 0; i < gridSize; i++) {
                LevelConfig.Cell temp = grid[i][selectedCol];
                grid[i][selectedCol] = grid[i][col];
                grid[i][col] = temp;
            }
            selectedCol = -1;
            movesUsed++;
            checkVictory();
        }
    }

    private void handleMultiplyMode(int row, int col) {
        if (isNumberMode) {
            // Multiply selected row or column by 2 (example operation)
            if (selectedRow == -1 && selectedCol == -1) {
                // First click - select row or column
                selectedRow = row;
                selectedCol = col;
            } else {
                // Second click - perform multiplication
                if (selectedRow == row) {
                    // Multiply row
                    for (int i = 0; i < gridSize; i++) {
                        float newValue = grid[row][i].red * 2;
                        if (newValue <= 1.0f) { // Keep within valid range
                            grid[row][i].red = newValue;
                        }
                    }
                } else if (selectedCol == col) {
                    // Multiply column
                    for (int i = 0; i < gridSize; i++) {
                        float newValue = grid[i][col].red * 2;
                        if (newValue <= 1.0f) {
                            grid[i][col].red = newValue;
                        }
                    }
                }
                selectedRow = -1;
                selectedCol = -1;
                movesUsed++;
                checkVictory();
            }
        }
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        if (!isDragging) return;

        int[] cellPos = getCellFromCoordinates(mouseX, mouseY);
        if (cellPos != null && 
            (cellPos[0] != dragStartRow || cellPos[1] != dragStartCol)) { // Don't swap with self
            
            LevelConfig.Cell targetCell = grid[cellPos[0]][cellPos[1]];
            if (targetCell.isCenter && !isBlackCell(targetCell)) {
                if (DEBUG) System.out.printf("Swapping cells (%d,%d) and (%d,%d)%n",
                    dragStartRow, dragStartCol, cellPos[0], cellPos[1]);
                
                // Swap cells
                LevelConfig.Cell temp = grid[dragStartRow][dragStartCol];
                grid[dragStartRow][dragStartCol] = grid[cellPos[0]][cellPos[1]];
                grid[cellPos[0]][cellPos[1]] = temp;
                
                movesUsed++;
                if (DEBUG) System.out.printf("Moves used: %d/%d%n", movesUsed, maxMoves);
                checkVictory();
            }
        }

        isDragging = false;
        dragStartRow = -1;
        dragStartCol = -1;
    }

    private boolean checkMatch(LevelConfig.Cell cell1, LevelConfig.Cell cell2) {
        if (isNumberMode) {
            // Compare only red channel for numbers
            return Math.abs(cell1.red - cell2.red) < 0.01f;
        } else {
            // Compare all channels for colors
            return Math.abs(cell1.red - cell2.red) < 0.01f &&
                   Math.abs(cell1.green - cell2.green) < 0.01f &&
                   Math.abs(cell1.blue - cell2.blue) < 0.01f;
        }
    }

    private void checkVictory() {
        if (movesUsed > maxMoves) {
            hasLost = true;
            return;
        }
        
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                if (!checkMatch(grid[row][col], targetPattern[row][col])) {
                    return; // Grid doesn't match target yet
                }
            }
        }
        // If we get here, all cells match
        hasWon = true;
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        // ImGui handles hover states automatically
    }

    private int[] getCellFromCoordinates(double mouseX, double mouseY) {
        float windowWidth = ImGui.getIO().getDisplaySizeX();
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        
        // Calculate main grid position and size
        float mainGridCellSize = 60.0f;
        float mainGridX = windowWidth * 0.25f - (gridSize * mainGridCellSize) / 2;
        float mainGridY = windowHeight * 0.5f - (gridSize * mainGridCellSize) / 2;
        
        // Check if click is inside the main grid
        if (mouseX >= mainGridX && mouseX < mainGridX + gridSize * mainGridCellSize &&
            mouseY >= mainGridY && mouseY < mainGridY + gridSize * mainGridCellSize) {
            
            int col = (int)((mouseX - mainGridX) / mainGridCellSize);
            int row = (int)((mouseY - mainGridY) / mainGridCellSize);
            
            if (row >= 0 && row < gridSize && col >= 0 && col < gridSize) {
                return new int[]{row, col};
            }
        }
        
        return null;
    }

    private boolean isBlackCell(LevelConfig.Cell cell) {
        return cell.red == 0.0f && cell.green == 0.0f && cell.blue == 0.0f;
    }

    @Override
    public void handleKeyPress(int key, int action) {
        // Not needed as ImGui handles keyboard input
    }
    
}