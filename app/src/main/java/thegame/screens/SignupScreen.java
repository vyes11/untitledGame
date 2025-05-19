package thegame.screens;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;

import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;

import java.util.Random;

import thegame.App;
import thegame.Screen;
import thegame.onScreenObjects.Button;
import thegame.utils.EmailSender;
import thegame.utils.FontRenderer;
import thegame.utils.TextInput;

/**
 * Screen for new user registration.
 * Handles user signup with email verification process.
 */
public class SignupScreen implements Screen {
    private final App app;
    private TextInput emailInput;
    private TextInput usernameInput;
    private TextInput passwordInput;
    private TextInput verificationInput; // New field for verification code
    private String statusMessage = "";
    private boolean isSuccess = false;
    
    // Verification related
    private enum SignupState { INITIAL_INFO, VERIFICATION, PROCESSING }
    private SignupState currentState = SignupState.INITIAL_INFO;
    private String generatedVerificationCode = "";

    // Transition timer for screen changes
    private float transitionTimer = -1;
    private static final float TRANSITION_DELAY = 1.5f; // 1.5 seconds delay
    
    // MongoDB
    private MongoClient mongoClient;
    private MongoCollection<Document> usersCollection;
    
    // UI elements
    private FontRenderer fontRenderer;
    private Button submitButton; // Will be either "Sign Up" or "Verify" depending on state
    private Button backButton;
    private Button resendButton; // For resending verification code
    
    // Mouse position
    private double currentMouseX = 0;
    private double currentMouseY = 0;

    /**
     * Creates a new signup screen.
     * 
     * @param app The main application instance
     */
    public SignupScreen(App app) {
        this.app = app;

        // Initialize MongoDB
        try {
            this.mongoClient = MongoClients.create("mongodb://localhost:27017");
            this.usersCollection = mongoClient.getDatabase("theGame").getCollection("data");
        } catch (Exception e) {
            statusMessage = "DB Connection Failed!";
            e.printStackTrace();
        }
        
        // Initialize UI
        fontRenderer = new FontRenderer();
        fontRenderer.loadFont("/fonts/pf_tempesta_seven_bold.ttf");
        
        // Create text inputs
        emailInput = new TextInput(App.WINDOW_WIDTH/2 - 120, 230, 400, 40);
        usernameInput = new TextInput(App.WINDOW_WIDTH/2 - 120, 310, 400, 40);
        passwordInput = new TextInput(App.WINDOW_WIDTH/2 - 120, 390, 400, 40);
        passwordInput.setPasswordMode(true);

        // Set email input focused by default
        emailInput.setFocused(true);
        usernameInput.setFocused(false);
        passwordInput.setFocused(false);
        
        // Create verification code input (initially hidden)
        verificationInput = new TextInput(App.WINDOW_WIDTH/2 - 120, 380, 240, 40);
        
        // Create buttons with pink theme
        submitButton = new Button(App.WINDOW_WIDTH/2 - 60, 490, 120, 40, 0.9f, 0.5f, 0.8f, "Sign Up"); // Secondary pink
        backButton = new Button(20, App.WINDOW_HEIGHT - 60, 80, 30, 0.7f, 0.3f, 0.6f, "Back"); // Dark pink
        resendButton = new Button(App.WINDOW_WIDTH/2 - 60, 310, 120, 30, 0.8f, 0.4f, 0.7f, "Resend Code"); // Hot pink
    }

