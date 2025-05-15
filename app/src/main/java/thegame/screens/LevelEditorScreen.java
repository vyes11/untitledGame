package thegame.screens;

import org.bson.Document;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;

import com.mongodb.client.MongoCollection;

import thegame.App;
import thegame.Screen;
import thegame.utils.ImGuiUtils;
import thegame.utils.LevelConfig;
import thegame.utils.MongoDBConnection;

import java.util.ArrayList;
import java.util.List;
import imgui.flag.ImGuiCol;

public class LevelEditorScreen implements Screen {
    private final App app;
    private LevelConfig.Cell[][] grid;
    private LevelConfig.Cell[][] targetPattern;
    private int gridSize = 2; // Start with 2x2
    private int maxMoves = 3;
    
    // Selected color/number
    private float[] selectedColor = {0f, 0f, 0f}; // RGB values
    private boolean isNumberMode = false;
    private int selectedNumber = 0;
    
    // UI state
    private boolean editingTarget = false; // true = edit targetPattern, false = edit grid
    
    // ImGui positioning
    private static final float CELL_SIZE = 40.0f;
    private static final float GRID_SPACING = 20.0f;
    private static final float COLOR_BUTTON_SIZE = 30.0f;

    public LevelEditorScreen(App app) {
        this.app = app;
        initializeGrids();
    }

