package thegame.screens;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import thegame.App;
import thegame.Screen;
import thegame.utils.ImGuiUtils;

public class TitleScreen implements Screen {
    private final App app;

    public TitleScreen(App app) {
        this.app = app;
        System.out.println("TitleScreen initialized");
    }

    @Override
    public void render() {
        // Create a fullscreen window with no background
        ImGuiUtils.beginFullScreenWindow("Title Screen");
        
        // Calculate positions for title and buttons
        float windowHeight = ImGui.getWindowHeight();
        float windowWidth = ImGui.getWindowWidth();
        
        // Set cursor position for title text
        ImGui.setCursorPos(windowWidth * 0.4f, windowHeight * 0.2f);
        
        // Display large title text
        ImGui.pushFont(ImGui.getFont()); // Use default font - you can create larger fonts
        ImGui.textColored(0.9f, 0.9f, 1.0f, 1.0f, "The Game");
        ImGui.popFont();
        
        // Button dimensions
        float buttonWidth = 200;
        float buttonHeight = 50;
        
        // Space between buttons
        ImGui.setCursorPos(windowWidth * 0.4f, windowHeight * 0.4f);
        
        // Play Game button
        if (ImGuiUtils.coloredButton("Play Game", buttonWidth, buttonHeight, 
                                  0.3f, 0.3f, 0.8f, 0.4f, 0.4f, 0.9f)) {
            app.setCurrentScreen(new LevelSelectScreen(app));
        }
        
        // Login button
        ImGui.setCursorPos(windowWidth * 0.4f, windowHeight * 0.5f);
        if (ImGuiUtils.coloredButton("Login", buttonWidth, buttonHeight,
                                  0.3f, 0.3f, 0.8f, 0.4f, 0.4f, 0.9f)) {
            app.setCurrentScreen(new LoginScreen(app));
        }

        // End the fullscreen window
        ImGui.end();
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        // ImGui handles input automatically
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // ImGui handles input automatically
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        // ImGui handles input automatically
    }

    @Override
    public void handleKeyPress(int key, int action) {
        // ImGui handles input automatically
    }
}