    /**
     * Renders the signup screen with appropriate UI elements based on the current state.
     */
    @Override
    public void render() {
        // Draw background
        glClearColor(1.0f, 0.7f, 0.9f, 1.0f); // Primary pink
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Draw panel
        float panelWidth = App.WINDOW_WIDTH * 0.5f;
        float panelHeight = App.WINDOW_HEIGHT * 0.65f;
        float panelX = App.WINDOW_WIDTH/2 - panelWidth/2;
        float panelY = App.WINDOW_HEIGHT/2 - panelHeight/2;
        
        glColor4f(0.9f, 0.5f, 0.8f, 1.0f); // Secondary pink
        glBegin(GL_QUADS);
        glVertex2f(panelX, panelY);
        glVertex2f(panelX + panelWidth, panelY);
        glVertex2f(panelX + panelWidth, panelY + panelHeight);
        glVertex2f(panelX, panelY + panelHeight);
        glEnd();
        
        // Title varies based on state
        String title = currentState == SignupState.INITIAL_INFO ? 
                      "Create Your Account" : "Verify Your Email";
        fontRenderer.renderCenteredText(title, App.WINDOW_WIDTH/2 - 150, panelY -20 , 1.5f, 0.8f, 0.2f, 0.5f, 1.0f); // Pink-hued text
        
        // Draw separator
        glColor4f(0.7f, 0.3f, 0.6f, 1.0f); // Dark pink
        glLineWidth(2.0f);
        glBegin(GL_LINES);
        glVertex2f(panelX + 20, panelY + 70);
        glVertex2f(panelX + panelWidth - 20, panelY + 70);
        glEnd();
        glLineWidth(1.0f);
        
        if (currentState == SignupState.INITIAL_INFO) {
            // Draw initial signup fields
            fontRenderer.renderText("Email:", panelX + 50, panelY + 110, 1.0f);
            emailInput.render(fontRenderer, (float)currentMouseX, (float)currentMouseY);
            
            fontRenderer.renderText("Username:", panelX + 50, panelY + 190, 1.0f);
            usernameInput.render(fontRenderer, (float)currentMouseX, (float)currentMouseY);
            
            fontRenderer.renderText("Password:", panelX + 50, panelY + 270, 1.0f);
            passwordInput.render(fontRenderer, (float)currentMouseX, (float)currentMouseY);
            
            // Update button text
            submitButton.setText("Sign Up");
        } 
        else if (currentState == SignupState.VERIFICATION) {
            // Draw verification UI
            fontRenderer.renderCenteredText("A verification code has been sent to:", 
                                          App.WINDOW_WIDTH/2, panelY + 120, 1.0f);
            fontRenderer.renderCenteredText(emailInput.getText(), 
                                          App.WINDOW_WIDTH/2, panelY + 150, 1.0f, 0.2f, 0.8f, 1.0f, 1.0f);
            
            fontRenderer.renderCenteredText("Enter verification code:", 
                                          App.WINDOW_WIDTH/2, panelY + 230, 1.0f);
            verificationInput.render(fontRenderer, (float)currentMouseX, (float)currentMouseY);
            
            // Render resend button
            resendButton.render((float)currentMouseX, (float)currentMouseY);
            
            // Update button text
            submitButton.setText("Verify");
        }
        
        // Draw status message
        if (!statusMessage.isEmpty()) {
            float messageY = currentState == SignupState.VERIFICATION ? 
                             panelY + panelHeight - 130 : 
                             panelY + panelHeight - 100;
            
            if (isSuccess) {
                fontRenderer.renderCenteredText(statusMessage, App.WINDOW_WIDTH/2, messageY, 1.0f, 0.2f, 0.8f, 0.2f, 1.0f);
            } else {
                fontRenderer.renderCenteredText(statusMessage, App.WINDOW_WIDTH/2, messageY, 1.0f, 0.8f, 0.2f, 0.2f, 1.0f);
            }
        }
        
        // Draw buttons
        submitButton.render((float)currentMouseX, (float)currentMouseY);
        backButton.render((float)currentMouseX, (float)currentMouseY);
        
        // Check if we need to transition to login screen
        if (transitionTimer > 0) {
            transitionTimer -= 0.016f; // Assuming ~60fps
            if (transitionTimer <= 0) {
                app.setCurrentScreen(new LoginScreen(app));
                return;
            }
        }
    }

