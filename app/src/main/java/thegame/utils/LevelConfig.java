package thegame.utils;

import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bson.Document;
import java.util.HashMap;
import java.util.Map;

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

    // Define move types enum
    public enum MoveType {
        SWAP,        // Swapping two adjacent cells
        ROTATE,      // Rotating a section
        FLIP_ROW,    // Flipping a row
        FLIP_COLUMN, // Flipping a column
        MULTIPLY,    // Multiply cell value (number mode)
        DIVIDE,      // Divide cell value (number mode)
        ADD,         // Add to cell value (number mode)
        SUBTRACT,    // Subtract from cell value (number mode)
        EDIT_ROW,    // Edit an entire row at once
        EDIT_COLUMN  // Edit an entire column at once
    }

    // Add getter for level number
    public int getLevelNumber() {
        return id;
    }

    public static class Settings {
        @SerializedName("gridSize")
        private int gridSize = 4;  // Default to 4x4 grid

        @SerializedName("maxMoves")
        private int maxMoves = 10;  // Kept for backward compatibility
        
        @SerializedName("moveLimits")
        private Map<String, Integer> moveLimits = new HashMap<>();  // Limits per move type
        
        @SerializedName("difficulty")
        private String difficulty = "easy";  // Default difficulty

        @SerializedName("isNumberMode")
        private boolean isNumberMode;
        
        @SerializedName("maxRowEdits")
        private int maxRowEdits = 3;  // Default max row edits
        
        @SerializedName("maxColEdits")
        private int maxColEdits = 3;  // Default max column edits

        public Settings() {
            // Initialize default move limits
            for (MoveType moveType : MoveType.values()) {
                moveLimits.put(moveType.name(), 10); // Default 10 moves per type
            }
        }

        public Settings(int gridSize, int maxMoves, String difficulty, boolean isNumberMode) {
            this.gridSize = gridSize;
            this.maxMoves = maxMoves;
            this.difficulty = difficulty;
            this.isNumberMode = isNumberMode;
            
            // Initialize move limits with the same value
            for (MoveType moveType : MoveType.values()) {
                moveLimits.put(moveType.name(), maxMoves);
            }
        }

        public Settings(int gridSize, int maxMoves, String difficulty) {
            this(gridSize, maxMoves, difficulty, false);
        }
        
        // New constructor with move type limits
        public Settings(int gridSize, Map<String, Integer> moveLimits, String difficulty, boolean isNumberMode) {
            this.gridSize = gridSize;
            this.moveLimits = moveLimits;
            this.difficulty = difficulty;
            this.isNumberMode = isNumberMode;
            
            // Set maxMoves to the highest value for backward compatibility
            this.maxMoves = moveLimits.values().stream().mapToInt(Integer::intValue).max().orElse(10);
        }

        // Getters
        public int getGridSize() { return gridSize; }
        
        // For backward compatibility
        public int getMaxMoves() { return maxMoves; }
        
        // New method to get move limit for a specific type
        public int getMoveLimit(MoveType moveType) {
            return moveLimits.getOrDefault(moveType.name(), maxMoves);
        }
        
        // New method to get all move limitsevel
        public Map<String, Integer> getMoveLimits() {
            return moveLimits;
        }
        
        // New method to set a move limit
        public void setMoveLimit(MoveType moveType, int limit) {
            moveLimits.put(moveType.name(), limit);
        }
        
        public String getDifficulty() { return difficulty; }
        public boolean isNumberMode() { return isNumberMode; }
        
        // Add getters for new fields
        public int getMaxRowEdits() { return maxRowEdits; }
        public int getMaxColEdits() { return maxColEdits; }
        
        // Add setters for new fields
        public void setMaxRowEdits(int maxRowEdits) { this.maxRowEdits = maxRowEdits; }
        public void setMaxColEdits(int maxColEdits) { this.maxColEdits = maxColEdits; }
    }

    public static class Cell {
        // Values should be between 0.0f and 1.0f for OpenGL colors
        @SerializedName("red")
        public float red = 1.0f;  // Default to white if not specified

        @SerializedName("green")
        public float green = 1.0f;

        @SerializedName("blue")
        public float blue = 1.0f;

        @SerializedName(value = "isCenter", alternate = "editable")
        public boolean editable;
        
        @SerializedName("value")
        private Integer numericValue;

        public Cell() {} // Default constructor for GSON

        public Cell(float red, float green, float blue, boolean editable) {
            // Clamp values between 0 and 1
            this.red = Math.max(0.0f, Math.min(1.0f, red));
            this.green = Math.max(0.0f, Math.min(1.0f, green));
            this.blue = Math.max(0.0f, Math.min(1.0f, blue));
            this.editable = editable;
        }
        
        // Constructor for number mode
        public Cell(int value, boolean editable) {
            this.numericValue = value;
            // Also set RGB values for backward compatibility (value/9.0f)
            float normalized = Math.min(1.0f, Math.max(0.0f, value / 9.0f));
            this.red = normalized;
            this.green = normalized;
            this.blue = normalized;
            this.editable = editable;
        }
        
        // Get the numeric value, either directly or from red channel
        public int getNumericValue() {
            if (numericValue != null) {
                return numericValue;
            } else {
                // Derive from red channel (legacy mode)
                return (int)(red * 9);
            }
        }
    }

    // Custom deserializer for 2D Cell arrays from LevelData
    private static class CellArrayDeserializer implements JsonDeserializer<Cell[][]> {
        @Override
        public Cell[][] deserialize(JsonElement json, Type typeOfT, 
                                  JsonDeserializationContext context) throws JsonParseException {
            JsonElement[] rows = json.getAsJsonArray().asList().toArray(new JsonElement[0]);
            Cell[][] result = new Cell[rows.length][];
            
            for (int i = 0; i < rows.length; i++) {
                JsonElement[] cells = rows[i].getAsJsonArray().asList().toArray(new JsonElement[0]);
                result[i] = new Cell[cells.length];
                
                for (int j = 0; j < cells.length; j++) {
                    JsonElement cell = cells[j];
                    boolean isEditable = false;
                    
                    // Look for either "isCenter" or "editable" property
                    if (cell.getAsJsonObject().has("isCenter")) {
                        isEditable = cell.getAsJsonObject().get("isCenter").getAsBoolean();
                    } else if (cell.getAsJsonObject().has("editable")) {
                        isEditable = cell.getAsJsonObject().get("editable").getAsBoolean();
                    }
                    
                    // Check if we have a value property (new number mode)
                    if (cell.getAsJsonObject().has("value")) {
                        int value = cell.getAsJsonObject().get("value").getAsInt();
                        result[i][j] = new Cell(value, isEditable);
                    } else {
                        // Use RGB values (legacy mode)
                        result[i][j] = new Cell(
                            cell.getAsJsonObject().get("red").getAsFloat(),
                            cell.getAsJsonObject().get("green").getAsFloat(),
                            cell.getAsJsonObject().get("blue").getAsFloat(),
                            isEditable
                        );
                    }
                }
            }
            return result;
        }
    }

    // Load level from file (from LevelData)
    public static LevelConfig fromJsonFile(String filePath) throws Exception {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Cell[][].class, new CellArrayDeserializer())
            .create();

        try (Reader reader = Files.newBufferedReader(Path.of(filePath))) {
            return gson.fromJson(reader, LevelConfig.class);
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
                System.out.printf("Cell[%d][%d]: RGB(%.2f, %.2f, %.2f) Editable: %b%n",
                    i, j, cell.red, cell.green, cell.blue, cell.editable);
            }
        }
    }

    // Add builder pattern for easier level creation
    public static class Builder {
        private LevelConfig level;
        private boolean isNumberMode;
        private Map<String, Integer> moveLimits = new HashMap<>();
        private Cell[][] grid; // Add a field to store the grid
        
        private int maxRowEdits = 3;
        private int maxColEdits = 3;

        public Builder() {
            level = new LevelConfig();
            level.createdAt = System.currentTimeMillis();
            level.isCustomLevel = true;
            
            // Initialize default move limits
            for (MoveType moveType : MoveType.values()) {
                moveLimits.put(moveType.name(), 10);
            }
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
            this.grid = grid; // Store reference in the builder too
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
        
        // New method to set a specific move limit
        public Builder withMoveLimit(MoveType moveType, int limit) {
            moveLimits.put(moveType.name(), limit);
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
        
        public Builder withMaxRowEdits(int maxRowEdits) {
            this.maxRowEdits = maxRowEdits;
            return this;
        }
        
        public Builder withMaxColEdits(int maxColEdits) {
            this.maxColEdits = maxColEdits;
            return this;
        }

        // Add a getter for grid
        public Cell[][] getGrid() {
            return this.grid;
        }

        public LevelConfig build() {
            // Create settings if not already set
            if (level.settings == null) {
                // Use default grid size (4) if not set elsewhere
                int gridSize = level.grid != null ? level.grid.length : 4;
                level.settings = new Settings(gridSize, moveLimits, "custom", isNumberMode);
            } else {
                // Update existing settings with move limits
                for (Map.Entry<String, Integer> entry : moveLimits.entrySet()) {
                    try {
                        MoveType moveType = MoveType.valueOf(entry.getKey());
                        level.settings.setMoveLimit(moveType, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        // Ignore invalid move types
                    }
                }
            }
            
            // Update settings with row/column edit limits
            if (level.settings != null) {
                level.settings.setMaxRowEdits(maxRowEdits);
                level.settings.setMaxColEdits(maxColEdits);
            }
            
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
        Builder builder = new Builder()
            .withId(generateCustomLevelId())
            .withName("Custom Level")
            .withGrid(new Cell[gridSize][gridSize])
            .withTargetPattern(new Cell[gridSize][gridSize])
            .withCreator("anonymous")
            .withDescription("Custom created level");
            
        // Set the same limit for all move types
        for (MoveType moveType : MoveType.values()) {
            builder.withMoveLimit(moveType, maxMoves);
        }
        
        return builder.build();
    }

    // New method to create custom level with specific move limits
    public static LevelConfig createCustomLevel(int gridSize, Map<String, Integer> moveLimits) {
        Builder builder = new Builder()
            .withId(generateCustomLevelId())
            .withName("Custom Level")
            .withGrid(new Cell[gridSize][gridSize])
            .withTargetPattern(new Cell[gridSize][gridSize])
            .withCreator("anonymous")
            .withDescription("Custom created level");
            
        // Set specific limits for each move type
        for (Map.Entry<String, Integer> entry : moveLimits.entrySet()) {
            try {
                MoveType moveType = MoveType.valueOf(entry.getKey());
                builder.withMoveLimit(moveType, entry.getValue());
            } catch (IllegalArgumentException e) {
                // Ignore invalid move types
            }
        }
        
        return builder.build();
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