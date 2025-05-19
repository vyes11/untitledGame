package thegame.screens;

import static org.lwjgl.opengl.GL11.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL;

import thegame.App;
import thegame.Screen;
import thegame.utils.FontRenderer;
import thegame.utils.CloudBackground;
import thegame.onScreenObjects.Button;

/**
 * Screen for selecting game levels.
 * Displays a grid of level buttons and handles navigation to other screens.
 */
public class LevelSelect implements Screen {
    private final App app;
    private FontRenderer fontRenderer;
    private Button backButton;
    private Button onlineLevelsButton; // New button for online levels
    private List<Button> levelButtons = new ArrayList<>();
    
    // Add cloud background
    private CloudBackground cloudBackground;
    
    private double currentMouseX = 0;
    private double currentMouseY = 0;
    
    private static final int NUM_LEVELS = 20; // Updated from 5 to 20 levels
    private static final int BUTTONS_PER_ROW = 5;
    
    /**
     * Creates a new level selection screen.
     *
     * @param app The main application instance
     */
    public LevelSelect(App app) {
        this.app = app;
        // Initialize cloud background with textured clouds
        cloudBackground = new CloudBackground(CloudBackground.RenderStyle.TEXTURED);
        initUI();
    }
    
    /**
     * Initializes the user interface elements.
     */
    private void initUI() {
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("/fonts/pf_tempesta_seven_bold.ttf");
        
        // Keep the back button where it was since it works correctly
        backButton = new Button(20, App.WINDOW_HEIGHT - 60, 200, 40, 0.7f, 0.3f, 0.6f, "Back to Main Menu"); // Dark pink
        
        // Add online levels button (right side of screen)
        onlineLevelsButton = new Button(App.WINDOW_WIDTH - 220, App.WINDOW_HEIGHT - 60, 200, 40, 0.9f, 0.5f, 0.8f, "Online Levels"); // Secondary pink
        
        // Make buttons smaller to fit all levels on screen
        int buttonWidth = 90;  // Reduced from 120
        int buttonHeight = 80; // Reduced from 120
        int horizontalSpacing = 20; // Reduced from 30
        int verticalSpacing = 20;   // Reduced from 30
        
        // Calculate starting position to center the grid of buttons
        float gridWidth = BUTTONS_PER_ROW * buttonWidth + (BUTTONS_PER_ROW - 1) * horizontalSpacing;
        float startX = (App.WINDOW_WIDTH - gridWidth) / 2;
        float startY = App.WINDOW_HEIGHT * 0.20f; // Start higher at 20% down from top (was 30%)
        
        // Create level buttons in a grid layout
        for (int i = 0; i < NUM_LEVELS; i++) {
            int row = i / BUTTONS_PER_ROW;
            int col = i % BUTTONS_PER_ROW;
            
            float x = startX + col * (buttonWidth + horizontalSpacing);
            float y = startY + row * (buttonHeight + verticalSpacing);
            
            // Alternate pink shades for level buttons
            float r = (i % 2 == 0) ? 0.9f : 1.0f;
            float g = (i % 2 == 0) ? 0.5f : 0.4f;
            float b = (i % 2 == 0) ? 0.8f : 0.7f;
            
            Button levelButton = new Button(x, y, buttonWidth, buttonHeight, r, g, b, "Level " + (i + 1));
            levelButtons.add(levelButton);
        }
    }
    
    /**
     * Renders the level selection screen with all buttons and UI elements.
     */
    @Override
    public void render() {
        // Set up 2D projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, App.WINDOW_WIDTH, App.WINDOW_HEIGHT, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        
        // Clear screen with a pink background
        glClearColor(1.0f, 0.7f, 0.9f, 1.0f); // Primary pink
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Update and render clouds
        cloudBackground.update();
        cloudBackground.render();
        
        // Render title text higher up on screen
        float titleY = App.WINDOW_HEIGHT * 0.15f; // Move title higher
        fontRenderer.renderCenteredText("Select Level", App.WINDOW_WIDTH / 4 + 60, titleY / 2, 2.0f, 0.8f, 0.2f, 0.5f, 1.0f); // Pink-hued text

        // Render all level buttons
        for (Button button : levelButtons) {
            button.render((float)currentMouseX, (float)currentMouseY);
        }
        
        // Render back button
        backButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Render online levels button with gray color if user is not logged in
        boolean isLoggedIn = app.isLoggedIn();
        if (!isLoggedIn) {
            // Gray out the button if not logged in
            onlineLevelsButton.setColors(0.7f, 0.5f, 0.6f); // Grayed pink
            onlineLevelsButton.render((float)currentMouseX, (float)currentMouseY);
            
            // Add "Login Required" text underneath the button
            float textX = onlineLevelsButton.getX() + onlineLevelsButton.getWidth() / 2;
            float textY = onlineLevelsButton.getY() + onlineLevelsButton.getHeight() + 5;
            fontRenderer.renderCenteredText("Login Required", textX / 0.8f, textY / 0.9f, 0.8f, 0.8f, 0.2f, 0.5f, 1.0f); // Pink-hued text
        } else {
            // Normal colored button if logged in
            onlineLevelsButton.setColors(0.9f, 0.5f, 0.8f); // Secondary pink
            onlineLevelsButton.render((float)currentMouseX, (float)currentMouseY);
        }
    }
    
    /**
     * Handles mouse click events on buttons to navigate between screens
     * and select levels.
     * 
     * @param mouseX X coordinate of the mouse click
     * @param mouseY Y coordinate of the mouse click
     */
    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
        if (backButton.handleMouseClick(mx, my)) {
            app.setCurrentScreen(new TitleScreen(app));
            return;
        }
        
        // Handle online levels button click
        if (onlineLevelsButton.handleMouseClick(mx, my) && app.isLoggedIn()) {
            // Go to the Online Level Select screen
            app.setCurrentScreen(new OnlineLevelSelectScreen(app));
            return;
        }
        
        // Check if a level button was clicked
        for (int i = 0; i < levelButtons.size(); i++) {
            if (levelButtons.get(i).handleMouseClick(mx, my)) {
                // Load the selected level
                app.setCurrentScreen(GameScreen.fromLevelNumber(app, i + 1));
                return;
            }
        }
    }
    
    /**
     * Handles mouse release events.
     * 
     * @param mouseX X coordinate of the mouse release
     * @param mouseY Y coordinate of the mouse release
     */
    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // Not needed for level select
    }
    
    /**
     * Updates the current mouse position.
     * 
     * @param mouseX The current X coordinate of the mouse
     * @param mouseY The current Y coordinate of the mouse
     */
    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }
    
    /**
     * Handles keyboard key press events.
     * 
     * @param key The key code
     * @param action The action (press, release, etc.)
     */
    @Override
    public void handleKeyPress(int key, int action) {
        // Not needed for level select
    }
    
    /**
     * Handles character input events.
     * 
     * @param codepoint The Unicode code point of the character
     */
    @Override
    public void handleCharInput(int codepoint) {
        // Not needed for level select as it has no text input fields
    }
}