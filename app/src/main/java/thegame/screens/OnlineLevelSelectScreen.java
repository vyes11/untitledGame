package thegame.screens;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import thegame.App;
import thegame.Screen;
import thegame.onScreenObjects.Button;
import thegame.utils.FontRenderer;
import thegame.utils.LevelConfig;
import thegame.utils.MongoDBConnection;
import thegame.utils.CloudBackground;

/**
 * Screen for browsing, selecting, and managing online user-created levels.
 * Provides functionality for listing, filtering, and sorting levels.
 */
public class OnlineLevelSelectScreen implements Screen {
    private List<LevelConfig> onlineLevels;
    private Map<String, String> levelCreators; // Maps level ID to creator username
    private final App app;
    private String statusMessage = "";
    private boolean isLoading = false;
    private int currentPage = 0;
    private static final int LEVELS_PER_PAGE = 12;
    
    // Layout constants
    private static final int GRID_COLS = 4;
    private static final float BUTTON_WIDTH = 180.0f;
    private static final float BUTTON_HEIGHT = 100.0f;
    private static final float BUTTON_SPACING_X = 30.0f;
    private static final float BUTTON_SPACING_Y = 40.0f;
    
    // UI elements
    private FontRenderer fontRenderer;
    private List<Button> levelButtons = new ArrayList<>();
    private Button backButton;
    private Button createLevelButton;
    private Button nextPageButton;
    private Button prevPageButton;
    private Button toggleViewButton;
    private Button searchButton;
    
    // Mouse position
    private double currentMouseX = 0;
    private double currentMouseY = 0;

    // Flags to manage loading and button creation
    private boolean levelsLoaded = false;
    private boolean needsButtonCreation = false;
    private boolean showingUserLevelsOnly = false;
    private List<LevelConfig> filteredLevels = new ArrayList<>();
    private Map<Integer, Button> editButtons = new HashMap<>(); // Edit buttons for user's own levels
    private Map<Integer, Button> deleteButtons = new HashMap<>(); // New: delete buttons for user's own levels
    private boolean showingDeleteConfirmation = false;
    private int levelToDelete = -1;
    private Button confirmDeleteButton;
    private Button cancelDeleteButton;
    
    // Sort options
    private enum SortType {
        BY_ID("Sort: Default"),
        BY_PLAYS("Sort: Most Played"),
        BY_LIKES("Sort: Most Liked");
        
        private final String label;
        
        SortType(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }
    
    private SortType currentSortType = SortType.BY_ID;
    private Button sortButton;
    private boolean showSortOptions = false;
    private List<Button> sortOptionButtons = new ArrayList<>();
    
    // Search functionality
    private String searchText = "";
    private boolean isTypingSearch = false;
    private boolean shouldShowKeyboard = false;
    private static final int MAX_SEARCH_LENGTH = 10;
    
    private boolean editorMode;
    private boolean verificationSuccess;
    
    // Add a flag to check if user is admin
    private boolean isAdmin = false;

    // Add cloud background
    private CloudBackground cloudBackground;

    /**
     * Creates a new OnlineLevelSelectScreen with default settings.
     * 
     * @param app The main application instance
     */
    public OnlineLevelSelectScreen(App app) {
        this(app, false, false);
    }
    
    /**
     * Creates a new OnlineLevelSelectScreen with editor mode option.
     * 
     * @param app The main application instance
     * @param editorMode Whether the screen was opened from the level editor
     */
    public OnlineLevelSelectScreen(App app, boolean editorMode) {
        this(app, editorMode, false);
    }
    
    /**
     * Creates a new OnlineLevelSelectScreen with editor mode and verification status.
     * 
     * @param app The main application instance
     * @param editorMode Whether the screen was opened from the level editor
     * @param verificationSuccess Whether a level verification was successful
     */
    public OnlineLevelSelectScreen(App app, boolean editorMode, boolean verificationSuccess) {
        this.app = app;
        this.onlineLevels = new ArrayList<>();
        this.levelCreators = new HashMap<>();
        this.editorMode = editorMode;
        this.verificationSuccess = verificationSuccess;
        
        // Initialize cloud background with textured clouds
        cloudBackground = new CloudBackground(CloudBackground.RenderStyle.TEXTURED);
        
        // Check if the current user is an admin
        if (app.isLoggedIn() && app.getUserData() != null) {
            // Check for admin flag in user data
            Document userData = app.getUserData();
            this.isAdmin = userData.getBoolean("isAdmin", false);
        }
        
        // If verification was successful, update the verification status in the database
        if (verificationSuccess && app.isLoggedIn()) {
            // Get the most recently edited level ID from the app
            int levelToVerify = app.getMostRecentlyEditedLevelId();
            
            if (levelToVerify > 0) {
                updateLevelVerificationStatus(levelToVerify);
                statusMessage = "Level verified successfully!";
            } else {
                statusMessage = "Error: Could not verify level (invalid ID)";
            }
        }
        
        // Initialize UI
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("/fonts/pf_tempesta_seven_bold.ttf");
        
        // Initialize UI with pink theme
        initUI();
        
        // Load levels from database
        loadOnlineLevels();
    }

