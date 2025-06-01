package thegame.screens;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;

import thegame.App;
import thegame.Screen;
import thegame.onScreenObjects.Button;
import thegame.onScreenObjects.Slider;
import thegame.utils.FontRenderer;
import thegame.utils.SettingsManager;

/**
 * Screen for adjusting game settings.
 * Allows modification of audio volumes, visual settings, and other preferences.
 */
public class SettingsScreen implements Screen {
    private final App app;
    private FontRenderer fontRenderer;
    
    // Settings values - now load from SettingsManager
    private float musicVolume;
    private float effectsVolume;
    private boolean vsync;
    private int antiAliasing;
    
    // UI elements
    private Slider musicSlider;
    private Slider effectsSlider;
    private Button vsyncToggle;
    private Button aaToggle;  // New anti-aliasing button
    private Button applyButton;
    private Button backButton;
    
    private double currentMouseX = 0, currentMouseY = 0;
    private int leftMargin = 240;  // Left alignment for labels and controls

    /**
     * Creates a new settings screen.
     * 
     * @param app The main application instance
     */
    public SettingsScreen(App app) {
        this.app = app;
        
        // Load settings from the settings manager
        musicVolume = SettingsManager.getMusicVolume();
        effectsVolume = SettingsManager.getEffectsVolume();
        vsync = SettingsManager.isVsync();
        antiAliasing = SettingsManager.getAntiAliasing();
        
        // Initialize font renderer
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("/fonts/pf_tempesta_seven_bold.ttf");
        
        // Create sliders with pink theme
        int sliderWidth = 250;
        int sliderHeight = 30;
        
        // Adjust slider position to make them more visible
        // Ensure proper spacing between label and slider
        musicSlider = new Slider(leftMargin + 200, 90, sliderWidth, sliderHeight, musicVolume);
        musicSlider.setColors(0.9f, 0.5f, 0.8f); // Secondary pink
        effectsSlider = new Slider(leftMargin + 200, 160, sliderWidth, sliderHeight, effectsVolume);
        effectsSlider.setColors(0.9f, 0.5f, 0.8f); // Secondary pink
        
        // Create toggle buttons with pink theme
        int toggleWidth = 175;
        int toggleHeight = 40;
        int toggleSpacing = 30;
        int centerX = App.WINDOW_WIDTH / 2;
        
        // Create V-Sync toggle with pink theme
        vsyncToggle = new Button(centerX - toggleWidth/2, 240, toggleWidth, toggleHeight,
                                vsync ? 0.8f : 0.7f, vsync ? 0.4f : 0.3f, vsync ? 0.7f : 0.6f,
                                vsync ? "V-Sync: ON" : "V-Sync: OFF");
        
        // Add anti-aliasing toggle button with pink theme
        String aaText = antiAliasing > 0 ? "Anti-Aliasing: " + antiAliasing + "x" : "Anti-Aliasing: OFF";
        aaToggle = new Button(centerX - toggleWidth/2, 300, toggleWidth, toggleHeight,
                           antiAliasing > 0 ? 0.8f : 0.7f, antiAliasing > 0 ? 0.4f : 0.3f, antiAliasing > 0 ? 0.7f : 0.6f, aaText);
        
        // Create action buttons with pink theme
        int buttonWidth = 150;
        int buttonHeight = 40;
        int buttonSpacing = 20; // Space between buttons
        
        applyButton = new Button(centerX - buttonWidth - buttonSpacing/2, 360, buttonWidth, buttonHeight, 
                                0.9f, 0.5f, 0.8f, "Apply"); // Secondary pink
        backButton = new Button(centerX + buttonSpacing/2, 360, buttonWidth, buttonHeight, 
                              0.7f, 0.3f, 0.6f, "Back"); // Dark pink
    }

    /**
     * Renders the settings screen with all sliders, toggles, and buttons.
     */
    @Override
    public void render() {
        // Set up 2D projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, App.WINDOW_WIDTH, App.WINDOW_HEIGHT, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        
        glClearColor(1.0f, 0.7f, 0.9f, 1.0f); // Primary pink
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Get scaling factors for fullscreen mode
        float scaleX = app.getWidthScale();
        float scaleY = app.getHeightScale();
        
        // Ensure proper OpenGL state for drawing
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glLineWidth(2.0f);
        
        // Scale all rendering coordinates
        float titleX = app.denormalizeX(0.5f) - 50 * scaleX;
        float titleY = 50 * scaleY;
        fontRenderer.renderCenteredText("SETTINGS", titleX, titleY, 2.0f * scaleY);
        
        // Draw slider labels with pink text color
        float labelX = leftMargin * scaleX;
        fontRenderer.renderText("Music Volume:", labelX, 
                              musicSlider.getY() * scaleY + 8 * scaleY, 1.0f * scaleY,
                              0.8f, 0.2f, 0.5f, 1.0f); // Pink text
        fontRenderer.renderText("Effects Volume:", labelX, 
                              effectsSlider.getY() * scaleY + 8 * scaleY, 1.0f * scaleY,
                              0.8f, 0.2f, 0.5f, 1.0f); // Pink text
        
        // Draw UI elements with scaling
        // Use the scaled mouse coordinates
        float scaledMouseX = (float)currentMouseX;
        float scaledMouseY = (float)currentMouseY;
        
        // Render UI with scaled coordinates
        musicSlider.render(scaledMouseX, scaledMouseY, scaleX, scaleY);
        effectsSlider.render(scaledMouseX, scaledMouseY, scaleX, scaleY);
        
        // Display current values after rendering sliders
        fontRenderer.renderText(String.format("%.0f%%", musicVolume * 100), 
                             (musicSlider.getX() + musicSlider.getWidth() + 20) * scaleX, 
                             musicSlider.getY() * scaleY + 8 * scaleY, 1.0f * scaleY);
        fontRenderer.renderText(String.format("%.0f%%", effectsVolume * 100), 
                             (effectsSlider.getX() + effectsSlider.getWidth() + 20) * scaleX, 
                             effectsSlider.getY() * scaleY + 8 * scaleY, 1.0f * scaleY);
        
        // Render buttons with scaling
        vsyncToggle.render(scaledMouseX, scaledMouseY, scaleX, scaleY);
        aaToggle.render(scaledMouseX, scaledMouseY, scaleX, scaleY);
        applyButton.render(scaledMouseX, scaledMouseY, scaleX, scaleY);
        backButton.render(scaledMouseX, scaledMouseY, scaleX, scaleY);
    }

