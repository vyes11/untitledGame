package thegame.utils;

import com.google.gson.annotations.SerializedName;

public class LevelConfig {
    private String name;
    private Cell[][] grid;
    private Cell[][] targetPattern;
    private Settings settings;  // Add settings field

    public static class Settings {
        @SerializedName("gridSize")
        private int gridSize = 4;  // Default to 4x4 grid

        @SerializedName("maxMoves")
        private int maxMoves = 10;  // Default to 10 moves

        @SerializedName("difficulty")
        private String difficulty = "easy";  // Default difficulty

        public Settings() {} // Default constructor for GSON

        public Settings(int gridSize, int maxMoves, String difficulty) {
            this.gridSize = gridSize;
            this.maxMoves = maxMoves;
            this.difficulty = difficulty;
        }

        // Getters
        public int getGridSize() { return gridSize; }
        public int getMaxMoves() { return maxMoves; }
        public String getDifficulty() { return difficulty; }
    }

    public static class Cell {
        // Values should be between 0.0f and 1.0f for OpenGL colors
        @SerializedName("red")
        public float red = 1.0f;  // Default to white if not specified

        @SerializedName("green")
        public float green = 1.0f;

        @SerializedName("blue")
        public float blue = 1.0f;

        @SerializedName("isCenter")
        public boolean isCenter;

        public Cell() {} // Default constructor for GSON

        public Cell(float red, float green, float blue, boolean isCenter) {
            // Clamp values between 0 and 1
            this.red = Math.max(0.0f, Math.min(1.0f, red));
            this.green = Math.max(0.0f, Math.min(1.0f, green));
            this.blue = Math.max(0.0f, Math.min(1.0f, blue));
            this.isCenter = isCenter;
        }
    }

    // Getters
    public String getName() { return name; }
    public Cell[][] getGrid() { return grid; }
    public Cell[][] getTargetPattern() { return targetPattern; }

    // Add getter for settings
    public Settings getSettings() { 
        return settings != null ? settings : new Settings(); // Return default settings if null
    }

    // Add debug method to verify color values
    public void debugPrintGrid() {
        if (grid == null) {
            System.out.println("Grid is null!");
            return;
        }
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                Cell cell = grid[i][j];
                System.out.printf("Cell[%d][%d]: RGB(%.2f, %.2f, %.2f) Center: %b%n",
                    i, j, cell.red, cell.green, cell.blue, cell.isCenter);
            }
        }
    }
}