package thegame.screens;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL;

import com.google.gson.Gson;

import thegame.App;
import thegame.Screen;
import thegame.utils.FontRenderer;
import thegame.onScreenObjects.Button;
import thegame.utils.LevelConfig;
import thegame.utils.LevelConfig.MoveType;
import thegame.utils.MongoDBConnection;
import thegame.utils.CloudBackground;
import org.bson.Document;
import com.mongodb.client.MongoCollection;

/**
 * The main game screen where the user plays the puzzle game.
 * Manages game state, grid rendering, and gameplay mechanics.
 */
public class GameScreen implements Screen {
    // Move type constants
    private static final int MOVE_SWAP = 0;
    private static final int MOVE_ROW = 1;
    private static final int MOVE_COLUMN = 2;
    private static final int MOVE_MULTIPLY = 3; // Only for number mode
    
    // Number operation constants
    private static final int NUM_OP_ADD = 0;
    private static final int NUM_OP_SUBTRACT = 1;
    private static final int NUM_OP_MULTIPLY = 2;
    private static final int NUM_OP_DIVIDE = 3;

    private final int gridSize;
    private final LevelConfig.Cell[][] grid;
    private final LevelConfig.Cell[][] targetPattern;
    private final int maxMoves; // Keep for backward compatibility
    private Map<String, Integer> moveLimits; // New: Move limits per type
    private Map<String, Integer> movesUsedPerType; // New: Track moves used per type
    private int movesUsed; // Keep for backward compatibility
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
    private boolean numberControlsActive = false; // Toggle between movement and number controls
    private int currentMoveType = MOVE_SWAP;
    private int currentNumberOp = NUM_OP_ADD; // Default number operation
    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean selectingRow = false;
    private boolean selectingColumn = false;
    private boolean selectingCell = false;
    
    // OpenGL UI components
    private Button backButton;
    private Button[] modeButtons;
    private Button controlToggleButton; // Toggle between movement and number controls
    private Button[] numberOpButtons; // Buttons for number operations
    private Button selectRowButton; // Button to select a row
    private Button selectColumnButton; // Button to select a column
    private Button selectCellButton; // Button to select a cell
    private Button[] victoryButtons;
    private Button[] failureButtons;
    private Button likeButton;
    private boolean hasLiked = false;
    private boolean statisticsUpdated = false;
    private FontRenderer fontRenderer;
    
    // Grid rendering properties
    private float mainGridX, mainGridY, mainGridCellSize;
    private float targetGridX, targetGridY, targetGridCellSize;
    
    // Mouse position tracking
    private double currentMouseX = 0;
    private double currentMouseY = 0;

    private final LevelConfig levelConfig; // Add field to store the LevelConfig
    private boolean isVerificationMode = false;

    // Cloud background
    private CloudBackground cloudBackground;

    /**
     * Constructs a new game screen with the specified level configuration.
     * 
     * @param app The main application instance
     * @param levelConfig The configuration for the level to be played
     * @throws IllegalArgumentException if levelConfig is invalid
     */
    public GameScreen(App app, LevelConfig levelConfig) {
        this.app = app;
        this.levelConfig = levelConfig; // Store the levelConfig
        
        if (levelConfig == null) {
            throw new IllegalArgumentException("levelConfig cannot be null");
        }
        if (levelConfig.getSettings() == null) {
            throw new IllegalArgumentException("levelConfig settings cannot be null");
        }

        this.gridSize = levelConfig.getSettings().getGridSize();

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
        
        // Initialize move limits and tracking
        this.moveLimits = new HashMap<>(levelConfig.getSettings().getMoveLimits());
        this.movesUsedPerType = new HashMap<>();
        for (MoveType moveType : MoveType.values()) {
            movesUsedPerType.put(moveType.name(), 0);
        }
        
        this.levelName = levelConfig.getName();
        this.currentLevelNumber = levelConfig.getLevelNumber();
        this.isNumberMode = levelConfig.getSettings().isNumberMode();
        
        // Initialize cloud background
        cloudBackground = new CloudBackground(CloudBackground.RenderStyle.SIMPLE_BLOTS);
        
        initUI();
    }
    
    /**
     * Creates a GameScreen from a built-in level number.
     * 
     * @param app The main application instance
     * @param levelNumber The level number to load
     * @return A new GameScreen instance with the loaded level
     */
    public static GameScreen fromLevelNumber(App app, int levelNumber) {
        try {
            String resourcePath = "/levels/level" + levelNumber + ".json";
            
            InputStream inputStream = GameScreen.class.getResourceAsStream(resourcePath);
            if (inputStream == null) {
                // Try alternate path formats
                String[] alternativePaths = {
                    "/levels/level" + levelNumber + ".json",
                    "/level" + levelNumber + ".json",
                    "levels/level" + levelNumber + ".json",
                    "level" + levelNumber + ".json"
                };
                
                for (String path : alternativePaths) {
                    inputStream = GameScreen.class.getResourceAsStream(path);
                    if (inputStream != null) {
                        break;
                    }
                }
                
                if (inputStream == null) {
                    throw new RuntimeException("Could not find level " + levelNumber + " in any location");
                }
            }
            
            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            
            LevelConfig levelConfig = new Gson().fromJson(jsonContent, LevelConfig.class);
            
            return new GameScreen(app, levelConfig);
            
        } catch (Exception e) {
            return createTestScreen(app);
        }
    }
    
