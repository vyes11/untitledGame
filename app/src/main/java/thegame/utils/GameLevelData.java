package thegame.utils;

import com.google.gson.annotations.SerializedName;

public class GameLevelData {
    private String name;
    private ColorCell[][] grid;
    private ColorCell[][] targetPattern;

    public static class ColorCell {
        public float red;
        public float green;
        public float blue;
        @SerializedName("isCenter")
        public boolean isCenter;

        public ColorCell(float red, float green, float blue, boolean isCenter) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.isCenter = isCenter;
        }
    }

    // Getters
    public String getName() { return name; }
    public ColorCell[][] getGrid() { return grid; }
    public ColorCell[][] getTargetPattern() { return targetPattern; }
}