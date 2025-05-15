package thegame.screens;

import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;

import thegame.App;
import thegame.Screen;
import thegame.utils.ImGuiUtils;

public class SignupScreen implements Screen {
    private final App app;
    private final ImString email = new ImString(256);
    private final ImString username = new ImString(256);
    private final ImString password = new ImString(256);
    private String statusMessage = "";
    private boolean isSuccess = false;

    // MongoDB
    private MongoClient mongoClient;
    private MongoCollection<Document> usersCollection;

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
    }

    @Override
    public void render() {
        float windowWidth = ImGui.getIO().getDisplaySizeX();
        float windowHeight = ImGui.getIO().getDisplaySizeY();
        
        // Create a centered modal window
        float formWidth = windowWidth * 0.5f;
        float formHeight = windowHeight * 0.6f;
        
        ImGui.setNextWindowPos(
            windowWidth * 0.5f - formWidth * 0.5f,
            windowHeight * 0.5f - formHeight * 0.5f
        );
        ImGui.setNextWindowSize(formWidth, formHeight);
        
        ImGui.begin("Signup Screen", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        // Center the title text
        float titleWidth = ImGui.calcTextSize("Create Your Account").x;
        ImGui.setCursorPosX((formWidth - titleWidth) * 0.5f);
        ImGui.textColored(0.2f, 0.6f, 1.0f, 1.0f, "Create Your Account");
        ImGui.spacing();
        ImGui.separator();
        ImGui.spacing();

        // Add spacing for better layout
        ImGui.dummy(0, 20);
        
        // Email field
        ImGui.text("Email:");
        ImGui.setNextItemWidth(formWidth * 0.8f);
        ImGui.inputText("##email", email, ImGuiInputTextFlags.None);
        
        // Add spacing between fields
        ImGui.dummy(0, 10);
        
        // Username field
        ImGui.text("Username:");
        ImGui.setNextItemWidth(formWidth * 0.8f);
        ImGui.inputText("##username", username, ImGuiInputTextFlags.None);
        
        // Add spacing between fields
        ImGui.dummy(0, 10);
        
        // Password field
        ImGui.text("Password:");
        ImGui.setNextItemWidth(formWidth * 0.8f);
        ImGui.inputText("##password", password, ImGuiInputTextFlags.Password);
        
        // Add spacing before buttons
        ImGui.dummy(0, 30);
        
        // Center the signup button
        float buttonWidth = 120;
        ImGui.setCursorPosX((formWidth - buttonWidth) * 0.5f);
        if (ImGui.button("Sign Up", buttonWidth, 40)) {
            performSignup();
        }
        
        // Display status message in color
        if (!statusMessage.isEmpty()) {
            ImGui.spacing();
            float msgWidth = ImGui.calcTextSize(statusMessage).x;
            ImGui.setCursorPosX((formWidth - msgWidth) * 0.5f);
            
            if (isSuccess) {
                ImGui.textColored(0.2f, 0.8f, 0.2f, 1.0f, statusMessage);
            } else {
                ImGui.textColored(0.8f, 0.2f, 0.2f, 1.0f, statusMessage);
            }
        }
        
        // Back button at bottom
        ImGui.setCursorPos(10, formHeight - 40);
        if (ImGui.button("Back", 80, 30)) {
            app.setCurrentScreen(new TitleScreen(app));
        }
        
        ImGui.end();
    }

    private void performSignup() {
        if (email.get().isEmpty() || username.get().isEmpty() || password.get().isEmpty()) {
            statusMessage = "Please fill all fields!";
            isSuccess = false;
            return;
        }

        try {
            // Check if username already exists
            Document existingUser = usersCollection.find(new Document("username", username.get())).first();
            if (existingUser != null) {
                statusMessage = "Username already exists!";
                isSuccess = false;
                return;
            }

            Document newUser = new Document()
                .append("email", email.get())
                .append("username", username.get())
                .append("password", password.get())
                .append("levels", new Document()); // Empty levels

            usersCollection.insertOne(newUser);
            statusMessage = "User created successfully!";
            isSuccess = true;
            
            // Return to login after delay
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    app.setCurrentScreen(new LoginScreen(app));
                } catch (InterruptedException ignored) {}
            }).start();
        } catch (Exception e) {
            statusMessage = "Error creating user!";
            isSuccess = false;
            e.printStackTrace();
        }
    }

    public void cleanup() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        // ImGui handles input automatically
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        // ImGui handles hover states automatically
    }

    @Override
    public void handleMouseRelease(double x, double y) {
        // Not used with ImGui
    }
    
    @Override
    public void handleKeyPress(int key, int action) {
        // ImGui handles keyboard input automatically
    }
}