    /**
     * Creates a simple test level as a fallback when level loading fails.
     * 
     * @param app The main application instance
     * @return A new GameScreen with a basic test level
     */
    private static GameScreen createTestScreen(App app) {
        LevelConfig.Settings settings = new LevelConfig.Settings(2, 3, "test");
        
        LevelConfig.Cell[][] testGrid = new LevelConfig.Cell[2][2];
        testGrid[0][0] = new LevelConfig.Cell(1.0f, 0.0f, 0.0f, true); // Red
        testGrid[0][1] = new LevelConfig.Cell(0.0f, 1.0f, 0.0f, true); // Green
        testGrid[1][0] = new LevelConfig.Cell(0.0f, 0.0f, 1.0f, true); // Blue
        testGrid[1][1] = new LevelConfig.Cell(1.0f, 1.0f, 0.0f, true); // Yellow
        
        LevelConfig.Cell[][] testTarget = new LevelConfig.Cell[2][2];
        testTarget[0][0] = new LevelConfig.Cell(0.0f, 1.0f, 0.0f, true); // Green
        testTarget[0][1] = new LevelConfig.Cell(0.0f, 0.0f, 1.0f, true); // Blue
        testTarget[1][0] = new LevelConfig.Cell(1.0f, 1.0f, 0.0f, true); // Yellow
        testTarget[1][1] = new LevelConfig.Cell(1.0f, 0.0f, 0.0f, true); // Red
        
        LevelConfig testLevel = new LevelConfig.Builder()
            .withId(999)
            .withName("Test Level")
            .withGrid(testGrid)
            .withTargetPattern(testTarget)
            .withSettings(settings)
            .build();
        
        return new GameScreen(app, testLevel);
    }
    
    /**
     * Initializes the user interface components like buttons and grid positions.
     */
    private void initUI() {
        // Initialize font renderer
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("/fonts/pf_tempesta_seven_bold.ttf");
        
        // Create back button
        backButton = new Button(20, App.WINDOW_HEIGHT - 60, 200, 40, 0.7f, 0.3f, 0.6f, "Back to Level Select"); // Dark pink
        
        // Calculate grid positions with dynamic scaling for larger grids
        // Base size for 5x5 grids
        float baseMainCellSize = 80.0f;
        float baseTargetCellSize = 60.0f;
        
        // Scale down for larger grids
        if (gridSize > 5) {
            // Calculate scaling factor to keep the grid size similar to 5x5
            float scaleFactor = 5.0f / gridSize;
            mainGridCellSize = baseMainCellSize * scaleFactor;
            targetGridCellSize = baseTargetCellSize * scaleFactor;
        } else {
            // Use default sizes for small grids
            mainGridCellSize = baseMainCellSize;
            targetGridCellSize = baseTargetCellSize;
        }
        
        mainGridX = 140;
        mainGridY = 240;
        
        targetGridX = App.WINDOW_WIDTH - 200 - (gridSize * targetGridCellSize);
        targetGridY = 200;
        
        // Create mode buttons with pink theme
        modeButtons = new Button[isNumberMode ? 4 : 3];
        modeButtons[0] = new Button(20, 20, 120, 40, 0.9f, 0.5f, 0.8f, "Swap Mode"); // Secondary pink
        modeButtons[1] = new Button(150, 20, 120, 40, 1.0f, 0.4f, 0.7f, "Row Mode"); // Accent pink
        modeButtons[2] = new Button(280, 20, 120, 40, 0.7f, 0.3f, 0.6f, "Column Mode"); // Dark pink
        
        if (isNumberMode) {
            modeButtons[3] = new Button(410, 20, 120, 40, 1.0f, 0.6f, 0.8f, "Multiply Mode"); // Brighter pink
            
            // Add toggle button for number controls
            controlToggleButton = new Button(540, 20, 180, 40, 0.9f, 0.3f, 0.7f, "Toggle Number Controls"); // Vibrant pink
            
            // Initialize number operation buttons with pink theme
            numberOpButtons = new Button[4];
            numberOpButtons[0] = new Button(20, 70, 120, 40, 0.9f, 0.5f, 0.8f, "Add 1"); // Secondary pink
            numberOpButtons[1] = new Button(150, 70, 120, 40, 1.0f, 0.4f, 0.7f, "Subtract 1"); // Accent pink
            numberOpButtons[2] = new Button(280, 70, 120, 40, 0.7f, 0.3f, 0.6f, "Multiply by 2"); // Dark pink
            numberOpButtons[3] = new Button(410, 70, 120, 40, 1.0f, 0.6f, 0.8f, "Divide by 2"); // Brighter pink
            
            // Initialize selection mode buttons
            selectRowButton = new Button(20, 120, 120, 40, 0.9f, 0.5f, 0.8f, "Select Row"); // Secondary pink
            selectColumnButton = new Button(150, 120, 120, 40, 1.0f, 0.4f, 0.7f, "Select Column"); // Accent pink
            selectCellButton = new Button(280, 120, 120, 40, 0.7f, 0.3f, 0.6f, "Select Cell"); // Dark pink
        }
        
        // Create victory buttons with pink theme
        boolean isOnlineLevel = currentLevelNumber > 5000 || levelConfig.isCustomLevel();
        int buttonCount = isOnlineLevel ? 2 : 3;
        victoryButtons = new Button[buttonCount];
        float victoryButtonX = App.WINDOW_WIDTH / 2 - 80;
        float victoryButtonY = App.WINDOW_HEIGHT / 2 - 50;
        victoryButtons[0] = new Button(victoryButtonX, victoryButtonY, 160, 40, 0.9f, 0.5f, 0.8f, "Menu"); // Secondary pink
        victoryButtons[1] = new Button(victoryButtonX, victoryButtonY + 50, 160, 40, 1.0f, 0.4f, 0.7f, "Restart"); // Accent pink
        
        // Only add "Next Level" button for built-in levels
        if (!isOnlineLevel) {
            victoryButtons[2] = new Button(victoryButtonX, victoryButtonY + 100, 160, 40, 0.7f, 0.3f, 0.6f, "Next Level"); // Dark pink
        }
        
        // Create failure buttons with pink theme
        failureButtons = new Button[2];
        float failureButtonX = App.WINDOW_WIDTH / 2 - 80;
        float failureButtonY = App.WINDOW_HEIGHT / 2 - 20;
        failureButtons[0] = new Button(failureButtonX, failureButtonY, 160, 40, 0.9f, 0.5f, 0.8f, "Try Again"); // Secondary pink
        failureButtons[1] = new Button(failureButtonX, failureButtonY + 50, 160, 40, 0.7f, 0.3f, 0.6f, "Menu"); // Dark pink
        
        // Create like button for the victory screen
        likeButton = new Button(App.WINDOW_WIDTH / 2 - 80, App.WINDOW_HEIGHT / 2 + 150, 
                              160, 40, 1.0f, 0.4f, 0.7f, "Like Level"); // Accent pink
    }

