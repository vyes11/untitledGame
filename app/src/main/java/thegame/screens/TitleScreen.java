package thegame.screens;

import thegame.App;
import thegame.Screen;
import thegame.onScreenObjects.Button;

import org.joml.Matrix4f;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;

import static org.lwjgl.opengl.GL11.*;

public class TitleScreen implements Screen {
    private final App app;
    private Button loginButton;
    private boolean isHovering = false;
    private int frameCounter = 0;

    public TitleScreen(App app) {
        System.out.println("TitleScreen constructor called!");
        this.app = app;

        // Create a button in the center of the screen
        float buttonWidth = 200;  // Width in pixels
        float buttonHeight = 50;  // Height in pixels
        float buttonX = (App.WINDOW_WIDTH - buttonWidth) / 2;  // Center horizontally
        float buttonY = (App.WINDOW_HEIGHT - buttonHeight) / 2 + 50;  // Center vertically, slightly lower
        
        // Login button (gray)
        loginButton = new Button(buttonX, buttonY, buttonWidth, buttonHeight, 0.5f, 0.5f, 0.5f, "Login");
        
        // Set the projection matrix from App
        Matrix4f projMatrix = new Matrix4f().ortho2D(0, App.WINDOW_WIDTH, App.WINDOW_HEIGHT, 0);
        loginButton.setProjectionMatrix(projMatrix);
    }

    @Override
    public void render() {
        frameCounter++;
        // glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        // glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // // Render OpenGL content
        // loginButton.render();

        // // Ensure OpenGL state is ImGui-friendly
        // glViewport(0, 0, App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
        // glDisable(GL_SCISSOR_TEST);
        // glDisable(GL_STENCIL_TEST);
        // glDisable(GL_DEPTH_TEST);
        // glEnable(GL_BLEND);
        // glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        ImGui.showDemoWindow();
        ImGui.begin("Test Window");
        ImGui.text("Hello from ImGui!");
        ImGui.end();

        // Debug info window
        // ImGui.begin("Debug Info", ImGuiWindowFlags.AlwaysAutoResize);
        // ImGui.text("Frame: " + frameCounter);
        // ImGui.end();
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        System.out.println("Mouse clicked at: " + mouseX + ", " + mouseY);

        if (loginButton.isClicked((float) mouseX, (float) mouseY)) {
            System.out.println("Login button clicked!");
            app.setCurrentScreen(new LoginScreen(app)); // Transition to LoginScreen
        }
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        // Check if mouse is over the button
        boolean wasHovering = isHovering;
        isHovering = loginButton.isClicked((float) mouseX, (float) mouseY);
        
        // If hover state changed, print debug info
        if (wasHovering != isHovering) {
            System.out.println("Button hover state: " + isHovering);
        }
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // No additional action needed
    }

    @Override
    public void handleKeyPress(int key, int action) {
        // No additional action needed
    }
}