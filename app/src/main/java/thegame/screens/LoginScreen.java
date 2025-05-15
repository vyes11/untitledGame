package thegame.screens;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.bson.Document;
import thegame.App;
import thegame.Screen;
import thegame.utils.ImGuiUtils;

public class LoginScreen implements Screen {
    private final App app;
    private final ImString username = new ImString(256);
    private final ImString password = new ImString(256);
    private String statusMessage = "";

    // MongoDB
    private MongoClient mongoClient;
    private MongoCollection<Document> usersCollection;

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
    }

    @Override
    public void render() {
        // Create a centered modal window
        float windowWidth = ImGui.getIO().getDisplaySizeX() * 0.5f;
        float windowHeight = ImGui.getIO().getDisplaySizeY() * 0.6f;
        
        ImGui.setNextWindowPos(
            ImGui.getIO().getDisplaySizeX() * 0.5f - windowWidth * 0.5f,
            ImGui.getIO().getDisplaySizeY() * 0.5f - windowHeight * 0.5f
        );
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        
        ImGui.begin("Login Screen", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse);

        // Center the title text
        float titleWidth = ImGui.calcTextSize("Login to Your Account").x;
        ImGui.setCursorPosX((windowWidth - titleWidth) * 0.5f);
        ImGui.textColored(0.2f, 0.6f, 1.0f, 1.0f, "Login to Your Account");
        ImGui.spacing();
        ImGui.separator();
        ImGui.spacing();

        // Add spacing for better layout
        ImGui.dummy(0, 20);
        
        // Username field
        ImGui.text("Username:");
        ImGui.setNextItemWidth(windowWidth * 0.8f);
        ImGui.inputText("##username", username, ImGuiInputTextFlags.None);
        
        // Add spacing between fields
        ImGui.dummy(0, 10);
        
        // Password field
        ImGui.text("Password:");
        ImGui.setNextItemWidth(windowWidth * 0.8f);
        ImGui.inputText("##password", password, ImGuiInputTextFlags.Password);
        
        // Add spacing before buttons
        ImGui.dummy(0, 30);
        
        // Center the login button
        float buttonWidth = 120;
        ImGui.setCursorPosX((windowWidth - buttonWidth) * 0.5f);
        if (ImGui.button("Login", buttonWidth, 40)) {
            performLogin();
        }
        
        // Display status message in color
        if (!statusMessage.isEmpty()) {
            ImGui.spacing();
            float msgWidth = ImGui.calcTextSize(statusMessage).x;
            ImGui.setCursorPosX((windowWidth - msgWidth) * 0.5f);
            ImGui.textColored(1.0f, 0.3f, 0.3f, 1.0f, statusMessage);
        }
        
        // Back button at bottom
        ImGui.setCursorPos(10, windowHeight - 40);
        if (ImGui.button("Back", 80, 30)) {
            app.setCurrentScreen(new TitleScreen(app));
        }
        
        ImGui.end();
    }

    private void performLogin() {
        if (username.get().isEmpty() || password.get().isEmpty()) {
            statusMessage = "Please fill all fields!";
            return;
        }

        try {
            Document user = usersCollection.find(
                new Document("username", username.get()).append("password", password.get())
            ).first();

            if (user != null) {
                statusMessage = "Login successful!";
                app.setLoggedIn(true);
                // Proceed to main menu after delay
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        app.setCurrentScreen(new TitleScreen(app));
                    } catch (InterruptedException ignored) {}
                }).start();
            } else {
                statusMessage = "Invalid credentials!";
            }
        } catch (Exception e) {
            statusMessage = "Database error!";
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
    public void handleMouseRelease(double mouseX, double mouseY) {
        // ImGui handles input automatically
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        // ImGui handles input automatically
    }

    @Override
    public void handleKeyPress(int key, int action) {
        // ImGui handles input automatically
    }
}