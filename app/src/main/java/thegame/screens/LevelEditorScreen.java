package thegame.screens;

import static org.lwjgl.opengl.GL11.*;

import org.bson.Document;
import com.mongodb.client.MongoCollection;

import thegame.App;
import thegame.Screen;
import thegame.onScreenObjects.Button;
import thegame.utils.FontRenderer;
import thegame.utils.LevelConfig;
import thegame.utils.MongoDBConnection;

import java.util.ArrayList;
import java.util.List;

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
    
    // Grid positioning
    private static final float CELL_SIZE = 40.0f;
    private static final float GRID_SPACING = 20.0f;
    
    // UI elements
    private FontRenderer fontRenderer;
    private Button backButton;
    private Button saveButton;
    private Button toggleModeButton;
    private Button toggleEditButton;
    private Button[] gridSizeButtons = new Button[2];
    private Button[] maxMovesButtons = new Button[2];
    private Button[][] colorButtons;
    private Button[] numberButtons;
    
    // Mouse tracking
    private double currentMouseX = 0, currentMouseY = 0;

    private String levelId; // Store the ID of the level being edited
    private boolean isEditingExistingLevel = false;
    private LevelConfig levelConfig; // Add field to store original LevelConfig
    
    // Constructor for creating a new level
    public LevelEditorScreen(App app) {
        this.app = app;
        initializeGrids();
        initUI();
    }
    
    // New constructor for editing an existing level
    public LevelEditorScreen(App app, LevelConfig existingLevel) {
        this.app = app;
        this.isEditingExistingLevel = true;
        this.levelId = "level" + existingLevel.getLevelNumber();
        this.levelConfig = existingLevel; // Store the original level config
        
        // Set the grid size from the existing level
        this.gridSize = existingLevel.getSettings().getGridSize();
        this.maxMoves = existingLevel.getSettings().getMaxMoves();
        this.isNumberMode = existingLevel.getSettings().isNumberMode();
        
        // Copy grid and target pattern from existing level
        initializeGrids();
        copyLevelData(existingLevel);
        
        initUI();
    }
    
    private void copyLevelData(LevelConfig existingLevel) {
        // Copy the grid data
        LevelConfig.Cell[][] sourceGrid = existingLevel.getGrid();
        LevelConfig.Cell[][] sourceTarget = existingLevel.getTargetPattern();
        
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (sourceGrid != null && i < sourceGrid.length && j < sourceGrid[i].length) {
                    LevelConfig.Cell cell = sourceGrid[i][j];
                    grid[i][j] = new LevelConfig.Cell(cell.red, cell.green, cell.blue, cell.editable);
                }
                
                if (sourceTarget != null && i < sourceTarget.length && j < sourceTarget[i].length) {
                    LevelConfig.Cell cell = sourceTarget[i][j];
                    targetPattern[i][j] = new LevelConfig.Cell(cell.red, cell.green, cell.blue, cell.editable);
                }
            }
        }
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
    
    private void initUI() {
        // Initialize font renderer
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("F:/temp/theGame/untitledGame/app/src/main/resources/fonts/pf_tempesta_seven_bold.ttf");
        
        // Bottom buttons
        backButton = new Button(20, App.WINDOW_HEIGHT - 60, 100, 40, 0.3f, 0.3f, 0.6f, "Back");
        saveButton = new Button(App.WINDOW_WIDTH - 120, App.WINDOW_HEIGHT - 60, 100, 40, 0.2f, 0.6f, 0.2f, "Save Level");
        
        // Control buttons
        float leftPanelWidth = App.WINDOW_WIDTH * 0.2f;
        toggleModeButton = new Button(20, 180, leftPanelWidth - 40, 30, 0.4f, 0.4f, 0.7f, 
                                     isNumberMode ? "Switch to Colors" : "Switch to Numbers");
        
        toggleEditButton = new Button(App.WINDOW_WIDTH / 2 - 100, 90, 200, 30, 0.4f, 0.4f, 0.7f,
                                     editingTarget ? "Editing: Target Pattern" : "Editing: Initial Grid");
        
        // Grid size buttons
        gridSizeButtons[0] = new Button(150, 80, 20, 20, 0.6f, 0.3f, 0.3f, "-");
        gridSizeButtons[1] = new Button(175, 80, 20, 20, 0.3f, 0.6f, 0.3f, "+");
        
        // Max moves buttons
        maxMovesButtons[0] = new Button(150, 110, 20, 20, 0.6f, 0.3f, 0.3f, "-");
        maxMovesButtons[1] = new Button(175, 110, 20, 20, 0.3f, 0.6f, 0.3f, "+");
        
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
        
        colorButtons = new Button[3][3];
        float colorStartX = 20;
        float colorStartY = 220;
        float colorButtonSize = 30;
        float colorSpacing = 10;
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                float x = colorStartX + col * (colorButtonSize + colorSpacing);
                float y = colorStartY + row * (colorButtonSize + colorSpacing);
                
                float r = colors[index][0];
                float g = colors[index][1];
                float b = colors[index][2];
                
                colorButtons[row][col] = new Button(x, y, colorButtonSize, colorButtonSize, r, g, b, "");
            }
        }
        
        // Create number buttons
        numberButtons = new Button[10];
        float numberStartX = 20;
        float numberStartY = 220;
        float numberButtonSize = 30;
        float numberSpacing = 10;
        
        for (int i = 0; i < 10; i++) {
            int row = i / 3;
            int col = i % 3;
            float x = numberStartX + col * (numberButtonSize + numberSpacing);
            float y = numberStartY + row * (numberButtonSize + numberSpacing);
            
            numberButtons[i] = new Button(x, y, numberButtonSize, numberButtonSize, 0.3f, 0.3f, 0.3f, String.valueOf(i));
        }
        
        // Update save button text if editing existing level
        if (isEditingExistingLevel) {
            saveButton = new Button(App.WINDOW_WIDTH - 120, App.WINDOW_HEIGHT - 60, 100, 40, 0.2f, 0.6f, 0.2f, "Update Level");
        }
    }

    private void saveLevelToDatabase() {
        try {
            // When editing, preserve the existing level ID and key
            int numericId;
            String levelKey = null;
            String levelName;
            
            if (isEditingExistingLevel) {
                // Extract or preserve the numeric ID
                if (levelId != null && levelId.matches(".*\\d+.*")) {
                    String numericPart = levelId.replaceAll("[^0-9]", "");
                    numericId = Integer.parseInt(numericPart);
                    levelKey = levelId; // Keep the original key for database storage
                } else {
                    // Fallback if ID extraction fails
                    numericId = (int)(System.currentTimeMillis() % 100000000);
                    levelKey = "level" + numericId;
                }
                
                // Preserve the original level name when editing
                levelName = levelConfig.getName() != null ? levelConfig.getName() : "Updated Level";
            } else {
                // Generate new ID for new level
                numericId = (int)(System.currentTimeMillis() % 100000000);
                levelKey = "level" + numericId;
                levelName = "Custom Level";
            }
            
            // Create LevelConfig object with preserved identity when editing
            LevelConfig level = new LevelConfig.Builder()
                .withId(numericId)
                .withName(levelName)
                .withGrid(grid)
                .withTargetPattern(targetPattern)
                .withSettings(new LevelConfig.Settings(gridSize, maxMoves, "custom", isNumberMode))
                .withCreator(app.getUsername() != null ? app.getUsername() : "anonymous")
                .withDescription(isEditingExistingLevel && levelConfig.getDescription() != null ? 
                                 levelConfig.getDescription() : "Custom created level")
                .withNumberMode(isNumberMode)
                .build();

            // Convert to MongoDB document
            Document levelDoc = level.toDocument();
            levelDoc.append("id", String.valueOf(numericId));

            try (MongoDBConnection mongodb = new MongoDBConnection()) {
                if (app.isLoggedIn() && app.getUserData() != null) {
                    // User is logged in, save/update under their profile
                    String username = app.getUsername();
                    Document user = app.getUserData();
                    
                    System.out.println((isEditingExistingLevel ? "Updating" : "Saving") + " level for user: " + username);
                    
                    // Get or create Levels subdocument
                    Document levelsDoc = user.get("Levels", Document.class);
                    if (levelsDoc == null) {
                        levelsDoc = new Document();
                        System.out.println("Creating new Levels document for user");
                    }
                    
                    // Add/update the level using the preserved key
                    levelsDoc.put(levelKey, levelDoc);
                    System.out.println((isEditingExistingLevel ? "Updated" : "Added") + " level " + levelKey + " to Levels document");
                    
                    // Update user document
                    Document filter = new Document("username", username);
                    Document update = new Document("$set", new Document("Levels", levelsDoc));
                    
                    mongodb.getLevelsCollection().updateOne(filter, update);
                    System.out.println("Level " + (isEditingExistingLevel ? "updated" : "saved") + " under user: " + username);
                    
                    // Update cached user data
                    app.setLoggedInUser(mongodb.getLevelsCollection().find(filter).first());
                } else {
                    // Fallback - save as standalone level
                    if (isEditingExistingLevel) {
                        mongodb.getLevelsCollection().replaceOne(
                            new Document("id", String.valueOf(numericId)),
                            levelDoc
                        );
                    } else {
                        mongodb.getLevelsCollection().insertOne(levelDoc);
                    }
                    System.out.println("Level " + (isEditingExistingLevel ? "updated" : "saved") + " as standalone (not logged in)");
                }
                
                // Return to online level screen with "My Levels" tab active
                app.setCurrentScreen(new OnlineLevelSelectScreen(app, true));
            }
        } catch (Exception e) {
            System.err.println("Error saving level: " + e.getMessage());
            e.printStackTrace();
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
        glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Title
        fontRenderer.renderCenteredText("Level Editor", App.WINDOW_WIDTH / 2, 20, 2.0f);
        
        // Draw left panel background
        float leftPanelWidth = App.WINDOW_WIDTH * 0.2f;
        float leftPanelHeight = App.WINDOW_HEIGHT - 100;
        glColor4f(0.15f, 0.15f, 0.25f, 1.0f);
        glBegin(GL_QUADS);
        glVertex2f(10, 50);
        glVertex2f(10 + leftPanelWidth, 50);
        glVertex2f(10 + leftPanelWidth, 50 + leftPanelHeight);
        glVertex2f(10, 50 + leftPanelHeight);
        glEnd();
        
        // Render settings section
        fontRenderer.renderText("Level Settings", 20, 60, 1.2f);
        
        // Grid size control
        fontRenderer.renderText("Grid Size: " + gridSize, 20, 85, 1.0f);
        gridSizeButtons[0].render((float)currentMouseX, (float)currentMouseY);
        gridSizeButtons[1].render((float)currentMouseX, (float)currentMouseY);
        
        // Max moves control
        fontRenderer.renderText("Max Moves: " + maxMoves, 20, 115, 1.0f);
        maxMovesButtons[0].render((float)currentMouseX, (float)currentMouseY);
        maxMovesButtons[1].render((float)currentMouseX, (float)currentMouseY);
        
        // Mode toggle button
        toggleModeButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Color/Number selector
        if (isNumberMode) {
            fontRenderer.renderText("Select Number:", 20, 200, 1.0f);
            for (int i = 0; i < numberButtons.length; i++) {
                if (selectedNumber == i) {
                    // Highlight selected number
                    numberButtons[i].setCaptionColor(1.0f, 1.0f, 0.0f, 1.0f);
                } else {
                    numberButtons[i].setCaptionColor(1.0f, 1.0f, 1.0f, 1.0f);
                }
                numberButtons[i].render((float)currentMouseX, (float)currentMouseY);
            }
        } else {
            fontRenderer.renderText("Select Color:", 20, 200, 1.0f);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    Button button = colorButtons[row][col];
                    button.render((float)currentMouseX, (float)currentMouseY);
                    
                    // Draw selection outline if this is the selected color
                    int index = row * 3 + col;
                    float[] color = {
                        colorButtons[row][col].getR(),
                        colorButtons[row][col].getG(),
                        colorButtons[row][col].getB()
                    };
                    
                    if (color[0] == selectedColor[0] && 
                        color[1] == selectedColor[1] && 
                        color[2] == selectedColor[2]) {
                        // Draw outline around selected color
                        glColor3f(1.0f, 1.0f, 1.0f);
                        glLineWidth(2.0f);
                        glBegin(GL_LINE_LOOP);
                        glVertex2f(button.getX() - 2, button.getY() - 2);
                        glVertex2f(button.getX() + button.getWidth() + 2, button.getY() - 2);
                        glVertex2f(button.getX() + button.getWidth() + 2, button.getY() + button.getHeight() + 2);
                        glVertex2f(button.getX() - 2, button.getY() + button.getHeight() + 2);
                        glEnd();
                        glLineWidth(1.0f);
                    }
                }
            }
        }
        
        // Right panel - Grids
        float rightPanelX = leftPanelWidth + 20;
        
        // Toggle button for grid/target
        toggleEditButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Calculate grid positions
        float totalGridSize = gridSize * CELL_SIZE;
        float centerX = (App.WINDOW_WIDTH + leftPanelWidth) / 2;
        float initialGridX = centerX - totalGridSize - GRID_SPACING;
        float targetGridX = centerX + GRID_SPACING;
        float gridY = 150;
        
        // Draw labels
        fontRenderer.renderCenteredText("Initial Grid", initialGridX + totalGridSize/2, gridY - 30, 1.2f);
        fontRenderer.renderCenteredText("Target Pattern", targetGridX + totalGridSize/2, gridY - 30, 1.2f);
        
        // Draw both grids
        drawGrid(grid, initialGridX, gridY, !editingTarget);
        drawGrid(targetPattern, targetGridX, gridY, editingTarget);
        
        // Render bottom buttons
        backButton.render((float)currentMouseX, (float)currentMouseY);
        saveButton.render((float)currentMouseX, (float)currentMouseY);
    }
    
    private void drawGrid(LevelConfig.Cell[][] gridToRender, float startX, float startY, boolean isActive) {
        float gridTotalSize = CELL_SIZE * gridSize;
        
        // Draw grid background
        glColor3f(0.2f, 0.2f, 0.2f);
        glBegin(GL_QUADS);
        glVertex2f(startX - 5, startY - 5);
        glVertex2f(startX + gridTotalSize + 5, startY - 5);
        glVertex2f(startX + gridTotalSize + 5, startY + gridTotalSize + 5);
        glVertex2f(startX - 5, startY + gridTotalSize + 5);
        glEnd();
        
        // Draw highlight around active grid
        if (isActive) {
            glColor3f(0.9f, 0.9f, 0.3f);
            glLineWidth(2.0f);
            glBegin(GL_LINE_LOOP);
            glVertex2f(startX - 8, startY - 8);
            glVertex2f(startX + gridTotalSize + 8, startY - 8);
            glVertex2f(startX + gridTotalSize + 8, startY + gridTotalSize + 8);
            glVertex2f(startX - 8, startY + gridTotalSize + 8);
            glEnd();
            glLineWidth(1.0f);
        }
        
        // Draw each cell
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                float x = startX + col * CELL_SIZE;
                float y = startY + row * CELL_SIZE;
                LevelConfig.Cell cell = gridToRender[row][col];
                
                // Cell background color
                if (isNumberMode) {
                    // Gray background for number mode
                    glColor3f(0.3f, 0.3f, 0.3f);
                } else {
                    // Use cell's color
                    glColor3f(cell.red, cell.green, cell.blue);
                }
                
                // Draw cell
                glBegin(GL_QUADS);
                glVertex2f(x, y);
                glVertex2f(x + CELL_SIZE - 2, y);
                glVertex2f(x + CELL_SIZE - 2, y + CELL_SIZE - 2);
                glVertex2f(x, y + CELL_SIZE - 2);
                glEnd();
                
                // Draw cell border
                glColor3f(0.5f, 0.5f, 0.5f);
                glLineWidth(1.0f);
                glBegin(GL_LINE_LOOP);
                glVertex2f(x, y);
                glVertex2f(x + CELL_SIZE - 2, y);
                glVertex2f(x + CELL_SIZE - 2, y + CELL_SIZE - 2);
                glVertex2f(x, y + CELL_SIZE - 2);
                glEnd();
                
                // Draw number if in number mode
                if (isNumberMode) {
                    int number = (int)(cell.red * 9);
                    if (number > 0) {
                        fontRenderer.renderCenteredText(String.valueOf(number), 
                                                      x + CELL_SIZE/2, 
                                                      y + CELL_SIZE/2, 
                                                      1.0f);
                    }
                }
                
                // Check if clicked
                if (isActive && 
                    currentMouseX >= x && currentMouseX < x + CELL_SIZE && 
                    currentMouseY >= y && currentMouseY < y + CELL_SIZE) {
                    // Show hover effect
                    glColor4f(1.0f, 1.0f, 1.0f, 0.3f);
                    glBegin(GL_QUADS);
                    glVertex2f(x, y);
                    glVertex2f(x + CELL_SIZE - 2, y);
                    glVertex2f(x + CELL_SIZE - 2, y + CELL_SIZE - 2);
                    glVertex2f(x, y + CELL_SIZE - 2);
                    glEnd();
                }
            }
        }
    }
    
    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
        // Check bottom buttons
        if (backButton.handleMouseClick(mx, my)) {
            app.setCurrentScreen(new OnlineLevelSelectScreen(app));
            return;
        }
        
        if (saveButton.handleMouseClick(mx, my)) {
            saveLevelToDatabase();
            return;
        }
        
        // Check toggle mode button
        if (toggleModeButton.handleMouseClick(mx, my)) {
            isNumberMode = !isNumberMode;
            toggleModeButton.setCaption(isNumberMode ? "Switch to Colors" : "Switch to Numbers");
            return;
        }
        
        // Check toggle edit button
        if (toggleEditButton.handleMouseClick(mx, my)) {
            editingTarget = !editingTarget;
            toggleEditButton.setCaption(editingTarget ? "Editing: Target Pattern" : "Editing: Initial Grid");
            return;
        }
        
        // Check grid size buttons
        if (gridSizeButtons[0].handleMouseClick(mx, my) && gridSize > 2) {
            gridSize--;
            initializeGrids();
            return;
        }
        
        if (gridSizeButtons[1].handleMouseClick(mx, my) && gridSize < 6) {
            gridSize++;
            initializeGrids();
            return;
        }
        
        // Check max moves buttons
        if (maxMovesButtons[0].handleMouseClick(mx, my) && maxMoves > 1) {
            maxMoves--;
            return;
        }
        
        if (maxMovesButtons[1].handleMouseClick(mx, my)) {
            maxMoves++;
            return;
        }
        
        // Check color/number selection
        if (isNumberMode) {
            for (int i = 0; i < numberButtons.length; i++) {
                if (numberButtons[i].handleMouseClick(mx, my)) {
                    selectedNumber = i;
                    selectedColor[0] = i / 9f;
                    selectedColor[1] = 0f;
                    selectedColor[2] = 0f;
                    return;
                }
            }
        } else {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (colorButtons[row][col].handleMouseClick(mx, my)) {
                        selectedColor[0] = colorButtons[row][col].getR();
                        selectedColor[1] = colorButtons[row][col].getG();
                        selectedColor[2] = colorButtons[row][col].getB();
                        return;
                    }
                }
            }
        }
        
        // Check grid cell clicks
        LevelConfig.Cell[][] activeGrid = editingTarget ? targetPattern : grid;
        float gridTotalSize = CELL_SIZE * gridSize;
        float centerX = (App.WINDOW_WIDTH + App.WINDOW_WIDTH * 0.2f) / 2;
        float initialGridX = centerX - gridTotalSize - GRID_SPACING;
        float targetGridX = centerX + GRID_SPACING;
        float gridY = 150;
        
        float activeGridX = editingTarget ? targetGridX : initialGridX;
        
        // Check if click is within active grid
        if (mouseX >= activeGridX && mouseX < activeGridX + gridSize * CELL_SIZE &&
            mouseY >= gridY && mouseY < gridY + gridSize * CELL_SIZE) {
            
            int col = (int)((mouseX - activeGridX) / CELL_SIZE);
            int row = (int)((mouseY - gridY) / CELL_SIZE);
            
            // Apply selected color/number to cell
            if (isNumberMode) {
                activeGrid[row][col] = new LevelConfig.Cell(selectedNumber / 9f, 0f, 0f, true);
            } else {
                activeGrid[row][col] = new LevelConfig.Cell(
                    selectedColor[0], selectedColor[1], selectedColor[2], true
                );
            }       
        }
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // Not needed
    }

    @Override
    public void handleKeyPress(int key, int action) {
        // Not needed
    }

    @Override
    public void handleCharInput(int codepoint) {
        // Not needed for level select as it has no text input fields
    }
}