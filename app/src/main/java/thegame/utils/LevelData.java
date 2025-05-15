package thegame.utils;

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

public class LevelData {
    private Cell[][] grid;
    private Cell[][] targetPattern;
    private String name;
    private String id;
    private LevelSettings settings;

    public static class Cell {
        public float red;
        public float green;
        public float blue;
        public boolean isCenter;

        public Cell(float red, float green, float blue, boolean isCenter) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.isCenter = isCenter;
        }
    }

    public static class LevelSettings {
        private int gridSize;
        private int maxMoves;
        private String difficulty;
        private String creator;
        private String createdAt;
        private String description;

        // Getters
        public int getGridSize() { return gridSize; }
        public int getMaxMoves() { return maxMoves; }
        public String getDifficulty() { return difficulty; }
        public String getCreator() { return creator; }
        public String getCreatedAt() { return createdAt; }
        public String getDescription() { return description; }
    }

    // Custom deserializer for 2D Cell arrays
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
                    result[i][j] = new Cell(
                        cell.getAsJsonObject().get("red").getAsFloat(),
                        cell.getAsJsonObject().get("green").getAsFloat(),
                        cell.getAsJsonObject().get("blue").getAsFloat(),
                        cell.getAsJsonObject().get("isCenter").getAsBoolean()
                    );
                }
            }
            return result;
        }
    }

    public static LevelData fromJsonFile(String filePath) throws Exception {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Cell[][].class, new CellArrayDeserializer())
            .create();

        try (Reader reader = Files.newBufferedReader(Path.of(filePath))) {
            return gson.fromJson(reader, LevelData.class);
        }
    }

    // Example usage
    public static void main(String[] args) {
        try {
            LevelData level = LevelData.fromJsonFile("level.json");
            System.out.println("Loaded level: " + level.name);
            System.out.println("Grid size: " + level.settings.gridSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters
    public Cell[][] getGrid() { return grid; }
    public Cell[][] getTargetPattern() { return targetPattern; }
    public String getName() { return name; }
    public String getId() { return id; }
    public LevelSettings getSettings() { return settings; }
}