    /**
     * Initializes the user interface elements.
     */
    private void initUI() {
        // Create back button
        backButton = new Button(20, App.WINDOW_HEIGHT - 60, 150, 40, 0.7f, 0.3f, 0.6f, "Back"); // Dark pink
        
        // Create sort button in top left
        sortButton = new Button(20, 20, 180, 30, 0.9f, 0.5f, 0.8f, currentSortType.getLabel()); // Secondary pink
        
        // Create sort option buttons with pink theme
        int yOffset = 55;
        for (SortType sortType : SortType.values()) {
            Button optionButton = new Button(20, yOffset, 180, 30, 0.8f, 0.4f, 0.7f, sortType.getLabel()); // Hot pink
            sortOptionButtons.add(optionButton);
            yOffset += 35;
        }
        
        // Create level editor button
        createLevelButton = new Button(App.WINDOW_WIDTH - 180, App.WINDOW_HEIGHT - 60, 160, 40, 
                                       1.0f, 0.4f, 0.7f, "Create Level"); // Accent pink
        
        // Create page navigation buttons
        prevPageButton = new Button(App.WINDOW_WIDTH / 2 - 100, App.WINDOW_HEIGHT - 60, 80, 40,
                                  0.4f, 0.4f, 0.7f, "Previous");
        nextPageButton = new Button(App.WINDOW_WIDTH / 2 + 20, App.WINDOW_HEIGHT - 60, 80, 40,
                                  0.4f, 0.4f, 0.7f, "Next");
                                  
        // Add toggle view button - make it wider
        toggleViewButton = new Button(App.WINDOW_WIDTH / 2 - 90, 15, 180, 30, 
                                    0.4f, 0.6f, 0.4f, 
                                    showingUserLevelsOnly ? "Show All Levels" : "Show My Levels");
                                    
        // Initialize confirmation buttons - make them taller to accommodate multiline text
        confirmDeleteButton = new Button(App.WINDOW_WIDTH/2 - 100, App.WINDOW_HEIGHT/2, 90, 50, 
                                       0.8f, 0.2f, 0.2f, "Yes,\nDelete");
        cancelDeleteButton = new Button(App.WINDOW_WIDTH/2 + 10, App.WINDOW_HEIGHT/2, 90, 50, 
                                      0.3f, 0.3f, 0.6f, "Cancel");
        
        // Create search button
        searchButton = new Button(App.WINDOW_WIDTH - 180, 20, 160, 30, 
                                  0.4f, 0.4f, 0.7f, "Search by ID");
    }

    /**
     * Loads online levels from the database.
     */
    private void loadOnlineLevels() {
        isLoading = true;
        statusMessage = "Loading levels...";
        
        new Thread(() -> {
            try (MongoDBConnection mongodb = new MongoDBConnection()) {
                onlineLevels.clear();
                levelCreators.clear();
                
                // Always load from user collection since that's the only source
                loadUserLevels(mongodb);
                
                // Sort levels by ID
                Collections.sort(onlineLevels, (a, b) -> Integer.compare(a.getLevelNumber(), b.getLevelNumber()));
                
                // Sort levels based on current sort method
                sortLevels();
                
                // Don't create buttons here - just set the flag that data is ready
                levelsLoaded = true;
                needsButtonCreation = true;
                
                statusMessage = onlineLevels.isEmpty() ? 
                    "No levels found" : 
                    "Found " + onlineLevels.size() + " levels";
                    
            } catch (Exception e) {
                statusMessage = "Error: " + e.getMessage();
                e.printStackTrace();
            } finally {
                isLoading = false;
            }
        }).start();
    }
    
    /**
     * Loads user-created levels from all users in the database.
     * 
     * @param mongodb Connection to the MongoDB instance
     */
    private void loadUserLevels(MongoDBConnection mongodb) {
        MongoCollection<Document> usersCollection = mongodb.getDatabase().getCollection("data");
        FindIterable<Document> userDocs = usersCollection.find();

        for (Document userDoc : userDocs) {
            try {
                String username = userDoc.getString("username");
                if (username == null) {
                    continue;
                }
                
                Object levelsObj = userDoc.get("Levels");
                if (levelsObj == null) {
                    continue;
                }
                
                Document levelsDoc;
                if (levelsObj instanceof Document) {
                    levelsDoc = (Document) levelsObj;
                } else {
                    continue;
                }
                
                // Extract each level from the user's Levels object
                for (String levelKey : levelsDoc.keySet()) {
                    try {
                        Object levelObj = levelsDoc.get(levelKey);
                        Document levelDoc;
                        
                        if (levelObj instanceof Document) {
                            levelDoc = (Document) levelObj;
                        } else {
                            continue;
                        }
                        
                        // Get ID from document - handle both string and numeric formats
                        String levelId = null;
                        if (levelDoc.containsKey("id")) {
                            if (levelDoc.get("id") instanceof Integer) {
                                levelId = String.valueOf(levelDoc.getInteger("id"));
                            } else {
                                levelId = levelDoc.getString("id");
                            }
                        }
                        
                        // If no ID in document, use the key from Levels object
                        if (levelId == null) {
                            levelId = levelKey.replaceAll("level", "");
                        }
                        
                        // Convert MongoDB Document to LevelConfig
                        LevelConfig level = documentToLevelConfig(levelDoc);
                        
                        // Store the creator's username
                        levelCreators.put(levelId, username);
                        
                        // Add to our list
                        onlineLevels.add(level);
                    } catch (Exception e) {
                        // Skip this level
                    }
                }
            } catch (Exception e) {
                // Skip this user
            }
        }
    }

