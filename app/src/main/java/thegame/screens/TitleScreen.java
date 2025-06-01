package thegame.screens;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex2f;

import thegame.App;
import thegame.Screen;
import thegame.onScreenObjects.Button;
import thegame.utils.FontRenderer;

/**
 * The title screen of the game, first screen shown to the user.
 * Displays the game title, login status, and navigation buttons.
 */
public class TitleScreen implements Screen {
    private final App app;
    private FontRenderer fontRenderer;
    private Button playButton;
    private Button loginButton;
    private Button settingsButton;
    private Button quitButton;
    
    private float backgroundRotation = 0;
    private double currentMouseX = 0, currentMouseY = 0;

    /**
     * Constructs a new title screen.
     * 
     * @param app The main application instance
     */
    public TitleScreen(App app) {
        this.app = app;
        
        // Initialize font renderer
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("/fonts/pf_tempesta_seven_bold.ttf");
        
        // Create buttons
        int buttonWidth = 250;
        int buttonHeight = 50;
        int centerX = App.WINDOW_WIDTH / 2 - buttonWidth / 2;
        int startY = 250;
        int spacing = 60;
        
        playButton = new Button(centerX, startY, buttonWidth, buttonHeight, 0.9f, 0.5f, 0.8f, "Play"); // Secondary pink
        
        // Create login button with grey color to indicate disabled state
        loginButton = new Button(centerX, startY + spacing, buttonWidth, buttonHeight, 0.5f, 0.5f, 0.5f, // Grey color
                           app.isLoggedIn() ? "Logout" : "Login");
        
        settingsButton = new Button(centerX, startY + spacing * 2, buttonWidth, buttonHeight, 1.0f, 0.4f, 0.7f, "Settings"); // Accent pink
        quitButton = new Button(centerX, startY + spacing * 3, buttonWidth, buttonHeight, 0.8f, 0.2f, 0.5f, "Quit Game"); // Dark accent
    }

    /**
     * Renders the title screen, including animated background, 
     * title text, and buttons.
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
        
        // Draw animated background
        drawBackground();
        
        // Draw title - repositioned even more to the upper-left
        String title = "FLAULISS AUDITS";
        float titleX = App.WINDOW_WIDTH * 0.3f; // Position even more to the left (was 0.15f)
        float titleY = 30; // Position higher up (was 80)
                
        // Draw title with custom alignment for the a, u, and last s
        fontRenderer.renderCenteredText(title, titleX, titleY, 3.0f, 0.8f, 0.2f, 0.5f, 1.0f); // Pink-hued text
        
        // Draw login status if logged in
        if (app.isLoggedIn()) {
            fontRenderer.renderCenteredText("Logged in as " + app.getUsername(), 
                                          App.WINDOW_WIDTH / 2, 200, 1.0f, 0.9f, 0.4f, 0.7f, 1.0f); // Accent pink
        }
        
        // Draw buttons
        playButton.render((float)currentMouseX, (float)currentMouseY);
        loginButton.render((float)currentMouseX, (float)currentMouseY);
        settingsButton.render((float)currentMouseX, (float)currentMouseY);
        quitButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Increase rotation for next frame
        backgroundRotation += 0.05f;
    }
    
    /**
     * Draws an animated background with radiating lines.
     */
    private void drawBackground() {
        // Calculate time-based animation
        float time = (float)System.currentTimeMillis() / 1000.0f;
        
        // Draw animated background pattern
        glPushMatrix();
        glTranslatef(App.WINDOW_WIDTH / 2, App.WINDOW_HEIGHT / 2, 0);
        glRotatef(backgroundRotation, 0, 0, 1);
        
        // Draw radiating lines
        int numLines = 24;
        float radius = Math.max(App.WINDOW_WIDTH, App.WINDOW_HEIGHT) * 0.8f;
        glLineWidth(1.0f);
        
        for (int i = 0; i < numLines; i++) {
            float angle = (float)i / numLines * (float)Math.PI * 2.0f;
            float x = (float)Math.cos(angle) * radius;
            float y = (float)Math.sin(angle) * radius;
            
            // Make the lines pulse with time - pink hues
            float pulseIntensity = 0.3f + 0.2f * (float)Math.sin(time * 2.0f + i * 0.2f);
            glColor4f(0.9f, 0.5f, 0.8f, pulseIntensity); // Secondary pink
            
            glBegin(GL_LINES);
            glVertex2f(0, 0);
            glVertex2f(x, y);
            glEnd();
        }
        
        glPopMatrix();
    }
    
    /**
     * Handles mouse clicks on buttons and transitions to appropriate screens.
     * 
     * @param mouseX The x coordinate of the mouse click
     * @param mouseY The y coordinate of the mouse click
     */
    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
        if (playButton.handleMouseClick(mx, my)) {
            app.setCurrentScreen(new LevelSelect(app));
        } 
        // Remove or comment out login button click handling
        /*
        else if (loginButton.handleMouseClick(mx, my)) {
            if (app.isLoggedIn()) {
                // Logout
                app.clearLoggedInUser();
                loginButton.setCaption("Login");
            } else {
                // Show login screen
                app.setCurrentScreen(new LoginScreen(app));
            }
        }
        */
        else if (settingsButton.handleMouseClick(mx, my)) {
            app.setCurrentScreen(new SettingsScreen(app));
        } else if (quitButton.handleMouseClick(mx, my)) {
            System.exit(0);
        }
    }

    /**
     * Updates the current mouse position.
     * 
     * @param mouseX The current x coordinate of the mouse
     * @param mouseY The current y coordinate of the mouse
     */
    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        currentMouseX = mouseX;
        currentMouseY = mouseY;
    }

    /**
     * Handles mouse button release events.
     * 
     * @param mouseX The x coordinate where the mouse was released
     * @param mouseY The y coordinate where the mouse was released
     */
    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // Not needed
    }
    
    /**
     * Handles keyboard key press events.
     * 
     * @param key The key code that was pressed
     * @param action The action (press, release, etc.)
     */
    @Override
    public void handleKeyPress(int key, int action) {
        // Not needed
    }
    
    /**
     * Handles character input events.
     * 
     * @param codepoint The Unicode code point of the character
     */
    @Override
    public void handleCharInput(int codepoint) {
        // Not needed
    }
}