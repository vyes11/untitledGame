package thegame.screens;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import thegame.App;
import thegame.Screen;
import thegame.utils.ImGuiUtils;
import thegame.utils.LevelConfig;
import thegame.utils.MongoDBConnection;

public class OnlineLevelSelectScreen implements Screen {
    private List<LevelConfig> onlineLevels;
    private final App app;
    private String statusMessage = "";
    private boolean isLoading = false;

    // Layout constants
    private static final int GRID_COLS = 5;
    private static final float BUTTON_SIZE = 80.0f;
    private static final float BUTTON_SPACING = 20.0f;

    public OnlineLevelSelectScreen(App app) {
        this.app = app;
        this.onlineLevels = new ArrayList<>();
        loadOnlineLevels();
    }

    private void loadOnlineLevels() {
        isLoading = true;
        statusMessage = "Loading levels...";
        
        new Thread(() -> {
            try (MongoDBConnection mongodb = new MongoDBConnection()) {
                System.out.println("Connected to MongoDB successfully");
                MongoCollection<Document> levelsCollection = mongodb.getLevelsCollection();

                onlineLevels.clear();

                // Get all level documents
                FindIterable<Document> levelDocs = levelsCollection.find();

                for (Document levelDoc : levelDocs) {
                    try {
                        // Convert MongoDB Document to LevelConfig
                        LevelConfig level = documentToLevelConfig(levelDoc);
                        onlineLevels.add(level);
                    } catch (Exception e) {
                        System.err.println("Error processing level document: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                statusMessage = onlineLevels.isEmpty() ? 
                    "No online levels found" : 
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

    @SuppressWarnings("unchecked")
    private LevelConfig documentToLevelConfig(Document doc) {
        LevelConfig.Builder builder = new LevelConfig.Builder();
        
        // Set ID and name
        builder.withId(doc.getInteger("id", 0));
        builder.withName(doc.getString("name"));
        
        // Process settings
        Document settingsDoc = doc.get("settings", Document.class);
        if (settingsDoc != null) {
            LevelConfig.Settings settings = new LevelConfig.Settings(
                settingsDoc.getInteger("gridSize", 3),
                settingsDoc.getInteger("maxMoves", 10),
                settingsDoc.getString("difficulty")
            );
            builder.withSettings(settings);
            
            boolean isNumberMode = settingsDoc.getBoolean("isNumberMode", false);
            builder.withNumberMode(isNumberMode);
            
            String creator = settingsDoc.getString("creator");
            if (creator != null) {
                builder.withCreator(creator);
            }
        }

        // Process grid
        Object gridObj = doc.get("grid");
        if (gridObj instanceof List) {
            List<List<Document>> gridDocs = (List<List<Document>>) gridObj;
            int gridSize = gridDocs.size();
            LevelConfig.Cell[][] grid = new LevelConfig.Cell[gridSize][];

            for (int i = 0; i < gridSize; i++) {
                List<Document> row = gridDocs.get(i);
                grid[i] = new LevelConfig.Cell[row.size()];

                for (int j = 0; j < row.size(); j++) {
                    Document cellDoc = row.get(j);
                    grid[i][j] = new LevelConfig.Cell(
                        cellDoc.getDouble("red").floatValue(),
                        cellDoc.getDouble("green").floatValue(),
                        cellDoc.getDouble("blue").floatValue(),
                        cellDoc.getBoolean("isCenter", false)
                    );
                }
            }
            builder.withGrid(grid);
            
            // For simplicity, use the same grid as target pattern
            // If your document has a separate targetPattern field, process it here
            builder.withTargetPattern(grid);
        }

        return builder.build();
    }

    @Override
    public void render() {
        float windowWidth = ImGui.getIO().getDisplaySizeX();
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        
        // Create fullscreen window
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.begin("Online Level Select", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | 
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar);
        
        // Title
        float titleWidth = ImGui.calcTextSize("Online Levels").x;
        ImGui.setCursorPosX((windowWidth - titleWidth) / 2);
        ImGui.setCursorPosY(20);
        ImGui.textColored(0.9f, 0.9f, 1.0f, 1.0f, "Online Levels");
        
        // Status message
        ImGui.setCursorPosX((windowWidth - ImGui.calcTextSize(statusMessage).x) / 2);
        ImGui.setCursorPosY(50);
        ImGui.textColored(0.9f, 0.7f, 0.7f, 1.0f, statusMessage);
        
        // Show loading indicator if loading
        if (isLoading) {
            float spinnerSize = 20;
            float centerX = windowWidth / 2;
            float centerY = windowHeight / 2;
            
            // Draw a simple "loading" spinner
            float time = (float)ImGui.getTime() * 5.0f;
            for (int i = 0; i < 6; i++) {
                float angle = time + (float)i * 3.14159f / 3.0f;
                float x = centerX + (float)Math.cos(angle) * spinnerSize;
                float y = centerY + (float)Math.sin(angle) * spinnerSize;
                ImGui.getWindowDrawList().addCircleFilled(x, y, spinnerSize, ImGui.getColorU32(ImGuiCol.ButtonHovered));
            }
        } else {
            // Grid layout for level buttons
            for (int i = 0; i < onlineLevels.size(); i++) {
                LevelConfig level = onlineLevels.get(i);
                
                float x = (i % GRID_COLS) * (BUTTON_SIZE + BUTTON_SPACING);
                float y = (i / GRID_COLS) * (BUTTON_SIZE + BUTTON_SPACING) + 100;
                
                ImGui.setCursorPosX(x);
                ImGui.setCursorPosY(y);
                
                // Level button
                if (ImGui.button(level.getName(), BUTTON_SIZE, BUTTON_SIZE)) {
                    app.setCurrentScreen(new GameScreen(app, level));
                }
            }
        }

        ImGui.end();
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        // No longer needed, handled by ImGui
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        // No longer needed, handled by ImGui
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {}
    @Override
    public void handleKeyPress(int key, int action) {}
}