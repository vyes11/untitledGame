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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen for creating and editing game levels.
 * Allows users to design levels, set parameters, and manage verification.
 */
public class LevelEditorScreen implements Screen {
    private final App app;
    private LevelConfig.Cell[][] grid;
    private LevelConfig.Cell[][] targetPattern;
    private int gridSize = 2; // Start with 2x2
    private int maxMoves = 3;
    
    // Make these static to persist across screen transitions
    private static LevelConfig staticPendingLevel = null;
    private static String staticPendingLevelKey = null;

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
    
    // UI elements for move limits
    private Map<String, Integer> moveLimits = new HashMap<>();
    private Button expandMoveLimitsButton;
    private boolean showingMoveLimits = false;
    private Map<String, Button[]> moveLimitButtons = new HashMap<>();
    private String[] moveTypes = {
        "SWAP", "FLIP_ROW", "FLIP_COLUMN", "ROTATE",  // Basic moves
        "ADD", "SUBTRACT", "MULTIPLY", "DIVIDE"        // Math operations
    };
    
    // Mouse tracking
    private double currentMouseX = 0, currentMouseY = 0;

    private String levelId; // Store the ID of the level being edited
    private boolean isEditingExistingLevel = false;
    private LevelConfig levelConfig; // Add field to store original LevelConfig
    
    // Text input for level name
    private String levelNameInput = "Custom Level";
    private boolean isEditingName = false;
    private boolean showCursor = false;
    private long lastCursorBlink = 0;
    
    // Success dialog
    private boolean showSaveSuccess = false;
    private String savedLevelId = "";
    private Button continueButton;
    
    // Verification dialog
    private boolean showVerificationDialog = false;
    private Button verifyButton;
    private Button skipVerificationButton;
    private LevelConfig pendingLevel;
    private String pendingLevelKey;

    /**
     * Constructs a new LevelEditorScreen for creating a new level.
     * 
     * @param app The main application instance
     */
    public LevelEditorScreen(App app) {
        this.app = app;
        initializeGrids();
        initUI();
    }
    
    /**
     * Constructs a new LevelEditorScreen for editing an existing level.
     * 
     * @param app The main application instance
     * @param existingLevel The existing level to edit
     */
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
    
    /**
     * Copies data from an existing level to the editor grids.
     * 
     * @param existingLevel The level to copy data from
     */
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
    
    /**
     * Initializes the grid and target pattern arrays.
     */
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
    