    /**
     * Converts a MongoDB document to a LevelConfig object.
     * 
     * @param doc The document to convert
     * @return A new LevelConfig object
     */
    @SuppressWarnings("unchecked")
    private LevelConfig documentToLevelConfig(Document doc) {
        LevelConfig.Builder builder = new LevelConfig.Builder();
        
        // Set ID and name
        int id = 0;
        if (doc.containsKey("id")) {
            if (doc.get("id") instanceof Integer) {
                id = doc.getInteger("id");
            } else if (doc.get("id") instanceof String) {
                try {
                    id = Integer.parseInt(doc.getString("id"));
                } catch (NumberFormatException e) {
                    // Use hash code of the string ID as a fallback numeric ID
                    id = Math.abs(doc.getString("id").hashCode());
                }
            }
        }
        
        if (id <= 0) {
            // Generate a unique ID to ensure display
            id = (int) (System.currentTimeMillis() % 100000);
        }
        
        // Ensure we have name
        String name = doc.getString("name");
        if (name == null || name.isEmpty()) {
            name = "Unnamed Level";
        }
        
        builder.withId(id);
        builder.withName(name);
        
        // Process statistics if present
        Document statsDoc = doc.get("statistics", Document.class);
        if (statsDoc != null) {
            int timePlayed = statsDoc.getInteger("timeplayed", 0);
            int likes = statsDoc.getInteger("likes", 0);
            LevelConfig.Statistics stats = new LevelConfig.Statistics(timePlayed, likes);
            builder.withStatistics(stats);
        }
        
        // Process creator info
        String creator = doc.getString("creator");
        if (creator != null) {
            builder.withCreator(creator);
        }
        
        // Set description
        String description = doc.getString("description");
        if (description != null) {
            builder.withDescription(description);
        }
        
        // Process settings
        Document settingsDoc = doc.get("settings", Document.class);
        if (settingsDoc != null) {
            int gridSize = settingsDoc.getInteger("gridSize", 4);
            
            // Create move limits map
            Map<String, Integer> moveLimits = new HashMap<>();
            Document moveLimitsDoc = settingsDoc.get("moveLimits", Document.class);
            if (moveLimitsDoc != null) {
                for (String moveType : moveLimitsDoc.keySet()) {
                    Integer limit = moveLimitsDoc.getInteger(moveType);
                    if (limit != null) {
                        moveLimits.put(moveType, limit);
                    }
                }
            }
            
            String difficulty = settingsDoc.getString("difficulty");
            boolean isNumberMode = settingsDoc.getBoolean("isNumberMode", false);
            
            // Build settings object
            LevelConfig.Settings settings = new LevelConfig.Settings(
                gridSize, moveLimits, difficulty != null ? difficulty : "custom", isNumberMode);
            
            builder.withSettings(settings);
            
            String creatorSetting = settingsDoc.getString("creator");
            if (creatorSetting != null) {
                builder.withCreator(creatorSetting);
            }
        }

        // Process grid (handle both number and color formats)
        Object gridObj = doc.get("grid");
        LevelConfig.Cell[][] processedGrid = null; // Save processed grid for later
        if (gridObj instanceof List) {
            List<List<Document>> gridDocs = (List<List<Document>>) gridObj;
            int gridSize = gridDocs.size();
            LevelConfig.Cell[][] grid = new LevelConfig.Cell[gridSize][gridSize];
            
            for (int i = 0; i < gridSize; i++) {
                List<Document> row = gridDocs.get(i);
                for (int j = 0; j < row.size(); j++) {
                    Document cellDoc = row.get(j);
                    boolean editable = cellDoc.getBoolean("editable", true);
                    
                    // Handle number format (value property)
                    if (cellDoc.containsKey("value")) {
                        int value = cellDoc.getInteger("value");
                        grid[i][j] = new LevelConfig.Cell(value, editable);
                    } 
                    // Handle color format (RGB properties)
                    else if (cellDoc.containsKey("red")) {
                        grid[i][j] = new LevelConfig.Cell(
                            getFloatValue(cellDoc, "red"),
                            getFloatValue(cellDoc, "green"),
                            getFloatValue(cellDoc, "blue"),
                            editable
                        );
                    }
                    // Default empty cell
                    else {
                        grid[i][j] = new LevelConfig.Cell(0, 0, 0, editable);
                    }
                }
            }
            // Store the processed grid for later use with targetPattern
            processedGrid = grid;
            builder.withGrid(grid);
        }
        
        // Process target pattern
        Object targetObj = doc.get("targetPattern");
        if (targetObj instanceof List) {
            List<List<Document>> targetDocs = (List<List<Document>>) targetObj;
            int gridSize = targetDocs.size();
            LevelConfig.Cell[][] target = new LevelConfig.Cell[gridSize][gridSize];
            
            for (int i = 0; i < gridSize; i++) {
                List<Document> row = targetDocs.get(i);
                for (int j = 0; j < row.size(); j++) {
                    Document cellDoc = row.get(j);
                    boolean editable = cellDoc.getBoolean("editable", true);
                    
                    // Handle number format
                    if (cellDoc.containsKey("value")) {
                        int value = cellDoc.getInteger("value");
                        target[i][j] = new LevelConfig.Cell(value, editable);
                    } 
                    // Handle color format
                    else if (cellDoc.containsKey("red")) {
                        target[i][j] = new LevelConfig.Cell(
                            getFloatValue(cellDoc, "red"),
                            getFloatValue(cellDoc, "green"),
                            getFloatValue(cellDoc, "blue"),
                            editable
                        );
                    }
                    // Default empty cell
                    else {
                        target[i][j] = new LevelConfig.Cell(0, 0, 0, editable);
                    }
                }
            }
            builder.withTargetPattern(target);
        } else if (processedGrid != null) {
            // If target pattern is missing and we have a grid, use it as the target
            builder.withTargetPattern(processedGrid);
        }

        // Check for isVerified field and handle it properly
        boolean isVerified = false;
        if (doc.containsKey("isVerified")) {
            isVerified = doc.getBoolean("isVerified", false);
        }
        
        // Add isVerified status to the builder
        builder.withVerified(isVerified);
        
        return builder.build();
    }