    /**
     * Renders the game screen, including grids, buttons, and UI elements.
     */
    @Override
    public void render() {
        try {
            // Re-verify OpenGL context
            if (GL.getCapabilities() == null) {
                glfwMakeContextCurrent(app.getWindow());
                GL.createCapabilities();
                if (GL.getCapabilities() == null) return;
            }
            
            // Set up 2D projection
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(0, App.WINDOW_WIDTH, App.WINDOW_HEIGHT, 0, -1, 1);
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            
            // Clear screen with a pink background
            glClearColor(1.0f, 0.7f, 0.9f, 1.0f); // Primary pink
            glClear(GL_COLOR_BUFFER_BIT);
            
            // Update and render clouds (with simple white blots style)
            cloudBackground.update();
            cloudBackground.render();
            
            // Draw border to verify rendering
            glColor3f(0.9f, 0.5f, 0.8f); // Secondary pink
            glLineWidth(2.0f);
            glBegin(GL_LINE_LOOP);
            glVertex2f(10, 10);
            glVertex2f(App.WINDOW_WIDTH - 10, 10);
            glVertex2f(App.WINDOW_WIDTH - 10, App.WINDOW_HEIGHT - 10);
            glVertex2f(10, App.WINDOW_HEIGHT - 10);
            glEnd();
            
            // Render level name
            if (fontRenderer != null) {
                // Level name position 
                fontRenderer.renderCenteredText(levelName, App.WINDOW_WIDTH / 4f + 50, 40, 2.0f);
                
                // Move counters - adjust these values
                float moveCounterX = 20;
                float moveCounterY = 90;
                
                // If number controls are active, push down the text to avoid overlap with buttons
                if (isNumberMode && numberControlsActive) {
                    moveCounterY = 170; // Position below the number control buttons (which end at y=160)
                }
                
                // Increase this value to create more space between lines
                float moveCounterSpacing = 25;
                
                // Total moves text
                String totalMovesText = String.format("Total Moves: %d/%d", movesUsed, maxMoves);
                fontRenderer.renderText(totalMovesText, moveCounterX - 5, moveCounterY - 20, 1.2f);

                // Only show move counters if not using number controls
                if (!numberControlsActive || !isNumberMode) {
                    // Show specific move type counters with adjusted spacing
                    if (moveLimits.containsKey(MoveType.SWAP.name())) {
                        String swapText = String.format("Swaps: %d/%d", 
                            movesUsedPerType.getOrDefault(MoveType.SWAP.name(), 0),
                            moveLimits.getOrDefault(MoveType.SWAP.name(), 0));
                        fontRenderer.renderText(swapText, moveCounterX, moveCounterY + moveCounterSpacing, 1.0f);
                    }
                    
                    if (moveLimits.containsKey(MoveType.FLIP_ROW.name())) {
                        String rowText = String.format("Row Flips: %d/%d", 
                            movesUsedPerType.getOrDefault(MoveType.FLIP_ROW.name(), 0),
                            moveLimits.getOrDefault(MoveType.FLIP_ROW.name(), 0));
                        fontRenderer.renderText(rowText, moveCounterX, moveCounterY + moveCounterSpacing * 2, 1.0f);
                    }
                    
                    if (moveLimits.containsKey(MoveType.FLIP_COLUMN.name())) {
                        String colText = String.format("Column Flips: %d/%d", 
                            movesUsedPerType.getOrDefault(MoveType.FLIP_COLUMN.name(), 0),
                            moveLimits.getOrDefault(MoveType.FLIP_COLUMN.name(), 0));
                        fontRenderer.renderText(colText, moveCounterX, moveCounterY + moveCounterSpacing * 3, 1.0f);
                    }
                    
                    if (moveLimits.containsKey(MoveType.ROTATE.name())) {
                        String rotateText = String.format("Rotations: %d/%d", 
                            movesUsedPerType.getOrDefault(MoveType.ROTATE.name(), 0),
                            moveLimits.getOrDefault(MoveType.ROTATE.name(), 0));
                        fontRenderer.renderText(rotateText, moveCounterX, moveCounterY + moveCounterSpacing * 4, 1.0f);
                    }
                } else {
                    // Show number operation mode text
                    String modeText = "";
                    if (selectingRow) modeText = "Selecting Row";
                    else if (selectingColumn) modeText = "Selecting Column";
                    else if (selectingCell) modeText = "Selecting Cell";
                    
                    String opText = "";
                    switch (currentNumberOp) {
                        case NUM_OP_ADD: opText = "Add 1"; break;
                        case NUM_OP_SUBTRACT: opText = "Subtract 1"; break;
                        case NUM_OP_MULTIPLY: opText = "Multiply by 2"; break;
                        case NUM_OP_DIVIDE: opText = "Divide by 2"; break;
                    }
                    
                    fontRenderer.renderText("Number Controls: " + modeText, 
                        moveCounterX, moveCounterY + moveCounterSpacing, 1.0f);
                    fontRenderer.renderText("Operation: " + opText, 
                        moveCounterX, moveCounterY + moveCounterSpacing * 2, 1.0f);
                }
                
                // Render grid labels - position higher on the screen
                fontRenderer.renderText("CURRENT GRID:", mainGridX / 1.5f + 50, mainGridY / 1.5f - 40, 1.5f);
                fontRenderer.renderText("TARGET GRID:", targetGridX / 1.5f, targetGridY / 1.5f - 40, 1.5f);
            }
        
            // Draw grid backgrounds with pink theme
            drawRect(mainGridX - 10, mainGridY - 10, 
                    gridSize * mainGridCellSize + 20, gridSize * mainGridCellSize + 20, 
                    0.8f, 0.4f, 0.7f); // Accent pink
            
            drawRect(targetGridX - 10, targetGridY - 10, 
                    gridSize * targetGridCellSize + 20, gridSize * targetGridCellSize + 20, 
                    0.8f, 0.4f, 0.7f); // Accent pink
            
            // Render grids
            renderGrid(grid, mainGridX, mainGridY, mainGridCellSize);
            renderGrid(targetPattern, targetGridX, targetGridY, targetGridCellSize);
            
            // Render buttons
            backButton.render((float)currentMouseX, (float)currentMouseY);
            
            // Render appropriate control buttons based on mode
            if (!isNumberMode || !numberControlsActive) {
                // Standard movement controls
                for (int i = 0; i < modeButtons.length; i++) {
                    if (i == currentMoveType) {
                        modeButtons[i].setCaptionColor(1.0f, 1.0f, 0.0f, 1.0f);
                    } else {
                        modeButtons[i].setCaptionColor(1.0f, 1.0f, 1.0f, 1.0f);
                    }
                    modeButtons[i].render((float)currentMouseX, (float)currentMouseY);
                }
            } else {
                // Number-based operation controls
                for (int i = 0; i < numberOpButtons.length; i++) {
                    if (i == currentNumberOp) {
                        numberOpButtons[i].setCaptionColor(1.0f, 1.0f, 0.0f, 1.0f);
                    } else {
                        numberOpButtons[i].setCaptionColor(1.0f, 1.0f, 1.0f, 1.0f);
                    }
                    numberOpButtons[i].render((float)currentMouseX, (float)currentMouseY);
                }
                
                // Selection mode buttons
                selectRowButton.setCaptionColor(selectingRow ? 1.0f : 0.7f, 1.0f, selectingRow ? 0.0f : 0.7f, 1.0f);
                selectColumnButton.setCaptionColor(selectingColumn ? 1.0f : 0.7f, 1.0f, selectingColumn ? 0.0f : 0.7f, 1.0f);
                selectCellButton.setCaptionColor(selectingCell ? 1.0f : 0.7f, 1.0f, selectingCell ? 0.0f : 0.7f, 1.0f);
                
                selectRowButton.render((float)currentMouseX, (float)currentMouseY);
                selectColumnButton.render((float)currentMouseX, (float)currentMouseY);
                selectCellButton.render((float)currentMouseX, (float)currentMouseY);
            }
            
            // Render toggle button if in number mode
            if (isNumberMode) {
                controlToggleButton.setCaptionColor(
                    numberControlsActive ? 0.2f : 0.7f,
                    numberControlsActive ? 0.8f : 0.7f,
                    numberControlsActive ? 0.2f : 0.7f,
                    1.0f
                );
                controlToggleButton.render((float)currentMouseX, (float)currentMouseY);
            }
            
            // Handle victory screen
            if (hasWon) {
                // Check if this is verification mode
                if (isVerificationMode) {
                    // If in verification mode, save the level as verified and return to editor
                    if (!statisticsUpdated) {
                        if (app.isLoggedIn()) {
                            try {
                                // Get the level ID from the app or from the current level
                                int levelId = app.getMostRecentlyEditedLevelId();
                                if (levelId <= 0) {
                                    levelId = this.levelConfig.getLevelNumber();
                                }
                                
                                // Get the static pending level and key from LevelEditorScreen
                                LevelConfig pendingLevel = LevelEditorScreen.getPendingLevel();
                                String pendingLevelKey = LevelEditorScreen.getPendingLevelKey();
                                
                                if (pendingLevel != null) {
                                    System.out.println("VERIFICATION DEBUG: Found pending level: " + 
                                                     pendingLevel.getName() + " with ID " + 
                                                     pendingLevel.getLevelNumber());
                                } else {
                                    System.out.println("VERIFICATION DEBUG: No pending level found, using current level");
                                }
                                
                                // Create the OnlineLevelSelectScreen with verification success flag
                                app.setCurrentScreen(new OnlineLevelSelectScreen(app, true, true));
                                System.out.println("VERIFICATION DEBUG: Returned to OnlineLevelSelectScreen with success flag");
                                
                            } catch (Exception e) {
                                System.err.println("VERIFICATION ERROR: " + e.getMessage());
                                e.printStackTrace();
                                app.setCurrentScreen(new OnlineLevelSelectScreen(app, true, false));
                            }
                        } else {
                            System.out.println("VERIFICATION DEBUG: User not logged in, can't verify");
                            app.setCurrentScreen(new OnlineLevelSelectScreen(app, true, false));
                        }
                        statisticsUpdated = true;
                    }
                } else {
                    // Normal victory handling
                    // Increment play count when level is completed
                    if (!statisticsUpdated) {
                        updateLevelStatistics(true, false);
                        statisticsUpdated = true;
                    }
                }
                
                drawOverlay();
                fontRenderer.renderCenteredText("Level Complete!", App.WINDOW_WIDTH / 2, App.WINDOW_HEIGHT / 2 - 70, 2.0f, 0.2f, 0.8f, 0.2f, 1.0f);
                
                // Draw regular victory buttons
                for (Button button : victoryButtons) {
                    button.render((float)currentMouseX, (float)currentMouseY);
                }
                
                // Only show like button for online levels
                if (currentLevelNumber > 1000 || levelConfig.isCustomLevel()) {
                    if (hasLiked) {
                        fontRenderer.renderCenteredText("Thanks for liking!", App.WINDOW_WIDTH / 2, 
                                        App.WINDOW_HEIGHT / 2 + 120, 1.2f, 0.2f, 0.7f, 0.2f, 1.0f);
                    } else {
                        likeButton.render((float)currentMouseX, (float)currentMouseY);
                    }
                }
            }
            
            // Handle failure screen
            if (hasLost) {
                drawOverlay();
                fontRenderer.renderCenteredText("Out of Moves!", App.WINDOW_WIDTH / 2, App.WINDOW_HEIGHT / 2 - 70, 2.0f, 0.8f, 0.2f, 0.2f, 1.0f);
                for (Button button : failureButtons) {
                    button.render((float)currentMouseX, (float)currentMouseY);
                }
            }
            
        } catch (Exception e) {
            // Handle exception silently
        }
    }
    
