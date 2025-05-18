package thegame.screens;

import static org.lwjgl.opengl.GL11.*;

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
    
    // Mouse position
    private double currentMouseX = 0;
    private double currentMouseY = 0;

    // Flags to manage loading and button creation
    private boolean levelsLoaded = false;
    private boolean needsButtonCreation = false;
    private boolean showingUserLevelsOnly = false;
    private List<LevelConfig> filteredLevels = new ArrayList<>();
    private Map<Integer, Button> editButtons = new HashMap<>(); // New: edit buttons for user's own levels

    public OnlineLevelSelectScreen(App app) {
        this(app, false);
    }
    
    public OnlineLevelSelectScreen(App app, boolean userLevelsMode) {
        this.app = app;
        this.onlineLevels = new ArrayList<>();
        this.levelCreators = new HashMap<>();
        this.showingUserLevelsOnly = userLevelsMode;
        
        // Initialize UI
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("F:/temp/theGame/untitledGame/app/src/main/resources/fonts/pf_tempesta_seven_bold.ttf");
        
        // Create back button
        backButton = new Button(20, App.WINDOW_HEIGHT - 60, 150, 40, 0.3f, 0.3f, 0.6f, "Back");
        
        // Create level editor button
        createLevelButton = new Button(App.WINDOW_WIDTH - 180, App.WINDOW_HEIGHT - 60, 160, 40, 
                                      0.2f, 0.7f, 0.4f, "Create Level");
        
        // Create page navigation buttons
        prevPageButton = new Button(App.WINDOW_WIDTH / 2 - 100, App.WINDOW_HEIGHT - 60, 80, 40,
                                  0.4f, 0.4f, 0.7f, "Previous");
        nextPageButton = new Button(App.WINDOW_WIDTH / 2 + 20, App.WINDOW_HEIGHT - 60, 80, 40,
                                  0.4f, 0.4f, 0.7f, "Next");
                                  
        // Add toggle view button
        toggleViewButton = new Button(App.WINDOW_WIDTH / 2 - 70, 15, 140, 30, 
                                    0.4f, 0.6f, 0.4f, 
                                    showingUserLevelsOnly ? "Show All Levels" : "Show My Levels");
                                    
        // Load levels from database
        loadOnlineLevels();
    }

    private void loadOnlineLevels() {
        isLoading = true;
        statusMessage = "Loading levels...";
        
        new Thread(() -> {
            try (MongoDBConnection mongodb = new MongoDBConnection()) {
                System.out.println("Connected to MongoDB successfully");
                
                onlineLevels.clear();
                levelCreators.clear();
                
                // Always load from user collection since that's the only source
                loadUserLevels(mongodb);
                
                // Sort levels by ID
                Collections.sort(onlineLevels, (a, b) -> Integer.compare(a.getLevelNumber(), b.getLevelNumber()));
                
                // Don't create buttons here - just set the flag that data is ready
                levelsLoaded = true;
                needsButtonCreation = true;
                
                statusMessage = onlineLevels.isEmpty() ? 
                    "No levels found" : 
                    "Found " + onlineLevels.size() + " levels";
                    
            } catch (Exception e) {
                statusMessage = "Error: " + e.getMessage();
                System.err.println("Error loading levels: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isLoading = false;
            }
        }).start();
    }
    
    private void loadUserLevels(MongoDBConnection mongodb) {
        MongoCollection<Document> usersCollection = mongodb.getDatabase().getCollection("data");
        FindIterable<Document> userDocs = usersCollection.find();

        for (Document userDoc : userDocs) {
            try {
                String username = userDoc.getString("username");
                if (username == null) {
                    System.out.println("Skipping document without username: " + userDoc.get("_id"));
                    continue;
                }
                
                // Debug info
                System.out.println("Processing user: " + username);
                
                Object levelsObj = userDoc.get("Levels");
                if (levelsObj == null) {
                    System.out.println("User has no Levels field: " + username);
                    continue;
                }
                
                Document levelsDoc;
                if (levelsObj instanceof Document) {
                    levelsDoc = (Document) levelsObj;
                } else {
                    System.out.println("Levels field is not a Document but a: " + levelsObj.getClass().getName());
                    continue;
                }
                
                System.out.println("Found " + levelsDoc.keySet().size() + " levels for user: " + username);
                
                // Extract each level from the user's Levels object
                for (String levelKey : levelsDoc.keySet()) {
                    try {
                        Object levelObj = levelsDoc.get(levelKey);
                        Document levelDoc;
                        
                        if (levelObj instanceof Document) {
                            levelDoc = (Document) levelObj;
                        } else {
                            System.out.println("Level is not a Document: " + levelKey);
                            continue;
                        }
                        
                        // Convert MongoDB Document to LevelConfig
                        LevelConfig level = documentToLevelConfig(levelDoc);
                        
                        // Store the creator's username - ensure it's preserved
                        // Get ID from either string or int format
                        String levelId;
                        if (levelDoc.containsKey("id")) {
                            if (levelDoc.get("id") instanceof Integer) {
                                levelId = levelDoc.getInteger("id").toString();
                            } else {
                                levelId = levelDoc.getString("id");
                            }
                        } else {
                            // Use the level key itself if no ID present
                            levelId = levelKey;
                        }
                        
                        levelCreators.put(levelId, username);
                        System.out.println("Added level: " + levelId + " by " + username + " - " + level.getName());
                        
                        onlineLevels.add(level);
                    } catch (Exception e) {
                        System.err.println("Error processing level " + levelKey + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing user document: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    // Modified to filter levels based on current view mode
    private void createLevelButtons() {
        levelButtons.clear();
        editButtons.clear(); // Clear edit buttons too
        
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
            String buttonText = level.getName() + "\nBy: " + creator;
            
            // Color based on level properties
            float r = level.getSettings().isNumberMode() ? 0.2f : 0.3f;
            float g = level.getSettings().isNumberMode() ? 0.5f : 0.4f;
            float b = 0.6f;
            
            Button levelButton = new Button(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, r, g, b, buttonText);
            levelButtons.add(levelButton);
            
            // If we're showing user's levels and the current user is the creator, add an edit button
            if (showingUserLevelsOnly && app.isLoggedIn() && 
                app.getUsername() != null && app.getUsername().equals(creator)) {
                
                // Create a small edit button in the bottom right of the level button
                Button editButton = new Button(
                    x + BUTTON_WIDTH - 30, // Position it at the bottom right
                    y + BUTTON_HEIGHT - 25,
                    25, 20, // Small button
                    0.2f, 0.7f, 0.4f, "Edit"
                );
                editButtons.put(i, editButton);
            }
        }
        
        needsButtonCreation = false;
    }

    @SuppressWarnings("unchecked")
    private LevelConfig documentToLevelConfig(Document doc) {
        LevelConfig.Builder builder = new LevelConfig.Builder();
        
        // Debug print to see the structure
        System.out.println("Converting document: " + doc.toJson().substring(0, Math.min(100, doc.toJson().length())) + "...");
        
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
                    id = doc.getString("id").hashCode();
                }
            }
        }
        
        builder.withId(id > 0 ? id : 0);
        builder.withName(doc.getString("name"));
        
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
            
            String creator = settingsDoc.getString("creator");
            if (creator != null) {
                builder.withCreator(creator);
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
                            cellDoc.getDouble("red").floatValue(),
                            cellDoc.getDouble("green").floatValue(),
                            cellDoc.getDouble("blue").floatValue(),
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
                            cellDoc.getDouble("red").floatValue(),
                            cellDoc.getDouble("green").floatValue(),
                            cellDoc.getDouble("blue").floatValue(),
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

        return builder.build();
    }

    @Override
    public void render() {
        // Check if we need to create buttons (only on main thread)
        if (levelsLoaded && needsButtonCreation) {
            createLevelButtons();
        }
        
        glClearColor(0.1f, 0.1f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Draw title
        String title = showingUserLevelsOnly ? "My Levels" : "Online User Levels";
        fontRenderer.renderCenteredText(title, App.WINDOW_WIDTH / 2, 30, 2.0f);
        
        // Draw toggle view button
        if (app.isLoggedIn()) {
            toggleViewButton.render((float)currentMouseX, (float)currentMouseY);
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
                
                // Also render edit buttons when in "My Levels" mode
                if (showingUserLevelsOnly && app.isLoggedIn()) {
                    for (Button button : editButtons.values()) {
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
    }
    
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
            
            float x1 = (float)Math.cos(angle) * size;
            float y1 = (float)Math.sin(angle) * size;
            float x2 = (float)Math.cos(angle) * (size * 0.5f);
            float y2 = (float)Math.sin(angle) * (size * 0.5f);
            
            glColor4f(0.8f, 0.8f, 1.0f, alpha);
            glBegin(GL_TRIANGLES);
            glVertex2f(x1, y1);
            glVertex2f(x2, y2);
            glVertex2f((float)Math.cos(angle + 0.3) * size, (float)Math.sin(angle + 0.3) * size);
            glEnd();
        }
        
        glPopMatrix();
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
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
        
        // Check level buttons and edit buttons
        if (!isLoading) {
            // First check edit buttons so they have priority over level buttons
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
    
    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {}
    
    @Override
    public void handleKeyPress(int key, int action) {}
    
    @Override
    public void handleCharInput(int codepoint) {}
}