    /**
     * Renders the level selection screen.
     */
    @Override
    public void render() {
        // Check if we need to create buttons (only on main thread)
        if (levelsLoaded && needsButtonCreation) {
            createLevelButtons();
        }
        
        glClearColor(1.0f, 0.7f, 0.9f, 1.0f); // Primary pink
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Update and render clouds
        cloudBackground.update();
        cloudBackground.render();
        
        // Draw title
        String title = showingUserLevelsOnly ? "My Levels" : "Online User Levels";
        fontRenderer.renderCenteredText(title, App.WINDOW_WIDTH / 2 - 24, 30, 2.0f, 0.8f, 0.2f, 0.5f, 1.0f); // Pink-hued text
        
        // Draw sort button
        sortButton.setCaption(currentSortType.getLabel());
        sortButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Draw sort options if shown
        if (showSortOptions) {
            // Draw dropdown background
            float dropdownHeight = sortOptionButtons.size() * 35;
            glColor4f(0.9f, 0.5f, 0.8f, 0.9f); // Semi-transparent pink
            glBegin(GL_QUADS);
            glVertex2f(20, 55);
            glVertex2f(200, 55);
            glVertex2f(200, 55 + dropdownHeight);
            glVertex2f(20, 55 + dropdownHeight);
            glEnd();
            
            // Draw options
            for (Button optionButton : sortOptionButtons) {
                optionButton.render((float)currentMouseX, (float)currentMouseY);
            }
        }
        
        // Draw toggle view button
        if (app.isLoggedIn()) {
            toggleViewButton.render((float)currentMouseX, (float)currentMouseY);
        }
        
        // Draw search button and search box
        searchButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Draw search text box
        float searchBoxX = App.WINDOW_WIDTH - 350;
        float searchBoxY = 20;
        float searchBoxWidth = 160;
        float searchBoxHeight = 30;
        
        // Draw search box background
        glColor3f(0.7f, 0.3f, 0.6f); // Dark pink
        glBegin(GL_QUADS);
        glVertex2f(searchBoxX, searchBoxY);
        glVertex2f(searchBoxX + searchBoxWidth, searchBoxY);
        glVertex2f(searchBoxX + searchBoxWidth, searchBoxY + searchBoxHeight);
        glVertex2f(searchBoxX, searchBoxY + searchBoxHeight);
        glEnd();
        
        // Draw search box border (thicker when active)
        float borderWidth = isTypingSearch ? 2.0f : 1.0f;
        glLineWidth(borderWidth);
        glColor3f(isTypingSearch ? 1.0f : 0.8f, isTypingSearch ? 0.4f : 0.2f, isTypingSearch ? 0.7f : 0.5f); // Pink border
        glBegin(GL_LINE_LOOP);
        glVertex2f(searchBoxX, searchBoxY);
        glVertex2f(searchBoxX + searchBoxWidth, searchBoxY);
        glVertex2f(searchBoxX + searchBoxWidth, searchBoxY + searchBoxHeight);
        glVertex2f(searchBoxX, searchBoxY + searchBoxHeight);
        glEnd();
        
        // Draw search text
        String displayText = searchText.isEmpty() ? "Enter Level ID..." : searchText;
        float textColor = searchText.isEmpty() && !isTypingSearch ? 0.6f : 1.0f;
        fontRenderer.renderText(displayText, searchBoxX + 10, searchBoxY + 8, 1.0f, 
                               textColor, textColor, textColor, 1.0f);
        
        // Draw blinking cursor when typing
        if (isTypingSearch && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = searchBoxX + 10 + fontRenderer.getTextWidth(searchText, 1.0f);
            if (cursorX > searchBoxX + searchBoxWidth - 10) {
                cursorX = searchBoxX + searchBoxWidth - 10;
            }
            glLineWidth(1.0f);
            glColor3f(1.0f, 1.0f, 1.0f);
            glBegin(GL_LINES);
            glVertex2f(cursorX, searchBoxY + 5);
            glVertex2f(cursorX, searchBoxY + searchBoxHeight - 5);
            glEnd();
        }
        
        // Draw status message and page info
        fontRenderer.renderCenteredText(statusMessage, App.WINDOW_WIDTH / 2, 60, 1.0f, 0.9f, 0.7f, 0.7f, 1.0f);
        
        if (!onlineLevels.isEmpty()) {
            int totalPages = (onlineLevels.size() - 1) / LEVELS_PER_PAGE + 1;
            String pageInfo = String.format("Page %d of %d", currentPage + 1, totalPages);
            fontRenderer.renderCenteredText(pageInfo, App.WINDOW_WIDTH / 2, 85, 1.0f);
        }
        
        // Show loading indicator if loading
        if (isLoading) {
            drawLoadingSpinner();
        } else {
            // Draw level buttons
            if (!isLoading) {
                for (Button button : levelButtons) {
                    button.render((float)currentMouseX, (float)currentMouseY);
                }
                
                // Render edit buttons only in My Levels view
                if (showingUserLevelsOnly && app.isLoggedIn()) {
                    for (Button button : editButtons.values()) {
                        button.render((float)currentMouseX, (float)currentMouseY);
                    }
                }
                
                // Always render delete buttons if admin, or if in My Levels view
                if ((showingUserLevelsOnly && app.isLoggedIn()) || isAdmin) {
                    for (Button button : deleteButtons.values()) {
                        button.render((float)currentMouseX, (float)currentMouseY);
                    }
                }
            }
        }
        
        // Draw bottom buttons
        backButton.render((float)currentMouseX, (float)currentMouseY);
        createLevelButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Calculate total pages based on filtered levels
        int totalPages = filteredLevels.isEmpty() ? 1 : (filteredLevels.size() - 1) / LEVELS_PER_PAGE + 1;
        
        // Only show Previous button if we're not on the first page
        if (currentPage > 0) {
            prevPageButton.render((float)currentMouseX, (float)currentMouseY);
        }
        
        // Only show Next button if we're not on the last page
        if (currentPage < totalPages - 1) {
            nextPageButton.render((float)currentMouseX, (float)currentMouseY);
        }
        
        // Draw delete confirmation dialog if active
        if (showingDeleteConfirmation) {
            // Draw overlay
            glColor4f(0.9f, 0.5f, 0.8f, 0.7f); // Semi-transparent pink
            glBegin(GL_QUADS);
            glVertex2f(0, 0);
            glVertex2f(App.WINDOW_WIDTH, 0);
            glVertex2f(App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
            glVertex2f(0, App.WINDOW_HEIGHT);
            glEnd();
            
            // Draw confirmation box
            float boxWidth = 300;
            float boxHeight = 150;
            float boxX = App.WINDOW_WIDTH/2 - boxWidth/2;
            float boxY = App.WINDOW_HEIGHT/2 - boxHeight/2;
            
            glColor3f(0.8f, 0.4f, 0.7f); // Hot pink
            glBegin(GL_QUADS);
            glVertex2f(boxX, boxY);
            glVertex2f(boxX + boxWidth, boxY);
            glVertex2f(boxX + boxWidth, boxY + boxHeight);
            glVertex2f(boxX, boxY + boxHeight);
            glEnd();
            
            // Draw text
            fontRenderer.renderCenteredText("Confirm Deletion", App.WINDOW_WIDTH/3 + 45, boxY /1.5f + 30, 1.5f);
            fontRenderer.renderCenteredText("Are you sure you want to delete this level?", 
                                           App.WINDOW_WIDTH/2, boxY -60 , 1.0f);
            
            // Draw buttons
            confirmDeleteButton.render((float)currentMouseX, (float)currentMouseY);
            cancelDeleteButton.render((float)currentMouseX, (float)currentMouseY);
        }
    }
    
    /**
     * Draws an animated loading spinner.
     */
    private void drawLoadingSpinner() {
        float centerX = App.WINDOW_WIDTH / 2;
        float centerY = App.WINDOW_HEIGHT / 2;
        float size = 30;
        float time = (float)System.currentTimeMillis() / 200.0f; // Animation speed
        
        glPushMatrix();
        glTranslatef(centerX, centerY, 0);
        glRotatef(time % 360, 0, 0, 1);
        
        for (int i = 0; i < 8; i++) {
            float angle = (float)(i * Math.PI / 4.0);
            float alpha = 0.2f + 0.8f * ((i + (int)(time / 45)) % 8) / 8.0f;
            
            // Pink spinner
            float r = 1.0f;
            float g = 0.2f + (i % 3) * 0.2f; // Varying pink shades
            float b = 0.7f;
            
            float x1 = (float)Math.cos(angle) * size;
            float y1 = (float)Math.sin(angle) * size;
            float x2 = (float)Math.cos(angle) * (size * 0.5f);
            float y2 = (float)Math.sin(angle) * (size * 0.5f);
            
            glColor4f(r, g, b, alpha);
            glBegin(GL_TRIANGLES);
            glVertex2f(x1, y1);
            glVertex2f(x2, y2);
            glVertex2f((float)Math.cos(angle + 0.3) * size, (float)Math.sin(angle + 0.3) * size);
            glEnd();
        }
        
        glPopMatrix();
    }

    /**
     * Handles mouse click events for buttons and level selection.
     * 
     * @param mouseX X coordinate of the mouse click
     * @param mouseY Y coordinate of the mouse click
     */
    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
        // Check if clicked on search box
        float searchBoxX = App.WINDOW_WIDTH - 350;
        float searchBoxY = 20;
        float searchBoxWidth = 160;
        float searchBoxHeight = 30;
        
        if (mx >= searchBoxX && mx <= searchBoxX + searchBoxWidth &&
            my >= searchBoxY && my <= searchBoxY + searchBoxHeight) {
            isTypingSearch = true;
            shouldShowKeyboard = true;
            return;
        } else if (isTypingSearch) {
            // Clicking outside the search box ends typing
            isTypingSearch = false;
        }
        
        // Check if search button was clicked
        if (searchButton.handleMouseClick(mx, my)) {
            searchByLevelId();
            return;
        }
        
        // Handle sort button and options
        if (sortButton.handleMouseClick(mx, my)) {
            showSortOptions = !showSortOptions;
            return;
        }
        
        if (showSortOptions) {
            for (int i = 0; i < sortOptionButtons.size(); i++) {
                if (sortOptionButtons.get(i).handleMouseClick(mx, my)) {
                    currentSortType = SortType.values()[i];
                    showSortOptions = false;
                    sortLevels();
                    needsButtonCreation = true;
                    return;
                }
            }
        }
        
        // Check back button
        if (backButton.handleMouseClick(mx, my)) {
            app.setCurrentScreen(new LevelSelect(app));
            return;
        }
        
        // Check create level button
        if (createLevelButton.handleMouseClick(mx, my)) {
            app.setCurrentScreen(new LevelEditorScreen(app));
            return;
        }
        
        // Handle toggle view button
        if (app.isLoggedIn() && toggleViewButton.handleMouseClick(mx, my)) {
            showingUserLevelsOnly = !showingUserLevelsOnly;
            toggleViewButton.setCaption(showingUserLevelsOnly ? "Show All Levels" : "Show My Levels");
            needsButtonCreation = true;
            currentPage = 0; // Reset to first page when switching views
            return;
        }
        
        // Check pagination buttons
        int totalPages = filteredLevels.isEmpty() ? 1 : (filteredLevels.size() - 1) / LEVELS_PER_PAGE + 1;
        
        if (currentPage > 0 && prevPageButton.handleMouseClick(mx, my)) {
            currentPage--;
            needsButtonCreation = true;  // Set flag instead of directly creating buttons
            return;
        }
        
        if (currentPage < totalPages - 1 && nextPageButton.handleMouseClick(mx, my)) {
            currentPage++;
            needsButtonCreation = true;  // Set flag instead of directly creating buttons
            return;
        }
        
        // Check level buttons and edit/delete buttons
        if (!isLoading) {
            if (showingDeleteConfirmation) {
                // Handle delete confirmation dialog
                if (confirmDeleteButton.handleMouseClick(mx, my)) {
                    deleteLevel(levelToDelete);
                    showingDeleteConfirmation = false;
                    levelToDelete = -1;
                    return;
                }
                if (cancelDeleteButton.handleMouseClick(mx, my)) {
                    showingDeleteConfirmation = false;
                    levelToDelete = -1;
                    return;
                }
                return; // Block other clicks while dialog is open
            }
            
            // First check delete buttons - always check for admin
            if ((showingUserLevelsOnly && app.isLoggedIn()) || isAdmin) {
                for (Map.Entry<Integer, Button> entry : deleteButtons.entrySet()) {
                    if (entry.getValue().handleMouseClick(mx, my)) {
                        // Show confirmation dialog instead of deleting immediately
                        levelToDelete = currentPage * LEVELS_PER_PAGE + entry.getKey();
                        showingDeleteConfirmation = true;
                        return;
                    }
                }
            }
            
            // Then check edit buttons
            if (showingUserLevelsOnly && app.isLoggedIn()) {
                for (Map.Entry<Integer, Button> entry : editButtons.entrySet()) {
                    if (entry.getValue().handleMouseClick(mx, my)) {
                        int levelIndex = currentPage * LEVELS_PER_PAGE + entry.getKey();
                        if (levelIndex < filteredLevels.size()) {
                            // Go to Level Editor with the selected level for editing
                            LevelConfig levelToEdit = filteredLevels.get(levelIndex);
                            app.setCurrentScreen(new LevelEditorScreen(app, levelToEdit));
                        }
                        return;
                    }
                }
            }
            
            // Then check regular level buttons
            for (int i = 0; i < levelButtons.size(); i++) {
                if (levelButtons.get(i).handleMouseClick(mx, my)) {
                    int levelIndex = currentPage * LEVELS_PER_PAGE + i;
                    if (levelIndex < filteredLevels.size()) {
                        app.setCurrentScreen(new GameScreen(app, filteredLevels.get(levelIndex)));
                    }
                    return;
                }
            }
        }
    }
    
