package thegame.screens;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL11.*;
import com.google.gson.Gson;

import thegame.App;
import thegame.Screen;
import thegame.onScreenObjects.Button;
import thegame.utils.LevelConfig;

public class LevelSelectScreen implements Screen {
    private final App app;
    private final List<Button> levelButtons;
    private final Button returnButton;
    private final Button onlineLevelsButton;

    public LevelSelectScreen(App app) {
        this.app = app;
        this.levelButtons = new ArrayList<>();

        // Button dimensions and positioning
        float buttonWidth = 0.8f;
        float buttonHeight = 0.15f;
        float centerX = -buttonWidth / 2;
        float startY = 0.4f;
        float spacing = 0.3f;

        // Create buttons for all three levels
        for (int i = 1; i <= 3; i++) {
            final int level = i;
            String levelPath = "/levels/level" + i + ".json";
            try (InputStream inputStream = getClass().getResourceAsStream(levelPath)) {
                if (inputStream != null) {
                    String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    final LevelConfig levelConfig = new Gson().fromJson(jsonContent, LevelConfig.class);
                    
                    Button levelButton = new Button(
                        app,
                        "Level " + i,
                        centerX,
                        startY - (i-1) * spacing,
                        buttonWidth,
                        buttonHeight,
                        () -> app.setCurrentScreen(new GameScreen(app, levelConfig))
                    );
                    levelButtons.add(levelButton);
                }
            } catch (IOException e) {
                System.err.println("Error loading level " + i + ": " + e.getMessage());
            }
        }

        // Create return button
        returnButton = new Button(
            app,
            "Return to Title",
            centerX,
            -0.8f,
            buttonWidth,
            buttonHeight,
            () -> app.setCurrentScreen(new TitleScreen(app))
        );

        // Create online levels button
        onlineLevelsButton = new Button(
            app,
            "Online Levels",
            -0.9f,
            -0.8f,
            0.4f,
            buttonHeight,
            () -> app.setCurrentScreen(new OnlineLevelSelectScreen(app))
        );
    }

    @Override
    public void render() {
        glClearColor(0.8f, 0.9f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        for (Button button : levelButtons) {
            button.render();
        }
        returnButton.render();
        onlineLevelsButton.render();
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        if (returnButton.contains((float)mouseX, (float)mouseY)) {
            returnButton.click();
            return;
        }

        if (onlineLevelsButton.contains((float)mouseX, (float)mouseY)) {
            onlineLevelsButton.click();
            return;
        }

        for (Button button : levelButtons) {
            if (button.contains((float)mouseX, (float)mouseY)) {
                button.click();
                return;
            }
        }
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        returnButton.setHovered(returnButton.contains((float)mouseX, (float)mouseY));
        onlineLevelsButton.setHovered(onlineLevelsButton.contains((float)mouseX, (float)mouseY));
        
        for (Button button : levelButtons) {
            button.setHovered(button.contains((float)mouseX, (float)mouseY));
        }
    }

    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // Not needed for this screen
    }
}