    /**
     * Initiates the signup process by validating inputs and sending verification email.
     */
    private void initiateSignup() {
        if (emailInput.getText().isEmpty() || usernameInput.getText().isEmpty() || passwordInput.getText().isEmpty()) {
            statusMessage = "Please fill all fields!";
            isSuccess = false;
            return;
        }
        
        // Validate email format (simple validation)
        if (!emailInput.getText().contains("@") || !emailInput.getText().contains(".")) {
            statusMessage = "Please enter a valid email address!";
            isSuccess = false;
            return;
        }

        try {
            // Check if username already exists
            Document existingUser = usersCollection.find(new Document("username", usernameInput.getText())).first();
            if (existingUser != null) {
                statusMessage = "Username already exists!";
                isSuccess = false;
                return;
            }
            
            // Check if email already exists
            existingUser = usersCollection.find(new Document("email", emailInput.getText())).first();
            if (existingUser != null) {
                statusMessage = "Email already registered!";
                isSuccess = false;
                return;
            }

            // Generate verification code (6 digits)
            Random random = new Random();
            generatedVerificationCode = String.format("%06d", random.nextInt(1000000));
            
            // Send verification email
            boolean emailSent = EmailSender.sendVerificationEmail(
                emailInput.getText(), 
                generatedVerificationCode
            );
            
            if (emailSent) {
                // Switch to verification state
                currentState = SignupState.VERIFICATION;
                statusMessage = "Verification code sent!";
                isSuccess = true;
                verificationInput.setText("");  // Clear any previous input
                verificationInput.setFocused(true);  // Focus on verification input
            } else {
                statusMessage = "Failed to send verification email!";
                isSuccess = false;
            }
        } catch (Exception e) {
            statusMessage = "Error: " + e.getMessage();
            isSuccess = false;
            e.printStackTrace();
        }
    }
    
    /**
     * Verifies the entered verification code against the generated one.
     */
    private void verifyCode() {
        if (verificationInput.getText().isEmpty()) {
            statusMessage = "Please enter verification code!";
            isSuccess = false;
            return;
        }
        
        if (verificationInput.getText().equals(generatedVerificationCode)) {
            // Code matched, create the user
            completeSignup();
        } else {
            statusMessage = "Invalid verification code!";
            isSuccess = false;
        }
    }
    
    /**
     * Completes the signup process by creating the user in the database.
     */
    private void completeSignup() {
        try {
            // Generate unique user ID (4 digits, starting from 0000)
            String userId = generateNextUserId();
            
            // Create new user document
            Document newUser = new Document()
                .append("userId", userId)
                .append("email", emailInput.getText())
                .append("username", usernameInput.getText())
                .append("password", passwordInput.getText())
                .append("emailVerified", true)
                .append("levels", new Document()); // Empty levels

            usersCollection.insertOne(newUser);
            statusMessage = "Account created successfully!";
            isSuccess = true;
            
            // Set timer to transition to login screen
            // This ensures the transition happens on the main render thread
            transitionTimer = TRANSITION_DELAY;
        } catch (Exception e) {
            statusMessage = "Error creating user!";
            isSuccess = false;
            e.printStackTrace();
        }
    }
    
    /**
     * Generates the next available user ID in sequence.
     * 
     * @return The next available user ID as a formatted string
     */
    private String generateNextUserId() {
        try {
            // Query to find the highest existing user ID
            Document highestIdDoc = usersCollection
                .find()
                .sort(new Document("userId", -1)) // Sort by userId in descending order
                .limit(1)                         // Get only the highest one
                .first();
            
            int nextId = 0; // Default starting ID
            
            if (highestIdDoc != null && highestIdDoc.containsKey("userId")) {
                String highestId = highestIdDoc.getString("userId");
                try {
                    // Parse the existing highest ID and increment by 1
                    nextId = Integer.parseInt(highestId) + 1;
                } catch (NumberFormatException e) {
                    // If parsing fails, start from 0
                    nextId = 0;
                }
            }
            
            // Format as 4-digit number with leading zeros
            return String.format("%04d", nextId);
        } catch (Exception e) {
            // Fallback to a random ID if something goes wrong
            return String.format("%04d", new Random().nextInt(10000));
        }
    }
    
    /**
     * Resends the verification code to the user's email.
     */
    private void resendVerificationCode() {
        // Generate new verification code
        Random random = new Random();
        generatedVerificationCode = String.format("%06d", random.nextInt(1000000));
        
        // Resend email
        boolean emailSent = EmailSender.sendVerificationEmail(
            emailInput.getText(), 
            generatedVerificationCode
        );
        
        if (emailSent) {
            statusMessage = "New verification code sent!";
            isSuccess = true;
        } else {
            statusMessage = "Failed to resend code!";
            isSuccess = false;
        }
    }

    /**
     * Cleans up resources used by the screen.
     */
    public void cleanup() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    /**
     * Handles mouse click events on buttons and input fields.
     * 
     * @param mouseX X coordinate of the mouse click
     * @param mouseY Y coordinate of the mouse click
     */
    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        float mx = (float)mouseX;
        float my = (float)mouseY;
        