    /**
     * Draws a semi-transparent overlay for victory/failure screens.
     */
    private void drawOverlay() {
        float panelWidth = 400;
        float panelHeight = 300;
        float panelX = App.WINDOW_WIDTH / 2 - panelWidth / 2;
        float panelY = App.WINDOW_HEIGHT / 2 - panelHeight / 2;
        
        glColor4f(0.9f, 0.5f, 0.8f, 0.8f); // Semi-transparent pink
        glBegin(GL_QUADS);
        glVertex2f(panelX, panelY);
        glVertex2f(panelX + panelWidth, panelY);
        glVertex2f(panelX + panelWidth, panelY + panelHeight);
        glVertex2f(panelX, panelY + panelHeight);
        glEnd();
    }
    
    /**
     * Draws a rectangle with the specified coordinates and color.
     * 
     * @param x X coordinate of the top-left corner
     * @param y Y coordinate of the top-left corner
     * @param width Width of the rectangle
     * @param height Height of the rectangle
     * @param r Red component (0.0-1.0)
     * @param g Green component (0.0-1.0)
     * @param b Blue component (0.0-1.0)
     */
    private void drawRect(float x, float y, float width, float height, float r, float g, float b) {
        glColor3f(r, g, b);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();
    }
    
    /**
     * Renders a grid of cells at the specified position.
     * 
     * @param gridToRender The grid data to render
     * @param posX X coordinate of the top-left corner
     * @param posY Y coordinate of the top-left corner
     * @param cellSize Size of each cell
     */
    private void renderGrid(LevelConfig.Cell[][] gridToRender, float posX, float posY, float cellSize) {
        if (gridToRender == null) {
            return;
        }
        
        try {
            // Draw cells
            for (int row = 0; row < gridToRender.length; row++) {
                for (int col = 0; col < gridToRender[row].length; col++) {
                    LevelConfig.Cell cell = gridToRender[row][col];
                    
                    float x = posX + col * cellSize;
                    float y = posY + row * cellSize;
                    
                    if (isNumberMode) {
                        // For number mode, use grayscale background based on value
                        int value = cell.getNumericValue();
                        float grayValue = Math.min(0.8f, value * 0.1f);
                        glColor3f(grayValue, grayValue, grayValue);
                    } else {
                        // For color mode, use the RGB values
                        glColor3f(cell.red, cell.green, cell.blue);
                    }
                    
                    // Draw cell with padding
                    glBegin(GL_QUADS);
                    glVertex2f(x + 2, y + 2);
                    glVertex2f(x + cellSize - 2, y + 2);
                    glVertex2f(x + cellSize - 2, y + cellSize - 2);
                    glVertex2f(x + 2, y + cellSize - 2);
                    glEnd();
                    
                    // Draw cell border
                    glColor3f(0.0f, 0.0f, 0.0f);
                    glLineWidth(1.0f);
                    glBegin(GL_LINE_LOOP);
                    glVertex2f(x, y);
                    glVertex2f(x + cellSize, y);
                    glVertex2f(x + cellSize, y + cellSize);
                    glVertex2f(x, y + cellSize);
                    glEnd();
                    
                    // Draw number if in number mode
                    if (isNumberMode) {
                        int number = cell.getNumericValue();
                        if (fontRenderer != null) {
                            String numStr = String.valueOf(number);
                            float textWidth = fontRenderer.getTextWidth(numStr, 1.0f);
                            
                            // Use a high contrast color for text that will stand out on gray
                            float r = 0.9f;
                            float g = 0.2f;
                            float b = 0.2f;
                            
                            fontRenderer.renderText(numStr, 
                                            x + (cellSize - textWidth) / 2, 
                                            y + (cellSize - fontRenderer.getTextHeight(1.0f)) / 2, 
                                            1.0f, r, g, b, 1.0f);
                        }
                    }
                    
                    // Optionally add visual indicator for non-editable cells
                    if (!cell.editable) {
                        glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
                        glBegin(GL_QUADS);
                        glVertex2f(x + 5, y + 5);
                        glVertex2f(x + cellSize - 5, y + 5);
                        glVertex2f(x + cellSize - 5, y + cellSize - 5);
                        glVertex2f(x + 5, y + cellSize - 5);
                        glEnd();
                    }
                    
                }
            }
            
            // Draw grid lines
            glColor3f(1.0f, 1.0f, 1.0f);
            glLineWidth(1.0f);
            glBegin(GL_LINES);
            // Vertical lines
            for (int i = 0; i <= gridSize; i++) {
                float x = posX + i * cellSize;
                glVertex2f(x, posY);
                glVertex2f(x, posY + gridSize * cellSize);
            }
            // Horizontal lines
            for (int i = 0; i <= gridSize; i++) {
                float y = posY + i * cellSize;
                glVertex2f(posX, y);
                glVertex2f(posX + gridSize * cellSize, y);
            }
            glEnd();
            
        } catch (Exception e) {
            // Handle exception silently
        }
    }