    /**
     * Searches for a level by its ID.
     */
    private void searchByLevelId() {
        if (searchText.isEmpty()) {
            return;
        }
        
        try {
            int levelId = Integer.parseInt(searchText);
            boolean found = false;
            
            for (int i = 0; i < onlineLevels.size(); i++) {
                LevelConfig level = onlineLevels.get(i);
                if (level.getLevelNumber() == levelId) {
                    // Found the level, start playing it
                    app.setCurrentScreen(new GameScreen(app, level));
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                statusMessage = "Level ID " + levelId + " not found";
            }
        } catch (NumberFormatException e) {
            statusMessage = "Invalid level ID";
        }
        
        // Clear search text after searching
        searchText = "";
    }

    /**
     * Creates the level selection buttons for the current page.
     */
    private void createLevelButtons() {
        levelButtons.clear();
        editButtons.clear();
        deleteButtons.clear(); // Clear delete buttons
        
        // Filter levels based on view mode
        filteredLevels.clear();
        
        if (showingUserLevelsOnly && app.isLoggedIn()) {
            // Only show levels created by the logged-in user
            String currentUser = app.getUsername();
            for (LevelConfig level : onlineLevels) {
                String levelId = String.valueOf(level.getLevelNumber());
                String creator = levelCreators.getOrDefault(levelId, "Unknown");
                
                if (creator.equals(currentUser)) {
                    filteredLevels.add(level);
                }
            }
        } else {
            // Show all levels
            filteredLevels.addAll(onlineLevels);
        }
        
        if (filteredLevels.isEmpty()) {
            return;
        }
        
        int startIndex = currentPage * LEVELS_PER_PAGE;
        int endIndex = Math.min(startIndex + LEVELS_PER_PAGE, filteredLevels.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            LevelConfig level = filteredLevels.get(i);
            int relativeIndex = i - startIndex;
            
            int row = relativeIndex / GRID_COLS;
            int col = relativeIndex % GRID_COLS;
            
            float x = 50 + col * (BUTTON_WIDTH + BUTTON_SPACING_X);
            float y = 120 + row * (BUTTON_HEIGHT + BUTTON_SPACING_Y);
            
            String creator = levelCreators.getOrDefault(String.valueOf(level.getLevelNumber()), "Unknown");
            
            // Add verification status to button caption
            String verificationStatus = level.isVerified() ? "" : "\n[Unverified]";
            String buttonText = level.getName() + verificationStatus + "\nBy: " + creator;
            
            // Color based on level properties and verification
            float r = level.getSettings().isNumberMode() ? 0.9f : 1.0f;
            float g = level.getSettings().isNumberMode() ? 0.5f : 0.4f;
            float b = level.getSettings().isNumberMode() ? 0.8f : 0.7f;
            
            // Dim unverified levels but keep pink theme
            if (!level.isVerified()) {
                r *= 0.8f;
                g *= 0.8f;
                b *= 0.8f;
            }
            
            Button levelButton = new Button(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, r, g, b, buttonText);
            levelButtons.add(levelButton);
            
            // If user is the creator OR user is admin, add edit/delete buttons
            boolean canEdit = app.isLoggedIn() && 
                             (app.getUsername() != null && app.getUsername().equals(creator));
            
            boolean canDelete = canEdit || isAdmin; // Admin can delete any level
            
            if (showingUserLevelsOnly && app.isLoggedIn() && canEdit) {
                // Create edit button (only for user's own levels)
                Button editButton = new Button(
                    x + BUTTON_WIDTH - 60,
                    y + BUTTON_HEIGHT - 25,
                    50, 20,
                    0.7f, 0.3f, 0.6f, "Edit" // Dark pink
                );
                editButtons.put(relativeIndex, editButton);
            }
            
            // Add delete button for both user's levels and admin (for any level)
            if ((showingUserLevelsOnly && app.isLoggedIn() && canEdit) || 
                (app.isLoggedIn() && isAdmin)) {
                
                // Create delete button
                Button deleteButton = new Button(
                    x + BUTTON_WIDTH - (canEdit ? 130 : 60), // Position based on whether edit button exists
                    y + BUTTON_HEIGHT - 25,
                    60, 20,
                    1.0f, 0.3f, 0.6f, // Hot pink
                    isAdmin && !canEdit ? "Admin Del" : "Delete"
                );
                deleteButtons.put(relativeIndex, deleteButton);
            }
        }
        
        needsButtonCreation = false;
    }

    /**
     * Sorts the list of levels based on the current sort type.
     */
    private void sortLevels() {
        switch (currentSortType) {
            case BY_PLAYS:
                Collections.sort(onlineLevels, (a, b) -> {
                    int playsA = a.getStatistics() != null ? a.getStatistics().getTimePlayed() : 0;
                    int playsB = b.getStatistics() != null ? b.getStatistics().getTimePlayed() : 0;
                    return Integer.compare(playsB, playsA); // Descending
                });
                break;
            case BY_LIKES:
                Collections.sort(onlineLevels, (a, b) -> {
                    int likesA = a.getStatistics() != null ? a.getStatistics().getLikes() : 0;
                    int likesB = b.getStatistics() != null ? b.getStatistics().getLikes() : 0;
                    return Integer.compare(likesB, likesA); // Descending
                });
                break;
            case BY_ID:
            default:
                Collections.sort(onlineLevels, (a, b) -> Integer.compare(a.getLevelNumber(), b.getLevelNumber()));
                break;
        }
    }
    
    /**
     * Safely extracts a float value from a document, handling different numeric types.
     * 
     * @param doc The document to extract from
     * @param key The key to extract
     * @return The float value, or 0.0f if not found or invalid
     */
    private float getFloatValue(Document doc, String key) {
        Object value = doc.get(key);
        if (value instanceof Integer) {
            return ((Integer) value).floatValue();
        } else if (value instanceof Double) {
            return ((Double) value).floatValue();
        } else if (value instanceof Long) {
            return ((Long) value).floatValue();
        } else {
            return 0.0f; // Default value if type is not numeric or null
        }
    }
    
    /**
     * Deletes a level from the database.
     * 
     * @param levelIndex The index of the level to delete in the filtered levels list
     */
    private void deleteLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= filteredLevels.size()) {
            return;
        }
        
