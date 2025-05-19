package thegame.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Manager for game settings that persists values between game sessions.
 * Handles loading, saving, and providing access to user preferences.
 */
public class SettingsManager {
    private static final String SETTINGS_FILE = "F:/temp/theGame/untitledGame/app/settings.properties";
    private static Properties properties;
    
    // Default settings
    private static float musicVolume = 0.7f;
    private static float effectsVolume = 0.8f;
    private static boolean fullscreen = false;
    private static boolean vsync = true;
    private static int antiAliasing = 4; // Default to 4x MSAA
    
    static {
        properties = new Properties();
        loadSettings();
    }
    
    /**
     * Loads settings from the properties file.
     * If the file doesn't exist, default values are used.
     */
    public static void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                properties.load(in);
                
                // Load values from properties file
                musicVolume = Float.parseFloat(properties.getProperty("musicVolume", "0.7"));
                effectsVolume = Float.parseFloat(properties.getProperty("effectsVolume", "0.8"));
                fullscreen = Boolean.parseBoolean(properties.getProperty("fullscreen", "false"));
                vsync = Boolean.parseBoolean(properties.getProperty("vsync", "true"));
                antiAliasing = Integer.parseInt(properties.getProperty("antiAliasing", "4"));
            } catch (IOException e) {
                System.err.println("Error loading settings: " + e.getMessage());
            }
        }
    }
    
    /**
     * Saves the current settings to the properties file.
     */
    public static void saveSettings() {
        properties.setProperty("musicVolume", String.valueOf(musicVolume));
        properties.setProperty("effectsVolume", String.valueOf(effectsVolume));
        properties.setProperty("fullscreen", String.valueOf(fullscreen));
        properties.setProperty("vsync", String.valueOf(vsync));
        properties.setProperty("antiAliasing", String.valueOf(antiAliasing));
        
        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(out, "Game Settings");
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }
    
    /**
     * Gets the music volume.
     * @return The music volume (0.0-1.0)
     */
    public static float getMusicVolume() { return musicVolume; }
    
    /**
     * Sets the music volume.
     * @param volume The music volume (0.0-1.0)
     */
    public static void setMusicVolume(float volume) { musicVolume = volume; }
    
    /**
     * Gets the effects volume.
     * @return The effects volume (0.0-1.0)
     */
    public static float getEffectsVolume() { return effectsVolume; }
    
    /**
     * Sets the effects volume.
     * @param volume The effects volume (0.0-1.0)
     */
    public static void setEffectsVolume(float volume) { effectsVolume = volume; }
    
    /**
     * Checks if fullscreen mode is enabled.
     * @return true if fullscreen is enabled, false otherwise
     */
    public static boolean isFullscreen() { return fullscreen; }
    
    /**
     * Sets the fullscreen mode.
     * @param fs true to enable fullscreen, false for windowed mode
     */
    public static void setFullscreen(boolean fs) { fullscreen = fs; }
    
    /**
     * Checks if vertical synchronization is enabled.
     * @return true if vsync is enabled, false otherwise
     */
    public static boolean isVsync() { return vsync; }
    
    /**
     * Sets the vertical synchronization.
     * @param vs true to enable vsync, false to disable
     */
    public static void setVsync(boolean vs) { vsync = vs; }
    
    /**
     * Gets the anti-aliasing level.
     * @return The anti-aliasing level (0=Off, 2=2x, 4=4x, 8=8x)
     */
    public static int getAntiAliasing() { return antiAliasing; }
    
    /**
     * Sets the anti-aliasing level.
     * @param aa The anti-aliasing level (0=Off, 2=2x, 4=4x, 8=8x)
     */
    public static void setAntiAliasing(int aa) { antiAliasing = aa; }
}
