package thegame.screens;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import thegame.App;
import thegame.Screen;
import thegame.onScreenObjects.Button;

public class TitleScreen implements Screen {
    @SuppressWarnings("unused") // Used in button callbacks
    private final App app;
    private final Button playButton;

    public TitleScreen(App app) {
        this.app = app;
        playButton = new Button(app, "Play Game", -0.3f, -0.1f, 0.6f, 0.2f, () -> {
            app.setCurrentScreen(new thegame.screens.LevelSelectScreen(app));
        });
    }

    @Override
    public void render() {
        // Set background color
        glClearColor(0.2f, 0.3f, 0.8f, 1.0f); // Blue background
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Draw title text (we'll just use the button for now since text rendering requires more setup)
        playButton.render();
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        if (playButton.contains((float)mouseX, (float)mouseY)) {
            playButton.click();
        }
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // Not needed for title screen
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        playButton.setHovered(playButton.contains((float)mouseX, (float)mouseY));
    }
}