    /**
     * Initializes the user interface elements.
     */
    private void initUI() {
        // Initialize font renderer
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("/fonts/pf_tempesta_seven_bold.ttf");
        
        // Bottom buttons
        backButton = new Button(20, App.WINDOW_HEIGHT - 60, 100, 40, 0.7f, 0.3f, 0.6f, "Back"); // Dark pink
        saveButton = new Button(App.WINDOW_WIDTH - 120, App.WINDOW_HEIGHT - 60, 100, 40, 0.8f, 0.4f, 0.7f, "Save Level"); // Hot pink
        
        // Control buttons with pink theme
        float leftPanelWidth = App.WINDOW_WIDTH * 0.2f;
        toggleModeButton = new Button(20, 180, leftPanelWidth - 40, 30, 0.9f, 0.5f, 0.8f, // Secondary pink
                                     isNumberMode ? "Switch to Colors" : "Switch to Numbers");
        
        toggleEditButton = new Button(App.WINDOW_WIDTH / 2 - 100, 90, 200, 30, 0.8f, 0.4f, 0.7f, // Hot pink
                                     editingTarget ? "Editing: Target Pattern" : "Editing: Initial Grid");
        
        // Grid size buttons with pink theme
        gridSizeButtons[0] = new Button(200, 80, 20, 20, 0.7f, 0.3f, 0.6f, "-"); // Dark pink
        gridSizeButtons[1] = new Button(225, 80, 20, 20, 0.9f, 0.5f, 0.8f, "+"); // Secondary pink
        
        // Max moves buttons with pink theme
        maxMovesButtons[0] = new Button(200, 110, 20, 20, 0.7f, 0.3f, 0.6f, "-"); // Dark pink
        maxMovesButtons[1] = new Button(225, 110, 20, 20, 0.9f, 0.5f, 0.8f, "+"); // Secondary pink
        
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
        float colorStartY = 250; // Changed from 220 to 250
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
        float numberStartY = 250; // Changed from 220 to 250
        float numberButtonSize = 30;
        float numberSpacing = 10;
        
        for (int i = 0; i < 10; i++) {
            int row = i / 3;
            int col = i % 3;
            float x = numberStartX + col * (numberButtonSize + numberSpacing);
            float y = numberStartY + row * (numberButtonSize + numberSpacing);
            
            numberButtons[i] = new Button(x, y, numberButtonSize, numberButtonSize, 0.3f, 0.3f, 0.3f, String.valueOf(i));
        }
        
        // Initialize move limits with default values
        for (String moveType : moveTypes) {
            moveLimits.put(moveType, 5); // Default 5 per move type
        }
        
        // Initialize expand button for move limits
        expandMoveLimitsButton = new Button(20, 140, leftPanelWidth - 40, 30, 0.8f, 0.4f, 0.7f, // Hot pink
                                         "Expand Move Limits");
        
        // Create buttons for each move limit with pink theme
        float moveButtonY = 180;
        for (String moveType : moveTypes) {
            Button[] buttons = new Button[2];
            buttons[0] = new Button(200, moveButtonY, 20, 20, 0.7f, 0.3f, 0.6f, "-"); // Dark pink
            buttons[1] = new Button(225, moveButtonY, 20, 20, 0.9f, 0.5f, 0.8f, "+"); // Secondary pink
            moveLimitButtons.put(moveType, buttons);
            moveButtonY += 30; // Space between move type controls
        }
        
        // If editing an existing level, load its move limits
        if (isEditingExistingLevel && levelConfig != null) {
            Map<String, Integer> existingLimits = levelConfig.getSettings().getMoveLimits();
            if (existingLimits != null) {
                for (Map.Entry<String, Integer> entry : existingLimits.entrySet()) {
                    moveLimits.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        // Update save button text if editing existing level
        if (isEditingExistingLevel) {
            saveButton = new Button(App.WINDOW_WIDTH - 120, App.WINDOW_HEIGHT - 60, 100, 40, 0.2f, 0.6f, 0.2f, "Update Level");
        }
        
        // Initialize name input with existing name if editing
        if (isEditingExistingLevel && levelConfig != null && levelConfig.getName() != null) {
            levelNameInput = levelConfig.getName();
        }
        
        // Initialize continue button for success dialog
        continueButton = new Button(App.WINDOW_WIDTH / 2 - 60, App.WINDOW_HEIGHT / 2 + 50, 120, 40, 
                                  0.9f, 0.5f, 0.8f, "Continue"); // Secondary pink
    
        // Initialize verification dialog buttons
        verifyButton = new Button(App.WINDOW_WIDTH / 2 - 150, App.WINDOW_HEIGHT / 2 + 20, 
                                120, 40, 0.8f, 0.4f, 0.7f, "Verify Level"); // Hot pink
        skipVerificationButton = new Button(App.WINDOW_WIDTH / 2 + 30, App.WINDOW_HEIGHT / 2 + 20, 
                                        120, 40, 0.7f, 0.3f, 0.6f, "Skip Verification"); // Dark pink
    }

    /**
     * Saves the level to the database.
     */
    private void saveLevelToDatabase() {
        try {
            // When editing, preserve the existing level ID and key
            int numericId;
            String levelKey = null;
            String levelName = levelNameInput;
            
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
            } else {
                // Generate new ID for new level
                numericId = (int)(System.currentTimeMillis() % 100000000);
                levelKey = "level" + numericId;
            }
            
            // Create LevelConfig object with preserved identity when editing
            LevelConfig level = new LevelConfig.Builder()
                .withId(numericId)
                .withName(levelName)
                .withGrid(grid)
                .withTargetPattern(targetPattern)
                .withSettings(new LevelConfig.Settings(gridSize, moveLimits, "custom", isNumberMode))
                .withCreator(app.getUsername() != null ? app.getUsername() : "anonymous")
                .withDescription(isEditingExistingLevel && levelConfig.getDescription() != null ? 
                                 levelConfig.getDescription() : "Custom created level")
                .withNumberMode(isNumberMode)
                // Add statistics with initial values of 0
                .withStatistics(new LevelConfig.Statistics(0, 0))
                // Add verification status - assume verified if editing an existing level
                .withVerified(isEditingExistingLevel)
                .build();
            
            // Store level in instance variables
            pendingLevel = level;
            pendingLevelKey = levelKey;
            
            // IMPORTANT: Store in static variables too so they persist across screens
            staticPendingLevel = level;
            staticPendingLevelKey = levelKey;
            
            // Store level ID in App for later retrieval
            app.setMostRecentlyEditedLevelId(numericId);
            
            // Show verification dialog instead of saving immediately
            showVerificationDialog = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Finalizes the level save process with verification status.
     * 
     * @param verified Whether the level has been verified as solvable
     */
    private void finalizeLevel(boolean verified) {
        try {
            // Update verification status
            if (verified) {
                // Use reflection to set isVerified since we don't want to rebuild the level
                java.lang.reflect.Field field = pendingLevel.getClass().getDeclaredField("isVerified");
                field.setAccessible(true);
                field.set(pendingLevel, true);
            }
            
            // Convert to MongoDB document
            Document levelDoc = pendingLevel.toDocument();
            levelDoc.append("id", String.valueOf(pendingLevel.getLevelNumber()));
            levelDoc.append("isVerified", verified);
            
            try (MongoDBConnection mongodb = new MongoDBConnection()) {
                if (app.isLoggedIn() && app.getUserData() != null) {
                    // User is logged in, save/update under their profile
                    String username = app.getUsername();
                    Document user = app.getUserData();
                    
                    // Get or create Levels subdocument
                    Document levelsDoc = user.get("Levels", Document.class);
                    if (levelsDoc == null) {
                        levelsDoc = new Document();
                    }
                    
                    // Add/update the level using the preserved key
                    levelsDoc.put(pendingLevelKey, levelDoc);
                    
                    // Update user document
                    Document filter = new Document("username", username);
                    Document update = new Document("$set", new Document("Levels", levelsDoc));
                    
                    mongodb.getDatabase().getCollection("data").updateOne(filter, update);
                    
                    // Update cached user data
                    app.setLoggedInUser(mongodb.getDatabase().getCollection("data").find(filter).first());
                } else {
                    // Fallback - save as standalone level
                    if (isEditingExistingLevel) {
                        mongodb.getDatabase().getCollection("levels").replaceOne(
                            new Document("id", String.valueOf(pendingLevel.getLevelNumber())),
                            levelDoc
                        );
                    } else {
                        mongodb.getDatabase().getCollection("levels").insertOne(levelDoc);
                    }
                }
                
                // After successful save, store ID and show success dialog
                savedLevelId = String.valueOf(pendingLevel.getLevelNumber());
                showSaveSuccess = true;
                showVerificationDialog = false;
                
                // Don't return to level select screen yet - let user click continue
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the next available level ID from the database.
     * 
     * @return The next available level ID
     */
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
            return 1;
        }
    }

    /**
     * Renders the level editor screen.
     */
    @Override
    public void render() {
        glClearColor(1.0f, 0.7f, 0.9f, 1.0f); // Primary pink
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Title
        fontRenderer.renderCenteredText("Level Editor", App.WINDOW_WIDTH / 2 - 100, 20, 2.0f, 0.8f, 0.2f, 0.5f, 1.0f); // Pink-hued text
        
        // If showing save success dialog, render that and nothing else
        if (showSaveSuccess) {
            // Draw overlay
            glColor4f(0.9f, 0.5f, 0.8f, 0.7f); // Semi-transparent pink
            glBegin(GL_QUADS);
            glVertex2f(0, 0);
            glVertex2f(App.WINDOW_WIDTH, 0);
            glVertex2f(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
            glVertex2f(0, App.WINDOW_HEIGHT);
            glEnd();
            
            // Draw dialog box
            float boxWidth = 400;
            float boxHeight = 200;
            float boxX = App.WINDOW_WIDTH / 2 - boxWidth / 2;
            float boxY = App.WINDOW_HEIGHT / 2 - boxHeight / 2;
            
            glColor3f(0.8f, 0.4f, 0.7f); // Hot pink
            glBegin(GL_QUADS);
            glVertex2f(boxX, boxY);
            glVertex2f(boxX + boxWidth, boxY);
            glVertex2f(boxX + boxWidth, boxY + boxHeight);
            glVertex2f(boxX, boxY + boxHeight);
            glEnd();
            
            // Draw success message - properly centered
            fontRenderer.renderCenteredText("Level Saved Successfully!", 
                                          App.WINDOW_WIDTH / 2, boxY + 50, 1.4f);
            fontRenderer.renderCenteredText("Level ID: " + savedLevelId, 
                                          App.WINDOW_WIDTH / 2, boxY + 100, 1.2f);
            
            // Draw continue button
            continueButton.render((float)currentMouseX, (float)currentMouseY);
            return;
        }
        
        // If showing verification dialog, render that instead of success dialog
        if (showVerificationDialog) {
            // Draw overlay
            glColor4f(0.9f, 0.5f, 0.8f, 0.7f); // Semi-transparent pink
            glBegin(GL_QUADS);
            glVertex2f(0, 0);
            glVertex2f(App.WINDOW_WIDTH, 0);
            glVertex2f(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
            glVertex2f(0, App.WINDOW_HEIGHT);
            glEnd();
            
            // Draw dialog box
            float boxWidth = 400;
            float boxHeight = 200;
            float boxX = App.WINDOW_WIDTH / 2 - boxWidth / 2;
            float boxY = App.WINDOW_HEIGHT / 2 - boxHeight / 2;
            
            glColor3f(0.8f, 0.4f, 0.7f); // Hot pink
            glBegin(GL_QUADS);
            glVertex2f(boxX, boxY);
            glVertex2f(boxX + boxWidth, boxY);
            glVertex2f(boxX + boxWidth, boxY + boxHeight);
            glVertex2f(boxX, boxY + boxHeight);
            glEnd();
            
            // Draw message
            fontRenderer.renderCenteredText("Verify Your Level", 
                                         App.WINDOW_WIDTH / 2 - 130, boxY - 40, 1.4f);
            fontRenderer.renderCenteredText("Play through your level to verify it's solvable", 
                                         App.WINDOW_WIDTH / 2, boxY + 80, 1.0f);
            fontRenderer.renderCenteredText("or skip verification\n(level will be marked as unverified)", 
                                         App.WINDOW_WIDTH / 2, boxY + 110, 1.0f);
            
            // Draw buttons
            verifyButton.render((float)currentMouseX, (float)currentMouseY);
            skipVerificationButton.render((float)currentMouseX, (float)currentMouseY);
            
            return;
        }
        
        // Draw left panel background
        float leftPanelWidth = App.WINDOW_WIDTH * 0.2f;
        float leftPanelHeight = App.WINDOW_HEIGHT - 100;
        glColor4f(0.9f, 0.5f, 0.8f, 1.0f); // Secondary pink
        glBegin(GL_QUADS);
        glVertex2f(10, 50);
        glVertex2f(10 + leftPanelWidth, 50);
        glVertex2f(10 + leftPanelWidth, 50 + leftPanelHeight);
        glVertex2f(10, 50 + leftPanelHeight);
        glEnd();
        
        // Render settings section
        fontRenderer.renderText("Level Settings", 20, 40, 1.2f);
        
        // Grid size control
        fontRenderer.renderText("Grid Size: " + gridSize, 20, 85, 1.0f);
        gridSizeButtons[0].render((float)currentMouseX, (float)currentMouseY);
        gridSizeButtons[1].render((float)currentMouseX, (float)currentMouseY);
        
        // Max moves control
        fontRenderer.renderText("Max Moves: " + maxMoves, 20, 115, 1.0f);
        maxMovesButtons[0].render((float)currentMouseX, (float)currentMouseY);
        maxMovesButtons[1].render((float)currentMouseX, (float)currentMouseY);
        
        // Render move limits button with more prominent appearance
        expandMoveLimitsButton = new Button(20, 140, leftPanelWidth - 40, 30, 
                                 showingMoveLimits ? 0.5f : 0.4f, 
                                 showingMoveLimits ? 0.7f : 0.4f, 
                                 0.7f, 
                                 showingMoveLimits ? "Collapse Move Limits" : "Expand Move Limits");
        expandMoveLimitsButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Render move limits section if expanded, otherwise show color/number selectors
        if (showingMoveLimits) {
            // Draw background for move limits section to make it stand out
            float moveSection_width = leftPanelWidth - 20;
            float moveSection_height = moveTypes.length * 30 + 20;
            float moveSection_x = 15;
            float moveSection_y = 175;
            
            glColor3f(0.8f, 0.4f, 0.7f); // Hot pink
            glBegin(GL_QUADS);
            glVertex2f(moveSection_x, moveSection_y);
            glVertex2f(moveSection_x + moveSection_width, moveSection_y);
            glVertex2f(moveSection_x + moveSection_width, moveSection_y + moveSection_height);
            glVertex2f(moveSection_x, moveSection_y + moveSection_height);
            glEnd();
            
            // Render move limits controls
            float moveTextY = 185;
            for (String moveType : moveTypes) {
                // Format the move type name for display
                String prettyName = moveType.replace('_', ' ');
                int limit = moveLimits.getOrDefault(moveType, 5);
                
                fontRenderer.renderText(prettyName + ": " + limit, 25, moveTextY, 1.0f);
                
                // Render +/- buttons
                Button[] buttons = moveLimitButtons.get(moveType);
                if (buttons != null) {
                    buttons[0].render((float)currentMouseX, (float)currentMouseY);
                    buttons[1].render((float)currentMouseX, (float)currentMouseY);
                }
                
                moveTextY += 30;
            }
        } else {
            // Mode toggle button - only show when move limits are collapsed
            toggleModeButton.render((float)currentMouseX, (float)currentMouseY);
            
            // Only render color/number selector when move limits are not expanded
            if (isNumberMode) {
                fontRenderer.renderText("Select Number:", 20, 230, 1.0f); // Changed from 200 to 230
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
                fontRenderer.renderText("Select Color:", 20, 230, 1.0f); // Changed from 200 to 230
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
        float gridY = 220; // Changed from 150 to 220 (pushed down by 70 pixels)
        
        // Draw labels - moved 50 pixels left
        fontRenderer.renderCenteredText("Initial Grid", (initialGridX + totalGridSize/2) - 120, gridY - 70, 1.2f);
        fontRenderer.renderCenteredText("Target Pattern", (targetGridX + totalGridSize/2) - 80, gridY - 70, 1.2f);
        
        // Draw both grids
        drawGrid(grid, initialGridX, gridY, !editingTarget);
        drawGrid(targetPattern, targetGridX, gridY, editingTarget);
        
        // Render bottom buttons
        backButton.render((float)currentMouseX, (float)currentMouseY);
        saveButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Render level name input field
        float nameFieldX = App.WINDOW_WIDTH / 2 - 150;
        float nameFieldY = 50;
        float nameFieldWidth = 300;
        float nameFieldHeight = 30;
        
        // Draw name field background with border
        glColor3f(0.7f, 0.3f, 0.6f); // Dark pink
        glBegin(GL_QUADS);
        glVertex2f(nameFieldX, nameFieldY);
        glVertex2f(nameFieldX + nameFieldWidth, nameFieldY);
        glVertex2f(nameFieldX + nameFieldWidth, nameFieldY + nameFieldHeight);
        glVertex2f(nameFieldX, nameFieldY + nameFieldHeight);
        glEnd();
        
        // Draw border, highlight if active
        if (isEditingName) {
            glColor3f(1.0f, 0.4f, 0.7f); // Accent pink
        } else {
            glColor3f(0.8f, 0.4f, 0.7f); // Hot pink
        }
        
        glLineWidth(2.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(nameFieldX, nameFieldY);
        glVertex2f(nameFieldX + nameFieldWidth, nameFieldY);
        glVertex2f(nameFieldX + nameFieldWidth, nameFieldY + nameFieldHeight);
        glVertex2f(nameFieldX, nameFieldY + nameFieldHeight);
        glEnd();
        glLineWidth(1.0f);
        
        // Draw label
        fontRenderer.renderText("Level Name:", nameFieldX - 200, nameFieldY + 8, 1.0f);
        
        // Draw text with cursor if editing
        String displayText = levelNameInput;
        if (isEditingName) {
            // Handle cursor blinking
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCursorBlink > 500) {
                showCursor = !showCursor;
                lastCursorBlink = currentTime;
            }
            
            if (showCursor) {
                displayText += "|";
            }
        }
        
        fontRenderer.renderText(displayText, nameFieldX + 10, nameFieldY + 8, 1.0f);
        
        // Render success dialog if needed
        if (showSaveSuccess) {
            float dialogWidth = 300;
            float dialogHeight = 150;
            float dialogX = App.WINDOW_WIDTH / 2 - dialogWidth / 2;
            float dialogY = App.WINDOW_HEIGHT / 2 - dialogHeight / 2;
            
            // Dialog background
            glColor4f(0.1f, 0.1f, 0.1f, 0.9f);
            glBegin(GL_QUADS);
            glVertex2f(dialogX, dialogY);
            glVertex2f(dialogX + dialogWidth, dialogY);
            glVertex2f(dialogX + dialogWidth, dialogY + dialogHeight);
            glVertex2f(dialogX, dialogY + dialogHeight);
            glEnd();
            
            // Dialog text
            fontRenderer.renderCenteredText("Level Saved!", dialogX, dialogY + dialogHeight - 40, 1.2f);
            fontRenderer.renderCenteredText("Level ID: " + savedLevelId, dialogX, dialogY + dialogHeight - 70, 1.0f);
            
            // Continue button
            continueButton.render((float)currentMouseX, (float)currentMouseY);
        }
    }
    
    /**
     * Draws a grid for editing.
     * 
     * @param gridToRender The grid data to render
     * @param startX X coordinate of the top-left corner
     * @param startY Y coordinate of the top-left corner
     * @param isActive Whether this grid is being actively edited
     */
    private void drawGrid(LevelConfig.Cell[][] gridToRender, float startX, float startY, boolean isActive) {
        float gridTotalSize = CELL_SIZE * gridSize;
        
        // Draw grid background
        glColor3f(0.7f, 0.3f, 0.6f); // Dark pink
        glBegin(GL_QUADS);
        glVertex2f(startX - 5, startY - 5);
        glVertex2f(startX + gridTotalSize + 5, startY - 5);
        glVertex2f(startX + gridTotalSize + 5, startY + gridTotalSize + 5);
        glVertex2f(startX - 5, startY + gridTotalSize + 5);
        glEnd();
        
        // Draw highlight around active grid
        if (isActive) {
            glColor3f(1.0f, 0.4f, 0.7f); // Accent pink
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
    
    /**
     * Gets the pending level key.
     * 
     * @return The pending level key
     */
    public static String getPendingLevelKey() {
        return staticPendingLevelKey;
    }
    
    /**
     * Gets the pending level.
     * 
     * @return The pending level
     */
    public static LevelConfig getPendingLevel() {
        return staticPendingLevel;
    }
    
    /**
     * Handles mouse click events.
     * 
     * @param mouseX X coordinate of the mouse click
     * @param mouseY Y coordinate of the mouse click
     */
    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
        // Check verification dialog buttons first
        if (showVerificationDialog) {
            if (verifyButton.handleMouseClick(mx, my)) {
                // Save the level to DB first (unverified) before launching verification
                if (pendingLevel != null) {
                    // Save level to database with unverified status first
                    saveLevelBeforeVerification();
                    
                    // Ensure static variables are updated before transitioning
                    staticPendingLevel = pendingLevel;
                    staticPendingLevelKey = pendingLevelKey;
                    
                    // Update App's most recently edited level ID
                    app.setMostRecentlyEditedLevelId(pendingLevel.getLevelNumber());
                    
                    app.setCurrentScreen(new GameScreen(app, pendingLevel, true));
                } else {
                    System.err.println("ERROR: Pending level is null, cannot verify");
                }
                return;
            }
            
            if (skipVerificationButton.handleMouseClick(mx, my)) {
                // Save the level without verification
                finalizeLevel(false);
                return;
            }
            
            return; // Block other clicks while dialog is open
        }
        
        // If showing success dialog, only handle continue button
        if (showSaveSuccess) {
            if (continueButton.handleMouseClick(mx, my)) {
                // Go to the level select screen
                app.setCurrentScreen(new OnlineLevelSelectScreen(app, true));
            }
            return;
        }
        
        // Check for name field click
        float nameFieldX = App.WINDOW_WIDTH / 2 - 150;
        float nameFieldY = 50;
        float nameFieldWidth = 300;
        float nameFieldHeight = 30;
        
        if (mx >= nameFieldX && mx <= nameFieldX + nameFieldWidth &&
            my >= nameFieldY && my <= nameFieldY + nameFieldHeight) {
            isEditingName = true;
        } else {
            isEditingName = false;
        }
        
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
        
        // Add check for expand move limits button
        if (expandMoveLimitsButton.handleMouseClick(mx, my)) {
            showingMoveLimits = !showingMoveLimits;
            expandMoveLimitsButton.setCaption(showingMoveLimits ? "Collapse Move Limits" : "Expand Move Limits");
            return;
        }
        
        // Handle move limit buttons if they're visible
        if (showingMoveLimits) {
            for (String moveType : moveTypes) {
                Button[] buttons = moveLimitButtons.get(moveType);
                if (buttons != null) {
                    // Check minus button
                    if (buttons[0].handleMouseClick(mx, my)) {
                        int currentLimit = moveLimits.getOrDefault(moveType, 5);
                        if (currentLimit > 0) {
                            moveLimits.put(moveType, currentLimit - 1);
                        }
                        return;
                    }
                    
                    // Check plus button
                    if (buttons[1].handleMouseClick(mx, my)) {
                        int currentLimit = moveLimits.getOrDefault(moveType, 5);
                        moveLimits.put(moveType, currentLimit + 1);
                        return;
                    }
                }
            }
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
        float gridY = 220; // Changed from 150 to 220 (pushed down by 70 pixels to match render method)
        
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

    /**
     * Updates the current mouse position.
     * 
     * @param mouseX X coordinate of the mouse
     * @param mouseY Y coordinate of the mouse
     */
    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }

    /**
     * Handles mouse release events.
     * 
     * @param mouseX X coordinate of the mouse release
     * @param mouseY Y coordinate of the mouse release
     */
    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // Not needed
    }

    /**
     * Handles key press events.
     * 
     * @param key The key code
     * @param action The action (press, release, etc.)
     */
    @Override
    public void handleKeyPress(int key, int action) {
        if (isEditingName && action == 1) { // 1 = press
            if (key == 259) { // BACKSPACE
                if (levelNameInput.length() > 0) {
                    levelNameInput = levelNameInput.substring(0, levelNameInput.length() - 1);
                }
            } else if (key == 257) { // ENTER
                isEditingName = false;
            }
        }
    }

    /**
     * Handles character input events for the level name field.
     * 
     * @param codepoint The Unicode code point of the character
     */
    @Override
    public void handleCharInput(int codepoint) {
        if (isEditingName) {
            // Convert codepoint to character
            char c = (char)codepoint;
            
            // Only allow alphanumeric and some special characters
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
                // Don't let the name get too long
                if (levelNameInput.length() < 25) {
                    levelNameInput += c;
                }
            }
        }
    }
    
    /**
     * Saves the level to the database with unverified status before verification.
     */
    private void saveLevelBeforeVerification() {
        try {
            // Convert to MongoDB document
            Document levelDoc = pendingLevel.toDocument();
            levelDoc.append("id", String.valueOf(pendingLevel.getLevelNumber()));
            levelDoc.append("isVerified", false); // Initially unverified
            
            try (MongoDBConnection mongodb = new MongoDBConnection()) {
                if (app.isLoggedIn() && app.getUserData() != null) {
                    // User is logged in, save under their profile
                    String username = app.getUsername();
                    Document user = app.getUserData();
                    
                    // Get or create Levels subdocument
                    Document levelsDoc = user.get("Levels", Document.class);
                    if (levelsDoc == null) {
                        levelsDoc = new Document();
                    }
                    
                    // Add the level using the preserved key
                    levelsDoc.put(pendingLevelKey, levelDoc);
                    
                    // Update user document
                    Document filter = new Document("username", username);
                    Document update = new Document("$set", new Document("Levels", levelsDoc));
                    
                    mongodb.getDatabase().getCollection("data").updateOne(filter, update);
                    
                    // Update cached user data to reflect the new level
                    app.setLoggedInUser(mongodb.getDatabase().getCollection("data").find(filter).first());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}