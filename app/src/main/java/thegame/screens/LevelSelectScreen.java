package thegame.screens;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;

import thegame.App;
import thegame.Screen;
import thegame.utils.ImGuiUtils;
import thegame.utils.LevelConfig;

public class LevelSelectScreen implements Screen {
    // Grid layout constants
    private static final int GRID_COLS = 5;
    private static final int MAX_LEVELS = 20;
    
    // Grid appearance
    private static final float BUTTON_SIZE = 80.0f;
    private static final float BUTTON_SPACING = 20.0f;
    
    private final App app;
    private final List<Boolean> levelExists;

    public LevelSelectScreen(App app) {
        this.app = app;
        this.levelExists = new ArrayList<>();
        
        // Check which levels exist
        for (int i = 1; i <= MAX_LEVELS; i++) {
            String levelPath = String.format("/levels/level%d.json", i);
            InputStream inputStream = getClass().getResourceAsStream(levelPath);
            levelExists.add(inputStream != null);
        }
    }

    private void loadLevel(int levelNum) {
        try {
            String levelPath = String.format("level%d.json", levelNum);
            InputStream inputStream = getClass().getResourceAsStream("/levels/" + levelPath);
            if (inputStream == null) {
                System.err.println("Level " + levelNum + " not found");
                return;
            }
            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            LevelConfig levelConfig = new Gson().fromJson(jsonContent, LevelConfig.class);
            app.setCurrentScreen(new GameScreen(app, levelConfig));
        } catch (Exception e) {
            System.err.println("Error loading level " + levelNum + ": " + e.getMessage());
        }
    }

    @Override
    public void render() {
        float windowWidth = ImGui.getIO().getDisplaySizeX();
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        
        // Create fullscreen window
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.begin("Level Select", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | 
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar);
        
        // Title
        float titleWidth = ImGui.calcTextSize("Select Level").x;
        ImGui.setCursorPosX((windowWidth - titleWidth) / 2);
        ImGui.setCursorPosY(20);
        ImGui.textColored(0.9f, 0.9f, 1.0f, 1.0f, "Select Level");
        
        // Calculate grid area size and position
        float gridWidth = GRID_COLS * (BUTTON_SIZE + BUTTON_SPACING) - BUTTON_SPACING;
        float gridHeight = ((MAX_LEVELS + GRID_COLS - 1) / GRID_COLS) * (BUTTON_SIZE + BUTTON_SPACING) - BUTTON_SPACING;
        float gridStartX = (windowWidth - gridWidth) / 2;
        float gridStartY = 100;  // Add some space below title
        
        // Create grid of level buttons
        int levelNum = 1;
        for (int row = 0; row < (MAX_LEVELS + GRID_COLS - 1) / GRID_COLS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                if (levelNum > MAX_LEVELS) break;
                
                // Calculate button position
                float x = gridStartX + col * (BUTTON_SIZE + BUTTON_SPACING);
                float y = gridStartY + row * (BUTTON_SIZE + BUTTON_SPACING);
                
                // Set cursor position for this button
                ImGui.setCursorPos(x, y);
                
                // Check if level exists
                boolean exists = levelNum <= levelExists.size() && levelExists.get(levelNum - 1);
                
                // Style the button based on existence
                if (!exists) {
                    ImGui.pushStyleColor(ImGuiCol.Button, ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1.0f));
                    ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImGui.colorConvertFloat4ToU32(0.35f, 0.35f, 0.35f, 1.0f));
                    ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 1.0f));
                } else {
                    ImGui.pushStyleColor(ImGuiCol.Button, ImGui.colorConvertFloat4ToU32(0.2f, 0.4f, 0.6f, 1.0f));
                    ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImGui.colorConvertFloat4ToU32(0.3f, 0.5f, 0.7f, 1.0f));
                    ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImGui.colorConvertFloat4ToU32(0.25f, 0.45f, 0.65f, 1.0f));
                }
                
                // Create the button
                if (ImGui.button(String.valueOf(levelNum), BUTTON_SIZE, BUTTON_SIZE)) {
                    if (exists) {
                        loadLevel(levelNum);
                    }
                }
                
                // Add level number to button
                float textWidth = ImGui.calcTextSize(String.valueOf(levelNum)).x;
                float textX = x + (BUTTON_SIZE - textWidth) / 2;
                float textY = y + (BUTTON_SIZE - ImGui.getTextLineHeight()) / 2;
                
                ImGui.popStyleColor(3);
                
                levelNum++;
            }
        }
        
        // Online Levels button (bottom left)
        ImGui.setCursorPos(20, windowHeight - 60);
        
        // Style based on login status
        if (!app.isLoggedIn()) {
            ImGui.pushStyleColor(ImGuiCol.Button, ImGui.colorConvertFloat4ToU32(0.5f, 0.5f, 0.5f, 1.0f));
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, ImGui.colorConvertFloat4ToU32(0.55f, 0.55f, 0.55f, 1.0f));
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, ImGui.colorConvertFloat4ToU32(0.5f, 0.5f, 0.5f, 1.0f));
            ImGui.button("Online Levels", 150, 40);
            ImGui.popStyleColor(3);
            
            // Add tooltip
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("Login required for online levels");
            }
        } else {
            if (ImGui.button("Online Levels", 150, 40)) {
                app.setCurrentScreen(new OnlineLevelSelectScreen(app));
            }
        }
        
        // Back button (bottom right)
        ImGui.setCursorPos(windowWidth - 170, windowHeight - 60);
        if (ImGui.button("Back", 150, 40)) {
            app.setCurrentScreen(new TitleScreen(app));
        }
        
        ImGui.end();
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
        // ImGui handles mouse release automatically
    }

    @Override
    public void handleKeyPress(int key, int action) {
        // ImGui handles keyboard input automatically
    }
}