    /**
     * Handles mouse click events, including UI button clicks and grid cell selection.
     * 
     * @param mouseX X coordinate of the mouse click
     * @param mouseY Y coordinate of the mouse click
     */
    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
        // Check UI button clicks first
        if (backButton.handleMouseClick(mx, my)) {
            // Check if this is an online level
            boolean isOnlineLevel = currentLevelNumber > 1000 || levelConfig.isCustomLevel();
            
            if (isOnlineLevel) {
                // Go back to online level select if it's an online level
                app.setCurrentScreen(new OnlineLevelSelectScreen(app));
            } else {
                // Otherwise go to main level select
                app.setCurrentScreen(new LevelSelect(app));
            }
            return;
        }
        
        // Handle toggle button if in number mode
        if (isNumberMode && controlToggleButton.handleMouseClick(mx, my)) {
            numberControlsActive = !numberControlsActive;
            // Reset selections
            selectingRow = false;
            selectingColumn = false;
            selectingCell = false;
            selectedRow = -1;
            selectedCol = -1;
            return;
        }
        
        // Handle number operation buttons
        if (isNumberMode && numberControlsActive) {
            // Check number operation buttons
            for (int i = 0; i < numberOpButtons.length; i++) {
                if (numberOpButtons[i].handleMouseClick(mx, my)) {
                    currentNumberOp = i;
                    return;
                }
            }
            
            // Check selection mode buttons
            if (selectRowButton.handleMouseClick(mx, my)) {
                selectingRow = true;
                selectingColumn = false;
                selectingCell = false;
                selectedRow = -1;
                selectedCol = -1;
                return;
            }
            
            if (selectColumnButton.handleMouseClick(mx, my)) {
                selectingRow = false;
                selectingColumn = true;
                selectingCell = false;
                selectedRow = -1;
                selectedCol = -1;
                return;
            }
            
            if (selectCellButton.handleMouseClick(mx, my)) {
                selectingRow = false;
                selectingColumn = false;
                selectingCell = true;
                selectedRow = -1;
                selectedCol = -1;
                return;
            }
            
            // Handle grid cell selection for number operations
            int[] cellCoords = getCellFromCoordinates(mouseX, mouseY);
            if (cellCoords != null) {
                handleNumberOperation(cellCoords[0], cellCoords[1]);
                return;
            }
            
        } else {
            // Normal mode button clicks
            for (int i = 0; i < modeButtons.length; i++) {
                if (modeButtons[i].handleMouseClick(mx, my)) {
                    currentMoveType = i;
                    return;
                }
            }
        }
        
