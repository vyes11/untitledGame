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
        fontRenderer.loadFont("F:/temp/theGame/untitledGame/app/src/main/resources/fonts/pf_tempesta_seven_bold.ttf");
        
        // Create text inputs
        emailInput = new TextInput(App.WINDOW_WIDTH/2 - 200, 150, 400, 40);
        usernameInput = new TextInput(App.WINDOW_WIDTH/2 - 200, 230, 400, 40);
        passwordInput = new TextInput(App.WINDOW_WIDTH/2 - 200, 310, 400, 40);
        passwordInput.setPasswordMode(true);
        
        // Create verification code input (initially hidden)
        verificationInput = new TextInput(App.WINDOW_WIDTH/2 - 120, 230, 240, 40);
        
        // Create buttons
        submitButton = new Button(App.WINDOW_WIDTH/2 - 60, 390, 120, 40, 0.2f, 0.6f, 0.8f, "Sign Up");
        backButton = new Button(20, App.WINDOW_HEIGHT - 60, 80, 30, 0.3f, 0.3f, 0.6f, "Back");
        resendButton = new Button(App.WINDOW_WIDTH/2 - 60, 310, 120, 30, 0.6f, 0.6f, 0.2f, "Resend Code");
    }

    @Override
    public void render() {
        // Draw background
        glClearColor(0.1f, 0.1f, 0.2f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);
        
        // Draw panel
        float panelWidth = App.WINDOW_WIDTH * 0.5f;
        float panelHeight = App.WINDOW_HEIGHT * 0.65f;
        float panelX = App.WINDOW_WIDTH/2 - panelWidth/2;
        float panelY = App.WINDOW_HEIGHT/2 - panelHeight/2;
        
        glColor4f(0.15f, 0.15f, 0.25f, 1.0f);
        glBegin(GL_QUADS);
        glVertex2f(panelX, panelY);
        glVertex2f(panelX + panelWidth, panelY);
        glVertex2f(panelX + panelWidth, panelY + panelHeight);
        glVertex2f(panelX, panelY + panelHeight);
        glEnd();
        
        // Title varies based on state
        String title = currentState == SignupState.INITIAL_INFO ? 
                      "Create Your Account" : "Verify Your Email";
        fontRenderer.renderCenteredText(title, App.WINDOW_WIDTH/2, panelY + 40, 1.5f, 0.2f, 0.6f, 1.0f, 1.0f);
        
        // Draw separator
        glColor4f(0.3f, 0.3f, 0.6f, 1.0f);
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
                                          App.WINDOW_WIDTH/2, panelY + 200, 1.0f);
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
                    System.err.println("Error parsing user ID: " + e.getMessage());
                    nextId = 0;
                }
            }
            
            // Format as 4-digit number with leading zeros
            return String.format("%04d", nextId);
        } catch (Exception e) {
            System.err.println("Error generating user ID: " + e.getMessage());
            e.printStackTrace();
            // Fallback to a random ID if something goes wrong
            return String.format("%04d", new Random().nextInt(10000));
        }
    }
    
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

    public void cleanup() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

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

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }

    @Override
    public void handleMouseRelease(double x, double y) {
        // Not used
    }
    
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