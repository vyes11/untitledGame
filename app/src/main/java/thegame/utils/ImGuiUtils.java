package thegame.utils;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;

/**
 * Utility class for ImGui operations to maintain consistent UI
 */
public class ImGuiUtils {

    public static void beginFullScreenWindow(String name) {
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY());
        ImGui.begin(name, ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | 
                   ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar | 
                   ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoBackground);
    }
    
    public static boolean centeredButton(String label, float width, float height) {
        float windowWidth = ImGui.getWindowWidth();
        float windowX = ImGui.getCursorPosX();
        
        // Center horizontally
        ImGui.setCursorPosX(windowX + (windowWidth - width) * 0.5f);
        
        return ImGui.button(label, width, height);
    }
    
    public static boolean coloredButton(String label, float width, float height, 
                                     float r, float g, float b, float hoverR, float hoverG, float hoverB) {
        ImVec2 pos = ImGui.getCursorScreenPos();
        boolean clicked = ImGui.button("##" + label, width, height);
        
        // Draw colored rectangle inside button
        ImGui.getWindowDrawList().addRectFilled(
            pos.x, pos.y, 
            pos.x + width, pos.y + height, 
            ImGui.isItemHovered() ? 
                ImGui.colorConvertFloat4ToU32(hoverR, hoverG, hoverB, 1.0f) : 
                ImGui.colorConvertFloat4ToU32(r, g, b, 1.0f)
        );
        
        // Center text over button
        float textWidth = ImGui.calcTextSize(label).x;
        float textX = pos.x + (width - textWidth) * 0.5f;
        float textY = pos.y + (height - ImGui.getTextLineHeight()) * 0.5f;
        
        ImGui.getWindowDrawList().addText(textX, textY, ImGui.colorConvertFloat4ToU32(1,1,1,1), label);
        
        return clicked;
    }
    
    public static void drawColoredGrid(float[][] grid, float startX, float startY, float cellSize) {
        ImVec2 pos = ImGui.getCursorScreenPos();
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                float x = pos.x + startX + j * cellSize;
                float y = pos.y + startY + i * cellSize;
                
                // Draw colored cell
                ImGui.getWindowDrawList().addRectFilled(
                    x, y, 
                    x + cellSize, y + cellSize, 
                    ImGui.colorConvertFloat4ToU32(grid[i][j], 0, 0, 1.0f)
                );
                
                // Draw cell border
                ImGui.getWindowDrawList().addRect(
                    x, y, 
                    x + cellSize, y + cellSize, 
                    ImGui.colorConvertFloat4ToU32(0.5f, 0.5f, 0.5f, 1.0f)
                );
            }
        }
    }
}