package thegame.utils;

import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import org.bson.Document;

public class LevelConfig {
    private int id;  // This will store the level number
    private String name;
    private Settings settings;
    private Cell[][] grid;
    private Cell[][] targetPattern;

    // Add new fields for upload metadata
    private String creator;
    private long createdAt;
    private boolean isCustomLevel;
    private String description;
    private boolean isNumberMode;

    // Add getter for level number
    public int getLevelNumber() {
        return id;
    }


    public static class Settings {
        @SerializedName("gridSize")
        private int gridSize = 4;  // Default to 4x4 grid

        @SerializedName("maxMoves")
        private int maxMoves = 10;  // Default to 10 moves

        @SerializedName("difficulty")
        private String difficulty = "easy";  // Default difficulty

        private boolean isNumberMode;

        public Settings() {} // Default constructor for GSON

        public Settings(int gridSize, int maxMoves, String difficulty, boolean isNumberMode) {
            this.gridSize = gridSize;
            this.maxMoves = maxMoves;
            this.difficulty = difficulty;
            this.isNumberMode = isNumberMode;
        }

        public Settings(int gridSize, int maxMoves, String difficulty) {
            this.gridSize = gridSize;
            this.maxMoves = maxMoves;
            this.difficulty = difficulty;
        }

        // Getters
        public int getGridSize() { return gridSize; }
        public int getMaxMoves() { return maxMoves; }
        public String getDifficulty() { return difficulty; }
        public boolean isNumberMode() { return isNumberMode; }
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

    // Add builder pattern for easier level creation
    public static class Builder {
        private LevelConfig level;
        private boolean isNumberMode;

        public Builder() {
            level = new LevelConfig();
            level.createdAt = System.currentTimeMillis();
            level.isCustomLevel = true;
        }

        public Builder withId(int id) {
            level.id = id;
            return this;
        }

        public Builder withName(String name) {
            level.name = name;
            return this;
        }

        public Builder withGrid(Cell[][] grid) {
            level.grid = grid;
            return this;
        }

        public Builder withTargetPattern(Cell[][] targetPattern) {
            level.targetPattern = targetPattern;
            return this;
        }

        public Builder withSettings(Settings settings) {
            level.settings = settings;
            return this;
        }

        public Builder withCreator(String creator) {
            level.creator = creator;
            return this;
        }

        public Builder withDescription(String description) {
            level.description = description;
            return this;
        }

        public Builder withNumberMode(boolean isNumberMode) {
            this.isNumberMode = isNumberMode;
            return this;
        }

        public LevelConfig build() {
            validateLevel();
            return level;
        }

        private void validateLevel() {
            if (level.id <= 0) throw new IllegalStateException("Level ID must be positive");
            if (level.name == null) throw new IllegalStateException("Level name is required");
            if (level.grid == null) throw new IllegalStateException("Grid is required");
            if (level.targetPattern == null) throw new IllegalStateException("Target pattern is required");
            if (level.settings == null) throw new IllegalStateException("Settings are required");
        }
    }

    // Add method to prepare level for upload
    public Document toDocument() {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        Document doc = Document.parse(json);
        
        // Add metadata if not present
        if (!doc.containsKey("createdAt")) {
            doc.append("createdAt", System.currentTimeMillis());
        }
        if (!doc.containsKey("creator")) {
            doc.append("creator", "anonymous");
        }
        
        return doc;
    }

    // Add static factory method for creating custom levels
    public static LevelConfig createCustomLevel(int gridSize, int maxMoves) {
        return new Builder()
            .withId(generateCustomLevelId())
            .withName("Custom Level")
            .withGrid(new Cell[gridSize][gridSize])
            .withTargetPattern(new Cell[gridSize][gridSize])
            .withSettings(new Settings(gridSize, maxMoves, "custom"))
            .withCreator("anonymous")
            .withDescription("Custom created level")
            .build();
    }

    private static int generateCustomLevelId() {
        // Start custom levels from a high number to avoid conflicts
        return 10000 + (int)(Math.random() * 90000);
    }

    // Add getters and setters for new fields
    public boolean isCustomLevel() { return isCustomLevel; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}