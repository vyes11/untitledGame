package thegame.screens;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;

import org.lwjgl.opengl.GL;
import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;

import thegame.App;
import thegame.Screen;
import thegame.utils.FontRenderer;
import thegame.onScreenObjects.Button;
import thegame.onScreenObjects.TextBox;

public class LoginScreen implements Screen {
    private final App app;
    private FontRenderer fontRenderer;
    
    private TextBox usernameBox;
    private TextBox passwordBox;
    private Button loginButton;
    private Button backButton;
    private Button signupButton; // New signup button
    
    private String statusMessage = "";
    private float statusMessageTimer = 0;
    private boolean isError = false;
    
    private double currentMouseX = 0;
    private double currentMouseY = 0;
    
    // MongoDB connection
    private MongoClient mongoClient;
    private MongoCollection<Document> usersCollection;
    
    // Transition timer for safe screen changes
    private float transitionTimer = -1;
    private static final float TRANSITION_DELAY = 0.8f; // 0.8 seconds delay
    
    public LoginScreen(App app) {
        this.app = app;
        
        // Initialize MongoDB
        try {
            this.mongoClient = MongoClients.create("mongodb://localhost:27017");
            this.usersCollection = mongoClient.getDatabase("theGame").getCollection("data");
        } catch (Exception e) {
            statusMessage = "DB Connection Failed!";
            e.printStackTrace();
        }
        
        initUI();
    }
    
    private void initUI() {
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("F:/temp/theGame/untitledGame/app/src/main/resources/fonts/pf_tempesta_seven_bold.ttf");
        
        int inputWidth = 300;
        int inputHeight = 40;
        int buttonWidth = 200;
        int buttonHeight = 50;
        int spacing = 20;
        
        float centerX = App.WINDOW_WIDTH / 2;
        float startY = App.WINDOW_HEIGHT * 0.4f;
        
        // Create input fields centered on screen
        usernameBox = new TextBox(centerX - inputWidth/2, startY, inputWidth, inputHeight, "Username");
        passwordBox = new TextBox(centerX - inputWidth/2, startY + inputHeight + spacing, inputWidth, inputHeight, "Password");
        passwordBox.setPasswordMode(true);
        
        // Create buttons
        loginButton = new Button(centerX - buttonWidth/2, startY + 2*(inputHeight + spacing), buttonWidth, buttonHeight, 0.5f, 0.2f, 0.7f, "Login");
        // Add signup button below login button with a green color scheme
        signupButton = new Button(centerX - buttonWidth/2, startY + 2*(inputHeight + spacing) + buttonHeight + spacing, 
                              buttonWidth, buttonHeight, 0.2f, 0.6f, 0.3f, "Create Account");
        backButton = new Button(20, App.WINDOW_HEIGHT - 60, 200, 40, 0.3f, 0.3f, 0.6f, "Back");
    }
    
    @Override
    public void render() {
        // Set up 2D projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, App.WINDOW_WIDTH, App.WINDOW_HEIGHT, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        
        // Clear screen with a purple background
        glClearColor(0.2f, 0.1f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Render title
        float titleY = App.WINDOW_HEIGHT * 0.25f;
        fontRenderer.renderCenteredText("User Login", App.WINDOW_WIDTH / 2, titleY, 2.0f);
        
        // Render input fields and buttons
        usernameBox.render((float)currentMouseX, (float)currentMouseY);
        passwordBox.render((float)currentMouseX, (float)currentMouseY);
        loginButton.render((float)currentMouseX, (float)currentMouseY);
        signupButton.render((float)currentMouseX, (float)currentMouseY); // Render the signup button
        backButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Render field labels
        fontRenderer.renderText("Username:", usernameBox.getX() - 180, usernameBox.getY() + 10, 1.2f);
        fontRenderer.renderText("Password:", passwordBox.getX() - 180, passwordBox.getY() + 10, 1.2f);
        
        // Render status message if there is one
        if (!statusMessage.isEmpty() && statusMessageTimer > 0) {
            fontRenderer.renderCenteredText(
                statusMessage, 
                App.WINDOW_WIDTH / 2, 
                App.WINDOW_HEIGHT * 0.6f, 
                1.5f,
                isError ? 1.0f : 0.2f,  // Red if error, green otherwise
                isError ? 0.2f : 0.8f, 
                0.2f,
                1.0f
            );
            statusMessageTimer -= 0.016; // Assuming 60fps
        }
        
        // Check if we need to transition to level select screen
        if (transitionTimer > 0) {
            transitionTimer -= 0.016f; // Assuming ~60fps
            if (transitionTimer <= 0) {
                app.setCurrentScreen(new LevelSelect(app));
                return;
            }
        }
    }
    
    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
        // Handle text box focus
        usernameBox.handleMouseClick(mx, my);
        passwordBox.handleMouseClick(mx, my);
        
        if (loginButton.handleMouseClick(mx, my)) {
            // Perform login
            String username = usernameBox.getText();
            String password = passwordBox.getText();
            
            if (username.isEmpty() || password.isEmpty()) {
                showStatusMessage("Please enter both username and password", true);
            } else {
                // Validate against database
                validateLogin(username, password);
            }
        } else if (signupButton.handleMouseClick(mx, my)) {
            // Navigate to the signup screen
            app.setCurrentScreen(new SignupScreen(app));
        } else if (backButton.handleMouseClick(mx, my)) {
            app.setCurrentScreen(new TitleScreen(app));
        }
    }
    
    private void validateLogin(String username, String password) {
        try {
            // Query the database for the user
            Document query = new Document("username", username).append("password", password);
            Document user = usersCollection.find(query).first();
            
            if (user != null) {
                // Login successful
                showStatusMessage("Login successful!", false);
                
                // Store the user data in the app
                app.setLoggedInUser(user);
                
                // Set timer for transition instead of using a separate thread
                transitionTimer = TRANSITION_DELAY;
            } else {
                // Login failed
                showStatusMessage("Invalid username or password", true);
            }
        } catch (Exception e) {
            showStatusMessage("Error connecting to database", true);
            e.printStackTrace();
        }
    }
    
    private void showStatusMessage(String message, boolean isError) {
        this.statusMessage = message;
        this.statusMessageTimer = 3.0f; // Show for 3 seconds
        this.isError = isError;
    }
    
    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // Not needed for this screen
    }
    
    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }
    
    @Override
    public void handleKeyPress(int key, int action) {
        // Only handle special keys here (like backspace, enter)
        if (action == GLFW_PRESS || action == GLFW_REPEAT) {
            if (usernameBox.isFocused()) {
                usernameBox.handleKeyPress(key, action);  // Fix: added action parameter
            } else if (passwordBox.isFocused()) {
                passwordBox.handleKeyPress(key, action);  // Fix: added action parameter
            }
            
            // Handle Enter key for login
            if (key == GLFW_KEY_ENTER) {
                handleMouseClick(loginButton.getX() + loginButton.getWidth()/2, 
                                loginButton.getY() + loginButton.getHeight()/2);
            }
        }
    }
    
    @Override
    public void handleCharInput(int codepoint) {
        // Handle character input (letters, numbers, symbols)
        // This is where actual text entry happens
        if (usernameBox.isFocused()) {
            usernameBox.handleCharInput(codepoint);
        } else if (passwordBox.isFocused()) {
            passwordBox.handleCharInput(codepoint);
        }
    }
    
    // Clean up resources when screen is closed
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}