        // Handle back button first (always available)
        if (backButton.handleMouseClick(mx, my)) {
            if (currentState == SignupState.INITIAL_INFO) {
                app.setCurrentScreen(new LoginScreen(app));
            } else {
                // Go back to initial signup state
                currentState = SignupState.INITIAL_INFO;
                statusMessage = "";
            }
            return;
        }
        
        // Handle state-specific clicks
        if (currentState == SignupState.INITIAL_INFO) {
            // Check text inputs
            emailInput.handleMouseClick(mx, my);
            usernameInput.handleMouseClick(mx, my);
            passwordInput.handleMouseClick(mx, my);

            // Ensure only one input is focused at a time
            if (emailInput.isFocused()) {
                usernameInput.setFocused(false);
                passwordInput.setFocused(false);
            } else if (usernameInput.isFocused()) {
                emailInput.setFocused(false);
                passwordInput.setFocused(false);
            } else if (passwordInput.isFocused()) {
                emailInput.setFocused(false);
                usernameInput.setFocused(false);
            }
            
            // Check signup button
            if (submitButton.handleMouseClick(mx, my)) {
                initiateSignup();
            }
        } 
        else if (currentState == SignupState.VERIFICATION) {
            // Check verification input
            verificationInput.handleMouseClick(mx, my);
            
            // Check verify button
            if (submitButton.handleMouseClick(mx, my)) {
                verifyCode();
            }
            
            // Check resend button
            if (resendButton.handleMouseClick(mx, my)) {
                resendVerificationCode();
            }
        }
    }

    /**
     * Updates the current mouse position.
     * 
     * @param mouseX X coordinate of the mouse
     * @param mouseY Y coordinate of the mouse
     */
    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }

    /**
     * Handles mouse release events.
     * 
     * @param mouseX X coordinate of the mouse release
     * @param mouseY Y coordinate of the mouse release
     */
    @Override
    public void handleMouseRelease(double x, double y) {
        // Not used
    }
    
    /**
     * Handles keyboard key press events like tab navigation and backspace.
     * 
     * @param key The key code
     * @param action The action (press, release, etc.)
     */
    @Override
    public void handleKeyPress(int key, int action) {
        if (action == GLFW_PRESS || action == GLFW_REPEAT) {
            if (key == GLFW_KEY_TAB) {
                if (currentState == SignupState.INITIAL_INFO) {
                    if (emailInput.isFocused()) {
                        emailInput.setFocused(false);
                        usernameInput.setFocused(true);
                    } else if (usernameInput.isFocused()) {
                        usernameInput.setFocused(false);
                        passwordInput.setFocused(true);
                    } else if (passwordInput.isFocused()) {
                        passwordInput.setFocused(false);
                        emailInput.setFocused(true);
                    }
                }
            } else if (key == GLFW_KEY_ENTER) {
                if (currentState == SignupState.INITIAL_INFO) {
                    initiateSignup();
                } else if (currentState == SignupState.VERIFICATION) {
                    verifyCode();
                }
            } else if (key == GLFW_KEY_BACKSPACE) {
                if (currentState == SignupState.INITIAL_INFO) {
                    if (emailInput.isFocused()) {
                        emailInput.handleBackspace();
                    } else if (usernameInput.isFocused()) {
                        usernameInput.handleBackspace();
                    } else if (passwordInput.isFocused()) {
                        passwordInput.handleBackspace();
                    }
                } else if (currentState == SignupState.VERIFICATION) {
                    if (verificationInput.isFocused()) {
                        verificationInput.handleBackspace();
                    }
                }
            }
        }
    }

    /**
     * Handles character input for text fields.
     * 
     * @param codepoint The Unicode code point of the character
     */
    @Override
    public void handleCharInput(int codepoint) {
        // Handle character input for text fields
        if (currentState == SignupState.INITIAL_INFO) {
            if (emailInput.isFocused()) {
                emailInput.handleCharInput((char)codepoint);
            } else if (usernameInput.isFocused()) {
                usernameInput.handleCharInput((char)codepoint);
            } else if (passwordInput.isFocused()) {
                passwordInput.handleCharInput((char)codepoint);
            }
        } else if (currentState == SignupState.VERIFICATION) {
            if (verificationInput.isFocused()) {
                // Only allow numbers for verification code
                char c = (char)codepoint;
                if (Character.isDigit(c)) {
                    verificationInput.handleCharInput(c);
                }
            }
        }
    }
}