        // Victory/failure buttons
        if (hasWon) {
            if (victoryButtons[0].handleMouseClick(mx, my)) {
                // Menu
                app.setCurrentScreen(new LevelSelect(app));
            } else if (victoryButtons[1].handleMouseClick(mx, my)) {
                // Restart
                app.setCurrentScreen(fromLevelNumber(app, currentLevelNumber));
            } else if (victoryButtons.length > 2 && victoryButtons[2].handleMouseClick(mx, my)) {
                // Next level - only for built-in levels
                app.setCurrentScreen(fromLevelNumber(app, currentLevelNumber + 1));
            }
            return;
        }
        
        if (hasLost) {
            if (failureButtons[0].handleMouseClick(mx, my)) {
                // Try again
                app.setCurrentScreen(fromLevelNumber(app, currentLevelNumber));
            } else if (failureButtons[1].handleMouseClick(mx, my)) {
                // Menu
                app.setCurrentScreen(new LevelSelect(app));
            }
            return;
        }
        
        // Handle grid interactions for movement-based controls
        if (!numberControlsActive && !hasWon && !hasLost) {
            int[] cellCoords = getCellFromCoordinates(mouseX, mouseY);
            if (cellCoords != null) {
                handleMoveTypeClick(cellCoords[0], cellCoords[1]);
            }
        }
    }
    
    /**
     * Handles number operations (add, subtract, multiply, divide) on cells.
     * 
     * @param row Row index of the selected cell
     * @param col Column index of the selected cell
     */
    private void handleNumberOperation(int row, int col) {
        String moveTypeName = MoveType.ROTATE.name(); // Use ROTATE for all number operations
        
        if (!canUseMove(moveTypeName)) {
            return;
        }
        
        boolean operationPerformed = false;
        
        if (selectingRow) {
            // Apply operation to entire row
            for (int c = 0; c < gridSize; c++) {
                operationPerformed |= applyNumberOperation(grid[row][c]);
            }
            
        } else if (selectingColumn) {
            // Apply operation to entire column
            for (int r = 0; r < gridSize; r++) {
                operationPerformed |= applyNumberOperation(grid[r][col]);
            }
            
        } else if (selectingCell) {
            // Apply operation to single cell
            operationPerformed = applyNumberOperation(grid[row][col]);
        }
        
        if (operationPerformed) {
            incrementMoveUsed(moveTypeName);
            checkVictory();
        }
    }
    
    /**
     * Applies a number operation to a single cell.
     * 
     * @param cell The cell to modify
     * @return true if the operation changed the cell value, false otherwise
     */
    private boolean applyNumberOperation(LevelConfig.Cell cell) {
        if (cell == null || !cell.editable) return false;
        
        int oldValue = cell.getNumericValue();
        int newValue = oldValue;
        
        switch (currentNumberOp) {
            case NUM_OP_ADD:
                newValue = Math.min(9, oldValue + 1); // Add 1, max 9
                break;
            case NUM_OP_SUBTRACT:
                newValue = Math.max(0, oldValue - 1); // Subtract 1, min 0
                break;
            case NUM_OP_MULTIPLY:
                newValue = Math.min(9, oldValue * 2); // Multiply by 2, max 9
                break;
            case NUM_OP_DIVIDE:
                newValue = oldValue / 2; // Divide by 2, integer division
                break;
        }
        
        // If value didn't change, return false
        if (newValue == oldValue) {
            return false;
        }
        
        // Update the cell value (both numeric value and RGB for consistency)
        float normalized = Math.min(1.0f, Math.max(0.0f, newValue / 9.0f));
        cell.red = normalized;
        cell.green = normalized;
        cell.blue = normalized;
        
        // Set numeric value using reflection (since it's private)
        try {
            java.lang.reflect.Field field = LevelConfig.Cell.class.getDeclaredField("numericValue");
            field.setAccessible(true);
            field.set(cell, newValue);
        } catch (Exception e) {
            System.err.println("Error setting numericValue: " + e.getMessage());
        }
        
        return true;
    }

    /**
     * Handles clicks on grid cells based on the current move type.
     * 
     * @param row Row index of the clicked cell
     * @param col Column index of the clicked cell
     */
    private void handleMoveTypeClick(int row, int col) {
        String moveTypeName;
        
        switch (currentMoveType) {
            case MOVE_SWAP:
                moveTypeName = MoveType.SWAP.name();
                if (canUseMove(moveTypeName)) {
                    handleSwapMode(row, col);
                }
                break;
            case MOVE_ROW:
                moveTypeName = MoveType.FLIP_ROW.name();
                if (canUseMove(moveTypeName)) {
                    handleRowMode(row);
                }
                break;
            case MOVE_COLUMN:
                moveTypeName = MoveType.FLIP_COLUMN.name();
                if (canUseMove(moveTypeName)) {
                    handleColumnMode(col);
                }
                break;
            case MOVE_MULTIPLY:
                moveTypeName = MoveType.ROTATE.name(); // Using ROTATE for MULTIPLY
                if (isNumberMode && canUseMove(moveTypeName)) {
                    handleMultiplyMode(row, col);
                }
                break;
        }
    }
    
    /**
     * Checks if a move type can be used based on the remaining move limits.
     * 
     * @param moveTypeName The name of the move type to check
     * @return true if the move can be used, false otherwise
     */
    private boolean canUseMove(String moveTypeName) {
        Integer limit = moveLimits.get(moveTypeName);
        Integer used = movesUsedPerType.get(moveTypeName);
        
        if (limit == null || used == null) {
            return true; // If no limits defined, allow it
        }
        
        return used < limit;
    }
    
    /**
     * Increments the counter for a specific move type.
     * 
     * @param moveTypeName The name of the move type to increment
     */
    private void incrementMoveUsed(String moveTypeName) {
        movesUsed++; // For backward compatibility
        
        // Increment for specific move type
        Integer current = movesUsedPerType.getOrDefault(moveTypeName, 0);
        movesUsedPerType.put(moveTypeName, current + 1);
    }

    /**
     * Handles cell selection in swap mode.
     * 
     * @param row Row index of the selected cell
     * @param col Column index of the selected cell
     */
    private void handleSwapMode(int row, int col) {
        LevelConfig.Cell clickedCell = grid[row][col];
        if (clickedCell.editable && !isBlackCell(clickedCell)) {
            dragStartRow = row;
            dragStartCol = col;
            isDragging = true;
        }
    }

    /**
     * Handles row selection and swapping.
     * 
     * @param row Row index to select or swap
     */
    private void handleRowMode(int row) {
        if (selectedRow == -1) {
            selectedRow = row;
        } else {
            // Swap entire rows
            LevelConfig.Cell[] tempRow = grid[selectedRow].clone();
            grid[selectedRow] = grid[row].clone();
            grid[row] = tempRow;
            selectedRow = -1;
            incrementMoveUsed(MoveType.FLIP_ROW.name());
            checkVictory();
        }
    }

    /**
     * Handles column selection and swapping.
     * 
     * @param col Column index to select or swap
     */
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
            incrementMoveUsed(MoveType.FLIP_COLUMN.name());
            checkVictory();
        }
    }

    /**
     * Handles multiply operations in number mode.
     * 
     * @param row Row index of the selected cell
     * @param col Column index of the selected cell
     */
    private void handleMultiplyMode(int row, int col) {
        if (isNumberMode) {
            // Multiply selected row or column by 2
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
                incrementMoveUsed(MoveType.ROTATE.name());
                checkVictory();
            }
        }
    }

    /**
     * Handles mouse release events, completing drag operations.
     * 
     * @param mouseX X coordinate of the mouse release
     * @param mouseY Y coordinate of the mouse release
     */
    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        if (!isDragging) return;

        int[] cellPos = getCellFromCoordinates(mouseX, mouseY);
        if (cellPos != null && 
            (cellPos[0] != dragStartRow || cellPos[1] != dragStartCol)) { // Don't swap with self
            
            // Check if cells are adjacent (not diagonal)
            boolean isAdjacent = areAdjacentCells(dragStartRow, dragStartCol, cellPos[0], cellPos[1]);
            
            LevelConfig.Cell targetCell = grid[cellPos[0]][cellPos[1]];
            if (targetCell.editable && !isBlackCell(targetCell) && isAdjacent) {
                // Swap cells
                LevelConfig.Cell temp = grid[dragStartRow][dragStartCol];
                grid[dragStartRow][dragStartCol] = grid[cellPos[0]][cellPos[1]];
                grid[cellPos[0]][cellPos[1]] = temp;
                
                incrementMoveUsed(MoveType.SWAP.name());
                checkVictory();
            }
        }

        isDragging = false;
        dragStartRow = -1;
        dragStartCol = -1;
    }

    /**
     * Check if two cells are adjacent (up, down, left, right)
     * @return true if cells are adjacent, false otherwise
     */
    private boolean areAdjacentCells(int row1, int col1, int row2, int col2) {
        // Same row, columns differ by 1 (left/right adjacency)
        if (row1 == row2 && Math.abs(col1 - col2) == 1) {
            return true;
        }
        
        // Same column, rows differ by 1 (up/down adjacency)
        if (col1 == col2 && Math.abs(row1 - row2) == 1) {
            return true;
        }
        
        // Not adjacent
        return false;
    }

    /**
     * Checks if two cells have matching colors or values.
     * 
     * @param cell1 The first cell
     * @param cell2 The second cell
     * @return true if the cells match, false otherwise
     */
    private boolean checkMatch(LevelConfig.Cell cell1, LevelConfig.Cell cell2) {
        if (isNumberMode) {
            // Compare numeric values for number mode instead of just the red channel
            return cell1.getNumericValue() == cell2.getNumericValue();
        } else {
            // Compare all channels for colors
            return Math.abs(cell1.red - cell2.red) < 0.01f &&
                   Math.abs(cell1.green - cell2.green) < 0.01f &&
                   Math.abs(cell1.blue - cell2.blue) < 0.01f;
        }
    }

    /**
     * Checks if the player has won or lost the level.
     * Updates hasWon or hasLost flags accordingly.
     */
    private void checkVictory() {
        // Check if any move type has exceeded its limit
        for (String moveType : moveLimits.keySet()) {
            int used = movesUsedPerType.getOrDefault(moveType, 0);
            int limit = moveLimits.getOrDefault(moveType, Integer.MAX_VALUE);
            
            if (used > limit) {
                hasLost = true;
                return;
            }
        }
        
        // Also check total moves for backward compatibility
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

    /**
     * Updates the current mouse position.
     * 
     * @param mouseX The current X coordinate of the mouse
     * @param mouseY The current Y coordinate of the mouse
     */
    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }

    /**
     * Converts mouse coordinates to grid cell indices.
     * 
     * @param mouseX The X coordinate of the mouse
     * @param mouseY The Y coordinate of the mouse
     * @return An array containing [row, col] if the coordinates are within the grid, null otherwise
     */
    private int[] getCellFromCoordinates(double mouseX, double mouseY) {
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
    
    /**
     * Checks if a cell is a black/empty cell.
     * 
     * @param cell The cell to check
     * @return true if the cell is black (all RGB values are 0), false otherwise
     */
    private boolean isBlackCell(LevelConfig.Cell cell) {
        return cell.red == 0.0f && cell.green == 0.0f && cell.blue == 0.0f;
    }

    /**
     * Handles key press events.
     * 
     * @param key The key code
     * @param action The action (press, release, etc.)
     */
    @Override
    public void handleKeyPress(int key, int action) {
        // Not needed for now
    }
    
    /**
     * Handles character input events.
     * 
     * @param codepoint The Unicode code point of the character
     */
    @Override
    public void handleCharInput(int codepoint) {
        // Not needed for level select as it has no text input fields
    }

    /**
     * Gets the level configuration for this game screen.
     * 
     * @return The level configuration
     */
    public LevelConfig getLevelConfig() {
        return levelConfig;
    }
    
    /**
     * Updates the statistics for this level in the database.
     * 
     * @param incrementPlays Whether to increment the "timeplayed" counter
     * @param incrementLikes Whether to increment the "likes" counter
     */
    private void updateLevelStatistics(boolean incrementPlays, boolean incrementLikes) {
        // Only process online levels
        if (!(currentLevelNumber > 1000 || levelConfig.isCustomLevel())) {
            return;
        }
        
        new Thread(() -> {
            try (MongoDBConnection mongodb = new MongoDBConnection()) {
                // Find the level owner
                MongoCollection<Document> usersCollection = mongodb.getDatabase().getCollection("data");
                String levelIdStr = String.valueOf(currentLevelNumber);
                String levelKey = "level" + levelIdStr;
                
                // Search for the level in all users
                for (Document userDoc : usersCollection.find()) {
                    Object levelsObj = userDoc.get("Levels");
                    if (!(levelsObj instanceof Document)) {
                        continue;
                    }
                    
                    Document levelsDoc = (Document) levelsObj;
                    if (!levelsDoc.containsKey(levelKey)) {
                        continue;
                    }
                    
                    // Found the level, now update its statistics
                    Document levelDoc = (Document) levelsDoc.get(levelKey);
                    Document statisticsDoc = levelDoc.get("statistics", new Document());
                    
                    // Create statistics object if it doesn't exist
                    if (statisticsDoc == null) {
                        statisticsDoc = new Document();
                    }
                    
                    // Update statistics
                    int timeplayed = statisticsDoc.getInteger("timeplayed", 0);
                    int likes = statisticsDoc.getInteger("likes", 0);
                    
                    if (incrementPlays) {
                        timeplayed++;
                    }
                    
                    if (incrementLikes) {
                        likes++;
                    }
                    
                    // Store updated values
                    statisticsDoc.put("timeplayed", timeplayed);
                    statisticsDoc.put("likes", likes);
                    levelDoc.put("statistics", statisticsDoc);
                    
                    // Update in database
                    Document updateDoc = new Document("$set", 
                                      new Document("Levels." + levelKey, levelDoc));
                    usersCollection.updateOne(new Document("_id", userDoc.get("_id")), updateDoc);
                    
                    // Update statistics in memory using reflection since setter methods aren't available
                    try {
                        if (levelConfig.getStatistics() == null) {
                            // Create a new Statistics object using reflection
                            Class<?> statsClass = Class.forName("thegame.utils.LevelConfig$Statistics");
                            java.lang.reflect.Constructor<?> constructor = statsClass.getDeclaredConstructor(int.class, int.class);
                            Object statsObj = constructor.newInstance(timeplayed, likes);
                            
                            // Set the statistics field in levelConfig
                            java.lang.reflect.Field statsField = levelConfig.getClass().getDeclaredField("statistics");
                            statsField.setAccessible(true);
                            statsField.set(levelConfig, statsObj);
                        } else {
                            // Update existing Statistics object fields
                            Object statsObj = levelConfig.getStatistics();
                            if (incrementPlays) {
                                java.lang.reflect.Field timePlayedField = statsObj.getClass().getDeclaredField("timePlayed");
                                timePlayedField.setAccessible(true);
                                timePlayedField.set(statsObj, timeplayed);
                            }
                            
                            if (incrementLikes) {
                                java.lang.reflect.Field likesField = statsObj.getClass().getDeclaredField("likes");
                                likesField.setAccessible(true);
                                likesField.set(statsObj, likes);
                            }
                        }
                    } catch (Exception e) {
                        // Handle silently
                    }
                    
                    break;
                }
            } catch (Exception e) {
                // Handle silently
            }
        }).start();
    }
    
    /**
     * Creates a game screen in verification mode for testing level solvability.
     * 
     * @param app The main application instance
     * @param levelConfig The level configuration to verify
     * @param verificationMode Whether to run in verification mode
     */
    public GameScreen(App app, LevelConfig levelConfig, boolean verificationMode) {
        this(app, levelConfig);
        this.isVerificationMode = verificationMode;
        
        if (verificationMode) {
            // Store the level ID in the app for later retrieval
            app.setMostRecentlyEditedLevelId(levelConfig.getLevelNumber());
        }
    }

    /**
     * Cleans up resources used by the game screen.
     */
    public void cleanup() {
        // Clean up cloud background
        if (cloudBackground != null) {
            cloudBackground.cleanup();
            cloudBackground = null;
        }
    }
}