    /**
     * Handles mouse click events on settings controls.
     * 
     * @param mouseX X coordinate of the mouse click
     * @param mouseY Y coordinate of the mouse click
     */
    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
        // Get scaling factors for fullscreen mode
        float scaleX = app.getWidthScale();
        float scaleY = app.getHeightScale();
        
        if (backButton.handleMouseClick(mx, my, scaleX, scaleY)) {
            app.setCurrentScreen(new TitleScreen(app));
            return;
        }
        
        if (applyButton.handleMouseClick(mx, my, scaleX, scaleY)) {
            // Save settings to the settings manager
            SettingsManager.setMusicVolume(musicVolume);
            SettingsManager.setEffectsVolume(effectsVolume);
            SettingsManager.setVsync(vsync);
            SettingsManager.setAntiAliasing(antiAliasing);
            SettingsManager.saveSettings();
            
            // Apply the settings
            applySettings();
            
            app.setCurrentScreen(new TitleScreen(app));
            return;
        }
        
        if (vsyncToggle.handleMouseClick(mx, my, scaleX, scaleY)) {
            vsync = !vsync;
            vsyncToggle.setCaption(vsync ? "V-Sync: ON" : "V-Sync: OFF");
            vsyncToggle.setColors(vsync ? 0.2f : 0.6f, 0.4f, 0.4f);
            return;
        }
        
        if (aaToggle.handleMouseClick(mx, my, scaleX, scaleY)) {
            // Cycle through common AA options: OFF, 2x, 4x, 8x
            antiAliasing = (antiAliasing == 0) ? 2 : (antiAliasing == 2) ? 4 : 
                         (antiAliasing == 4) ? 8 : 0;
                         
            String aaText = antiAliasing > 0 ? "Anti-Aliasing: " + antiAliasing + "x" : "Anti-Aliasing: OFF";
            aaToggle.setCaption(aaText);
            aaToggle.setColors(antiAliasing > 0 ? 0.2f : 0.6f, 0.4f, 0.4f);
            return;
        }
        
        // Handle slider interactions
        if (musicSlider.handleMouseClick(mx, my, scaleX, scaleY)) {
            musicVolume = musicSlider.getValue();
            return;
        }
        
        if (effectsSlider.handleMouseClick(mx, my, scaleX, scaleY)) {
            effectsVolume = effectsSlider.getValue();
            return;
        }
    }
    
    /**
     * Applies settings to the game.
     * Updates all settings in both the app and settings manager.
     */
    private void applySettings() {
        // Apply vsync setting
        app.setVsync(vsync);
        
        // Apply anti-aliasing setting
        app.setAntiAliasing(antiAliasing);
        
        // Save to settings manager
        SettingsManager.setAntiAliasing(antiAliasing);
        
        // Apply volume settings to audio system when implemented
        // app.setMusicVolume(musicVolume);
        // app.setEffectsVolume(effectsVolume);
    }
    
    /**
     * Updates the current mouse position and handles slider dragging.
     * 
     * @param mouseX X coordinate of the mouse
     * @param mouseY Y coordinate of the mouse
     */
    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
        
        float scaleX = app.getWidthScale();
        float scaleY = app.getHeightScale();
        
        // Update sliders if being dragged with scaling
        if (musicSlider.isDragging()) {
            musicSlider.handleMouseMove(mouseX, mouseY, scaleX, scaleY);
            musicVolume = musicSlider.getValue();
        }
        
        if (effectsSlider.isDragging()) {
            effectsSlider.handleMouseMove(mouseX, mouseY, scaleX, scaleY);
            effectsVolume = effectsSlider.getValue();
        }
    }

    /**
     * Handles mouse release events, ending slider dragging.
     * 
     * @param mouseX X coordinate of the mouse release
     * @param mouseY Y coordinate of the mouse release
     */
    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        musicSlider.handleMouseRelease(mouseX, mouseY);
        effectsSlider.handleMouseRelease(mouseX, mouseY);
    }
    
    /**
     * Handles keyboard key press events.
     * 
     * @param key The key code
     * @param action The action (press, release, etc.)
     */
    @Override
    public void handleKeyPress(int key, int action) {
        // Not needed for this screen
    }
    
    /**
     * Handles character input events.
     * 
     * @param codepoint The Unicode code point of the character
     */
    @Override
    public void handleCharInput(int codepoint) {
        // Not needed for this screen
    }
}
