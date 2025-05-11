package thegame.utils;

public class LevelData {
    private LevelConfig.Cell[][] grid;
    private LevelConfig.Cell[][] targetPattern;
    private String name;
    private String id;
    private LevelSettings settings;

    public static class LevelSettings {
        private int gridSize;           // Size of the grid (e.g., 4 for 4x4, 6 for 6x6)
        private int maxMoves;           // Maximum number of moves allowed
        private String difficulty;      // easy, medium, hard
        private String creator;         // Level creator's name
        private String createdAt;       // Creation date
        private String description;     // Level description

        public LevelSettings(int gridSize, int maxMoves, String difficulty) {
            this.gridSize = gridSize;
            this.maxMoves = maxMoves;
            this.difficulty = difficulty;
        }

        // Getters
        public int getGridSize() { return gridSize; }
        public int getMaxMoves() { return maxMoves; }
        public String getDifficulty() { return difficulty; }
        public String getCreator() { return creator; }
        public String getCreatedAt() { return createdAt; }
        public String getDescription() { return description; }

        // Setters for optional fields
        public void setCreator(String creator) { this.creator = creator; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public void setDescription(String description) { this.description = description; }
    }

    public LevelData(LevelConfig.Cell[][] grid, LevelConfig.Cell[][] targetPattern, 
                    String name, String id, LevelSettings settings) {
        this.grid = grid;
        this.targetPattern = targetPattern;
        this.name = name;
        this.id = id;
        this.settings = settings;
    }

    // Existing getters
    public LevelConfig.Cell[][] getGrid() { return grid; }
    public LevelConfig.Cell[][] getTargetPattern() { return targetPattern; }
    public String getName() { return name; }
    public String getId() { return id; }
    
    // New getter for settings
    public LevelSettings getSettings() { return settings; }
}