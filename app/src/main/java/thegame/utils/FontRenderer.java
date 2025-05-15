package thegame.utils;

import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FontRenderer {
    private ImFont defaultFont;
    private ImFont largeFont;
    private boolean fontsLoaded = false;
    private float currentScale = 1.0f;  // Track scale ourselves
    
    public void loadImGuiFont() {
        if (fontsLoaded) return;
        
        try {
            // Get the default font
            defaultFont = ImGui.getFont();
            
            // You could add custom fonts here if needed
            // For example:
            // largeFont = ImGui.getIO().getFonts().addFontFromFileTTF("path/to/font.ttf", 24.0f);
            
            // Build the font atlas - THIS IS IMPORTANT
            ImGui.getIO().getFonts().build();
            
            fontsLoaded = true;
            System.out.println("Fonts loaded successfully");
        } catch (Exception e) {
            System.err.println("Error loading fonts: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void renderText(String text, float x, float y, float scale) {
        // No need to save original scale - we'll track our own
        
        // Set the scale for this text
        ImGui.setNextWindowPos(x, y, ImGuiCond.Always);
        ImGui.setNextWindowBgAlpha(0.0f); // Transparent background
        
        // Begin a transparent window for the text
        int windowFlags = ImGuiWindowFlags.NoTitleBar | 
                          ImGuiWindowFlags.NoResize | 
                          ImGuiWindowFlags.NoMove |
                          ImGuiWindowFlags.NoScrollbar |
                          ImGuiWindowFlags.NoSavedSettings |
                          ImGuiWindowFlags.NoInputs |
                          ImGuiWindowFlags.NoBackground;
                          
        ImGui.begin("##text" + text.hashCode(), windowFlags);
        
        // Apply text scale
        ImGui.setWindowFontScale(scale);
        
        // Draw the text
        ImGui.text(text);
        
        // End this text window
        ImGui.end();
        
        // No need to restore original scale with this approach
    }
    
    public void renderCenteredText(String text, float centerX, float centerY, float scale) {
        // Calculate text dimensions
        float textWidth = getTextWidth(text, scale);
        float textHeight = getTextHeight(scale);
        
        // Render at centered position
        renderText(text, centerX - textWidth / 2, centerY - textHeight / 2, scale);
    }
    
    public float getTextWidth(String text, float scale) {
        return ImGui.calcTextSize(text).x * scale;
    }
    
    public float getTextHeight(float scale) {
        return ImGui.getTextLineHeight() * scale;
    }
    
    public void cleanup() {
        // ImGui handles font cleanup automatically
        fontsLoaded = false;
    }
}