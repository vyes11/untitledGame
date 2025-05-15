package thegame.onScreenObjects;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.ImVec2;
import thegame.App;
import thegame.screens.GameScreen;
import thegame.screens.LevelSelectScreen;
import thegame.utils.LevelConfig;
import com.google.gson.Gson;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class VictorySplash {
    private final App app;
    private final String currentLevelName;
    private final LevelConfig currentLevel;

    public VictorySplash(App app, String currentLevelName, LevelConfig currentLevel) {
        this.app = app;
        this.currentLevelName = currentLevelName;
        this.currentLevel = currentLevel;
    }

    public void render() {
        float windowWidth = ImGui.getIO().getDisplaySizeX();
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        
        // Create a semi-transparent overlay window
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.setNextWindowBgAlpha(0.7f); // Semi-transparent background
        
        ImGui.begin("##overlay", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | 
                ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar | 
                ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoInputs);
        ImGui.end();
        
        // Create a victory popup window
        float popupWidth = windowWidth * 0.5f;
        float popupHeight = windowHeight * 0.5f;
        float popupX = (windowWidth - popupWidth) * 0.5f;
        float popupY = (windowHeight - popupHeight) * 0.5f;
        
        ImGui.setNextWindowPos(popupX, popupY);
        ImGui.setNextWindowSize(popupWidth, popupHeight);
        
        ImGui.begin("Level Complete!", 
                ImGuiWindowFlags.NoResize | 
                ImGuiWindowFlags.NoMove | 
                ImGuiWindowFlags.NoCollapse |
                ImGuiWindowFlags.NoScrollbar);
        
        // Victory title
        float titleWidth = ImGui.calcTextSize("Level Complete!").x;
        ImGui.setCursorPosX((popupWidth - titleWidth) * 0.5f);
        ImGui.setCursorPosY(popupHeight * 0.2f);
        ImGui.pushFont(ImGui.getFont()); // Ensure default font is used
        ImGui.textColored(0.2f, 0.8f, 0.2f, 1.0f, "Level Complete!");
        ImGui.popFont();
        
        // Level name
        if (currentLevelName != null && !currentLevelName.isEmpty()) {
            float nameWidth = ImGui.calcTextSize(currentLevelName).x;
            ImGui.setCursorPosX((popupWidth - nameWidth) * 0.5f);
            ImGui.setCursorPosY(popupHeight * 0.3f);
            ImGui.text(currentLevelName);
        }
        
        // Buttons
        float buttonWidth = 120;
        float buttonHeight = 40;
        float buttonsY = popupHeight * 0.6f;
        
        // Level Select Button
        ImGui.setCursorPosX(popupWidth * 0.2f - buttonWidth * 0.5f);
        ImGui.setCursorPosY(buttonsY);
        if (ImGui.button("Level Select", buttonWidth, buttonHeight)) {
            app.setCurrentScreen(new LevelSelectScreen(app));
        }
        
        // Restart Button
        ImGui.setCursorPosX(popupWidth * 0.5f - buttonWidth * 0.5f);
        ImGui.setCursorPosY(buttonsY);
        if (ImGui.button("Restart", buttonWidth, buttonHeight)) {
            app.setCurrentScreen(new GameScreen(app, currentLevel));
        }
        
        // Next Level Button
        ImGui.setCursorPosX(popupWidth * 0.8f - buttonWidth * 0.5f);
        ImGui.setCursorPosY(buttonsY);
        if (ImGui.button("Next Level", buttonWidth, buttonHeight)) {
            try {
                int nextLevelNum = currentLevel.getLevelNumber() + 1;
                String nextLevelPath = String.format("/levels/level%d.json", nextLevelNum);
                InputStream inputStream = VictorySplash.class.getResourceAsStream(nextLevelPath);
                if (inputStream == null) {
                    System.err.println("No next level found");
                    return;
                }
                String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                LevelConfig nextLevel = new Gson().fromJson(jsonContent, LevelConfig.class);
                app.setCurrentScreen(new GameScreen(app, nextLevel));
            } catch (Exception e) {
                System.err.println("Error loading next level: " + e.getMessage());
            }
        }
        
        ImGui.end();
    }

    // ImGui handles input automatically, so these methods are no longer needed
    public void handleMouseClick(float mouseX, float mouseY) {
        // Not needed with ImGui
    }

    public void handleMouseMove(float mouseX, float mouseY) {
        // Not needed with ImGui
    }
}