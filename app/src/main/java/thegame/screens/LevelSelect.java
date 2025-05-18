package thegame.screens;

import static org.lwjgl.opengl.GL11.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL;

import thegame.App;
import thegame.Screen;
import thegame.utils.FontRenderer;
import thegame.onScreenObjects.Button;

public class LevelSelect implements Screen {
    private final App app;
    private FontRenderer fontRenderer;
    private Button backButton;
    private Button onlineLevelsButton; // New button for online levels
    private List<Button> levelButtons = new ArrayList<>();
    
    private double currentMouseX = 0;
    private double currentMouseY = 0;
    
    private static final int NUM_LEVELS = 5; // Assuming 5 levels are available
    private static final int BUTTONS_PER_ROW = 4;
    
    public LevelSelect(App app) {
        this.app = app;
        initUI();
    }
    
    private void initUI() {
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("F:/temp/theGame/untitledGame/app/src/main/resources/fonts/pf_tempesta_seven_bold.ttf");
        
        // Keep the back button where it was since it works correctly
        backButton = new Button(20, App.WINDOW_HEIGHT - 60, 200, 40, 0.3f, 0.3f, 0.6f, "Back to Main Menu");
        
        // Add online levels button (right side of screen)
        onlineLevelsButton = new Button(App.WINDOW_WIDTH - 220, App.WINDOW_HEIGHT - 60, 200, 40, 0.4f, 0.4f, 0.7f, "Online Levels");
        
        // Adjust level button layout to be more centered and higher
        int buttonWidth = 120;
        int buttonHeight = 120;
        int horizontalSpacing = 30;
        int verticalSpacing = 30;
        
        // Calculate starting position to center the grid of buttons
        float gridWidth = BUTTONS_PER_ROW * buttonWidth + (BUTTONS_PER_ROW - 1) * horizontalSpacing;
        float startX = (App.WINDOW_WIDTH - gridWidth) / 2;
        float startY = App.WINDOW_HEIGHT * 0.3f; // Start at 30% down from top
        
        // Create level buttons in a grid layout
        for (int i = 0; i < NUM_LEVELS; i++) {
            int row = i / BUTTONS_PER_ROW;
            int col = i % BUTTONS_PER_ROW;
            
            float x = startX + col * (buttonWidth + horizontalSpacing);
            float y = startY + row * (buttonHeight + verticalSpacing);
            
            Button levelButton = new Button(x, y, buttonWidth, buttonHeight, 0.2f, 0.5f, 0.7f, "Level " + (i + 1));
            levelButtons.add(levelButton);
        }
    }
    
    @Override
    public void render() {
        // Set up 2D projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, App.WINDOW_WIDTH, App.WINDOW_HEIGHT, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        
        // Clear screen with a green background
        glClearColor(0.1f, 0.3f, 0.1f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Render title text higher up on screen
        float titleY = App.WINDOW_HEIGHT * 0.15f; // Move title higher
        fontRenderer.renderCenteredText("Select Level", App.WINDOW_WIDTH / 2, titleY, 2.0f);
        
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
            onlineLevelsButton.setColors(0.5f, 0.5f, 0.5f); // Grayed out colors
            onlineLevelsButton.render((float)currentMouseX, (float)currentMouseY);
            
            // Add "Login Required" text underneath the button
            float textX = onlineLevelsButton.getX() + onlineLevelsButton.getWidth() / 2;
            float textY = onlineLevelsButton.getY() + onlineLevelsButton.getHeight() + 5;
            fontRenderer.renderCenteredText("Login Required", textX, textY, 0.8f, 0.8f, 0.2f, 0.2f, 1.0f);
        } else {
            // Normal colored button if logged in
            onlineLevelsButton.setColors(0.4f, 0.4f, 0.7f); // Original colors
            onlineLevelsButton.render((float)currentMouseX, (float)currentMouseY);
        }
    }
    
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
    
    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // Not needed for level select
    }
    
    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }
    
    @Override
    public void handleKeyPress(int key, int action) {
        // Not needed for level select
    }
    
    @Override
    public void handleCharInput(int codepoint) {
        // Not needed for level select as it has no text input fields
    }
}