        LevelConfig levelToDelete = filteredLevels.get(levelIndex);
        String levelKey = "level" + levelToDelete.getLevelNumber();
        String levelCreator = levelCreators.getOrDefault(String.valueOf(levelToDelete.getLevelNumber()), null);
        
        try (MongoDBConnection mongodb = new MongoDBConnection()) {
            if (app.isLoggedIn() && app.getUserData() != null) {
                String username = app.getUsername();
                boolean isOwner = levelCreator != null && levelCreator.equals(username);
                
                // Only allow if user is owner or admin
                if (!isOwner && !isAdmin) {
                    statusMessage = "Error: You don't have permission to delete this level";
                    return;
                }
                
                if (isAdmin && !isOwner) {
                    // Admin deleting someone else's level
                    
                    // Find the level owner's document
                    Document creatorFilter = new Document("username", levelCreator);
                    Document creatorDoc = mongodb.getDatabase().getCollection("data").find(creatorFilter).first();
                    
                    if (creatorDoc != null && creatorDoc.containsKey("Levels")) {
                        Document levelsDoc = creatorDoc.get("Levels", Document.class);
                        if (levelsDoc != null && levelsDoc.containsKey(levelKey)) {
                            // Remove the level
                            levelsDoc.remove(levelKey);
                            
                            // Update database
                            Document update = new Document("$set", new Document("Levels", levelsDoc));
                            mongodb.getDatabase().getCollection("data").updateOne(creatorFilter, update);
                            
                            // Reload levels
                            loadOnlineLevels();
                            statusMessage = "Level deleted successfully (admin action)";
                        } else {
                            statusMessage = "Error: Level not found in user's data";
                        }
                    } else {
                        statusMessage = "Error: Could not find level owner's data";
                    }
                } else {
                    // Normal deletion of own level
                    Document user = app.getUserData();
                    
                    // Get Levels subdocument
                    Document levelsDoc = user.get("Levels", Document.class);
                    if (levelsDoc == null || !levelsDoc.containsKey(levelKey)) {
                        statusMessage = "Error: Level not found or you don't have permission to delete it";
                        return;
                    }
                    
                    // Remove the level from the Levels document
                    levelsDoc.remove(levelKey);
                    
                    // Update database
                    Document filter = new Document("username", username);
                    Document update = new Document("$set", new Document("Levels", levelsDoc));
                    
                    mongodb.getDatabase().getCollection("data").updateOne(filter, update);
                    
                    // Update cached user data
                    app.setLoggedInUser(mongodb.getDatabase().getCollection("data").find(filter).first());
                    
                    // Reload levels
                    loadOnlineLevels();
                    statusMessage = "Level deleted successfully";
                }
            } else {
                statusMessage = "Error: You must be logged in to delete levels";
            }
        } catch (Exception e) {
            statusMessage = "Error deleting level: " + e.getMessage();
            System.err.println("Error deleting level: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Updates the verification status of a level in the database.
     * 
     * @param levelId The ID of the level to update
     */
    private void updateLevelVerificationStatus(int levelId) {
        new Thread(() -> {
            try (MongoDBConnection mongodb = new MongoDBConnection()) {
                // Find the user who owns this level
                MongoCollection<Document> usersCollection = mongodb.getDatabase().getCollection("data");
                String username = app.getUsername();
                
                // Create a query to find the user's document
                Document query = new Document("username", username);
                Document userDoc = usersCollection.find(query).first();
                
                if (userDoc != null) {
                    if (userDoc.containsKey("Levels")) {
                        Document levelsDoc = userDoc.get("Levels", Document.class);
                        
                        if (levelsDoc != null && levelsDoc.containsKey("level" + levelId)) {
                            // Update the isVerified field to true
                            Document update = new Document("$set", 
                                new Document("Levels.level" + levelId + ".isVerified", true));
                            
                            usersCollection.updateOne(query, update);
                            
                            // Also update the app's user data to reflect the change
                            if (app.getUserData() != null) {
                                Document userData = app.getUserData();
                                Document userLevels = userData.get("Levels", Document.class);
                                if (userLevels != null && userLevels.containsKey("level" + levelId)) {
                                    Document levelDoc2 = userLevels.get("level" + levelId, Document.class);
                                    if (levelDoc2 != null) {
                                        levelDoc2.put("isVerified", true);
                                    }
                                }
                            }
                            
                            // Also reload online levels to reflect changes
                            loadOnlineLevels();
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating level verification status: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
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
    public void handleMouseRelease(double mouseX, double mouseY) {}
    
    /**
     * Handles key press events.
     * 
     * @param key The key code
     * @param action The action (press, release, etc.)
     */
    @Override
    public void handleKeyPress(int key, int action) {
        if (isTypingSearch && key == GLFW_KEY_ENTER && action == GLFW_PRESS) {
            searchByLevelId();
            isTypingSearch = false;
        } else if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            isTypingSearch = false;
        }
    }
    
    /**
     * Handles character input events for the search field.
     * 
     * @param codepoint The Unicode code point of the character
     */
    @Override
    public void handleCharInput(int codepoint) {
        if (isTypingSearch) {
            // Handle backspace (8) and delete (127)
            if (codepoint == 8 || codepoint == 127) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                return;
            }
            
            // Only add numbers to search field
            char c = (char) codepoint;
            if (Character.isDigit(c) && searchText.length() < MAX_SEARCH_LENGTH) {
                searchText += c;
            }
        }
    }
}