    private void initializeGrids() {
        grid = new LevelConfig.Cell[gridSize][gridSize];
        targetPattern = new LevelConfig.Cell[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                grid[i][j] = new LevelConfig.Cell(0, 0, 0, true);
                targetPattern[i][j] = new LevelConfig.Cell(0, 0, 0, true);
            }
        }
    }

    private void saveLevelToDatabase() {
        try {
            LevelConfig level = new LevelConfig.Builder()
                .withId(getNextLevelId())
                .withName("Custom Level")
                .withGrid(grid)
                .withTargetPattern(targetPattern)
                .withSettings(new LevelConfig.Settings(gridSize, maxMoves, "custom", isNumberMode))
                .withCreator("anonymous")
                .withDescription("Custom created level")
                .withNumberMode(isNumberMode)
                .build();

            // Convert to MongoDB document
            Document doc = level.toDocument();

            try (MongoDBConnection mongodb = new MongoDBConnection()) {
                MongoCollection<Document> collection = mongodb.getDatabase()
                    .getCollection("data");
                collection.insertOne(doc);
                System.out.println("Level saved successfully!");
                app.setCurrentScreen(new OnlineLevelSelectScreen(app));
            }
        } catch (Exception e) {
            System.err.println("Error saving level: " + e.getMessage());
        }
    }

    private int getNextLevelId() {
        try (MongoDBConnection mongodb = new MongoDBConnection()) {
            MongoCollection<Document> collection = mongodb.getDatabase()
                .getCollection("data");

            Document maxDoc = collection
                .find()
                .sort(new Document("id", -1))
                .limit(1)
                .first();

            return maxDoc != null ? maxDoc.getInteger("id", 0) + 1 : 1;
        } catch (Exception e) {
            System.err.println("Error getting next level ID: " + e.getMessage());
            return 1;
        }
    }

    @Override
    public void render() {
        float windowWidth = ImGui.getIO().getDisplaySizeX();
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        
        // Create a fullscreen window
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.begin("Level Editor", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | 
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar);
        
        // Title
        ImGui.setCursorPosX((windowWidth - ImGui.calcTextSize("Level Editor").x) / 2);
        ImGui.textColored(0.9f, 0.9f, 1.0f, 1.0f, "Level Editor");
        ImGui.separator();
        ImGui.spacing();
        
        // Left panel - Settings and Colors
        float leftPanelWidth = windowWidth * 0.2f;
        ImGui.beginChild("LeftPanel", leftPanelWidth, windowHeight - 100, true);
        
        // Settings section
        ImGui.text("Level Settings");
        ImGui.separator();
        
        // Grid size controls
        ImGui.text("Grid Size: " + gridSize);
        ImGui.sameLine();
        if (ImGui.button("-##GridSize", 20, 20) && gridSize > 2) {
            gridSize--;
            initializeGrids();
        }
        ImGui.sameLine();
        if (ImGui.button("+##GridSize", 20, 20) && gridSize < 6) {
            gridSize++;
            initializeGrids();
        }
        
        // Max moves controls
        ImGui.text("Max Moves: " + maxMoves);
        ImGui.sameLine();
        if (ImGui.button("-##MaxMoves", 20, 20) && maxMoves > 1) {
            maxMoves--;
        }
        ImGui.sameLine();
        if (ImGui.button("+##MaxMoves", 20, 20)) {
            maxMoves++;
        }
        
        // Mode toggle
        if (ImGui.button(isNumberMode ? "Switch to Colors" : "Switch to Numbers", 
                leftPanelWidth - 20, 30)) {
            isNumberMode = !isNumberMode;
        }
        
        ImGui.spacing();
        ImGui.separator();
        
        // Color/Number selection
        if (isNumberMode) {
            ImGui.text("Select Number:");
            ImGui.spacing();
            
            // Create a grid of number buttons
            int columns = 3;
            if (ImGui.beginTable("NumbersTable", columns)) {
                for (int i = 0; i <= 9; i++) {
                    if (i > 0 && i % columns == 0) {
                        ImGui.tableNextRow();  // FIXED: use tableNextRow()
                    }
                    
                    ImGui.tableNextColumn();
                    
                    // Color the selected number differently
                    if (selectedNumber == i) {
                        ImGui.pushStyleColor(ImGuiCol.Button,  // FIXED: use ImGuiCol.Button
                                ImGui.colorConvertFloat4ToU32(0.7f, 0.7f, 0.2f, 1.0f));
                    }
                    
                    if (ImGui.button(String.valueOf(i), 30, 30)) {
                        selectedNumber = i;
                    }
                    
                    if (selectedNumber == i) {
                        ImGui.popStyleColor();
                    }
                }
                ImGui.endTable();
            }
        } else {
            ImGui.text("Select Color:");
            ImGui.spacing();
            
            // Create color buttons
            float[][] colors = {
                {1.0f, 0.0f, 0.0f}, // Red
                {0.0f, 1.0f, 0.0f}, // Green
                {0.0f, 0.0f, 1.0f}, // Blue
                {1.0f, 1.0f, 0.0f}, // Yellow
                {1.0f, 0.0f, 1.0f}, // Purple
                {0.0f, 1.0f, 1.0f}, // Cyan
                {0.5f, 0.5f, 0.5f}, // Gray
                {1.0f, 0.5f, 0.0f}, // Orange
                {0.0f, 0.0f, 0.0f}  // Black
            };
            
            if (ImGui.beginTable("ColorsTable", 3)) {
                for (int i = 0; i < colors.length; i++) {
                    if (i > 0 && i % 3 == 0) {
                        ImGui.tableNextRow();  // FIXED: use tableNextRow()
                    }
                    
                    ImGui.tableNextColumn();
                    
                    // Use a colored rectangle as a button
                    ImGui.pushStyleColor(ImGuiCol.Button,  // FIXED: use ImGuiCol.Button
                            ImGui.colorConvertFloat4ToU32(colors[i][0], colors[i][1], colors[i][2], 1.0f));
                    
                    String buttonId = "##ColorBtn" + i;
                    if (ImGui.button(buttonId, 30, 30)) {
                        selectedColor = colors[i];
                    }
                    
                    // Add outline for selected color
                    if (selectedColor[0] == colors[i][0] && 
                        selectedColor[1] == colors[i][1] && 
                        selectedColor[2] == colors[i][2]) {
                        ImVec2 pos = ImGui.getItemRectMin();
                        ImVec2 size = ImGui.getItemRectSize();
                        ImGui.getWindowDrawList().addRect(
                            pos.x, pos.y, 
                            pos.x + size.x, pos.y + size.y, 
                            ImGui.colorConvertFloat4ToU32(1.0f, 1.0f, 1.0f, 1.0f), 
                            0, 0, 2.0f
                        );
                    }
                    
                    ImGui.popStyleColor();
                }
                ImGui.endTable();
            }
        }
        
        ImGui.endChild();
        
        // Right panel - Grids
        float rightPanelX = leftPanelWidth + 20;
        float rightPanelWidth = windowWidth - rightPanelX - 20;
        float rightPanelHeight = windowHeight - 100;
        
        ImGui.setCursorPos(rightPanelX, 50);
        ImGui.beginChild("RightPanel", rightPanelWidth, rightPanelHeight, false);
        
        // Grid editing controls
        float centerX = rightPanelWidth / 2;
        
        // Toggle button for grid/target
        ImGui.setCursorPosX(centerX - 100);
        if (ImGui.button(editingTarget ? "Editing: Target Pattern" : "Editing: Initial Grid", 
                200, 30)) {
            editingTarget = !editingTarget;
        }
        
        ImGui.spacing();
        ImGui.spacing();
        
        // Calculate grid positions
        float totalGridSize = gridSize * CELL_SIZE;
        float initialGridX = centerX - totalGridSize - GRID_SPACING;
        float targetGridX = centerX + GRID_SPACING;
        float gridY = 100;
        
        // Initial Grid label
        ImGui.setCursorPos(initialGridX, gridY - 30);
        ImGui.text("Initial Grid");
        
        // Target Grid label
        ImGui.setCursorPos(targetGridX, gridY - 30);
        ImGui.text("Target Pattern");
        
        // Draw both grids
        drawGrid(grid, initialGridX, gridY, !editingTarget);
        drawGrid(targetPattern, targetGridX, gridY, editingTarget);
        
        ImGui.endChild();
        
        // Bottom buttons
        ImGui.setCursorPos(20, windowHeight - 60);
        if (ImGui.button("Back", 100, 40)) {
            app.setCurrentScreen(new OnlineLevelSelectScreen(app));
        }
        
        ImGui.setCursorPos(windowWidth - 120, windowHeight - 60);
        if (ImGui.button("Save Level", 100, 40)) {
            saveLevelToDatabase();
        }
        
        ImGui.end();
    }
    
    private void drawGrid(LevelConfig.Cell[][] gridToRender, float startX, float startY, boolean isActive) {
        ImVec2 screenPos = ImGui.getCursorScreenPos();
        float gridTotalSize = CELL_SIZE * gridSize;
        
        // Add grid background
        ImGui.getWindowDrawList().addRectFilled(
            startX - 5, startY - 5,
            startX + gridTotalSize + 5, startY + gridTotalSize + 5,
            ImGui.colorConvertFloat4ToU32(0.2f, 0.2f, 0.2f, 1.0f)
        );
        
        // Draw each cell
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                float x = startX + col * CELL_SIZE;
                float y = startY + row * CELL_SIZE;
                LevelConfig.Cell cell = gridToRender[row][col];
                
                // Cell background color
                int cellColor;
                if (isNumberMode) {
                    // Gray background for number mode
                    cellColor = ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1.0f);
                } else {
                    // Use cell's color
                    cellColor = ImGui.colorConvertFloat4ToU32(cell.red, cell.green, cell.blue, 1.0f);
                }
                
                // Draw cell background
                ImGui.getWindowDrawList().addRectFilled(
                    x, y, 
                    x + CELL_SIZE - 2, y + CELL_SIZE - 2,
                    cellColor
                );
                
                // Draw cell border
                ImGui.getWindowDrawList().addRect(
                    x, y,
                    x + CELL_SIZE - 2, y + CELL_SIZE - 2,
                    ImGui.colorConvertFloat4ToU32(0.5f, 0.5f, 0.5f, 1.0f)
                );
                
                // Draw number if in number mode
                if (isNumberMode) {
                    int number = (int)(cell.red * 9);
                    if (number > 0) {
                        String numText = String.valueOf(number);
                        float textWidth = ImGui.calcTextSize(numText).x;
                        float textX = x + (CELL_SIZE - textWidth) / 2 - 1;
                        float textY = y + (CELL_SIZE - ImGui.getTextLineHeight()) / 2;
                        
                        ImGui.getWindowDrawList().addText(
                            textX, textY,
                            ImGui.colorConvertFloat4ToU32(1.0f, 1.0f, 1.0f, 1.0f),
                            numText
                        );
                    }
                }
                
                // Make each cell clickable with invisible button
                ImGui.setCursorPos(x - screenPos.x, y - screenPos.y);
                String cellId = "##Cell_" + row + "_" + col + "_" + (isActive ? "active" : "inactive");
                if (ImGui.invisibleButton(cellId, CELL_SIZE - 2, CELL_SIZE - 2) && isActive) {
                    // Apply selected color or number to cell when clicked
                    if (isNumberMode) {
                        gridToRender[row][col] = new LevelConfig.Cell(
                            selectedNumber / 9f, 0f, 0f, true
                        );
                    } else {
                        gridToRender[row][col] = new LevelConfig.Cell(
                            selectedColor[0], selectedColor[1], selectedColor[2], true
                        );
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        // ImGui handles clicks automatically
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        // ImGui handles hover states automatically
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // Not needed for ImGui
    }
    
    @Override
    public void handleKeyPress(int key, int action) {
        // Not needed for ImGui
    }
}