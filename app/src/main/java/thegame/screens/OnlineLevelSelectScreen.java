package thegame.screens;

import thegame.App;
import thegame.Screen;
import thegame.onScreenObjects.Button;
import thegame.utils.MongoDBConnection;
import thegame.utils.LevelConfig;
import com.google.gson.Gson;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL11.*;

public class OnlineLevelSelectScreen implements Screen {
    private final App app;
    private final List<Button> levelButtons;
    private final Button returnButton;

    public OnlineLevelSelectScreen(App app) {
        this.app = app;
        this.levelButtons = new ArrayList<>();

        // Button dimensions
        float buttonWidth = 0.8f;
        float buttonHeight = 0.15f;
        float centerX = -buttonWidth / 2;
        float startY = 0.4f;
        float spacing = 0.3f;

        // Load levels from MongoDB
        try (MongoDBConnection mongodb = new MongoDBConnection()) {
            List<Document> levels = mongodb.getLevels();
            int i = 0;
            for (Document level : levels) {
                final LevelConfig levelConfig = parseLevelFromDocument(level);
                if (levelConfig != null) {
                    Button levelButton = new Button(
                        app,
                        "Online Level " + (i + 1),
                        centerX,
                        startY - i * spacing,
                        buttonWidth,
                        buttonHeight,
                        () -> app.setCurrentScreen(new GameScreen(app, levelConfig))
                    );
                    levelButtons.add(levelButton);
                    i++;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load online levels: " + e.getMessage());
            e.printStackTrace();
        }

        // Create return button
        returnButton = new Button(
            app,
            "Back to Level Select",
            -0.2f,
            -0.8f,
            0.4f,
            buttonHeight,
            () -> app.setCurrentScreen(new LevelSelectScreen(app))
        );
    }

    private LevelConfig parseLevelFromDocument(Document doc) {
        try {
            String jsonContent = doc.toJson();
            System.out.println("Parsing level document: " + jsonContent); // Debug output
            return new Gson().fromJson(jsonContent, LevelConfig.class);
        } catch (Exception e) {
            System.err.println("Error parsing level document: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void render() {
        glClearColor(0.8f, 0.9f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        for (Button button : levelButtons) {
            button.render();
        }
        returnButton.render();
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY) {
        if (returnButton.contains((float) mouseX, (float) mouseY)) {
            returnButton.click();
            return;
        }

        for (Button button : levelButtons) {
            if (button.contains((float) mouseX, (float) mouseY)) {
                button.click();
                return;
            }
        }
    }

    @Override
    public void handleMouseMove(double mouseX, double mouseY) {
        returnButton.setHovered(returnButton.contains((float) mouseX, (float) mouseY));
        for (Button button : levelButtons) {
            button.setHovered(button.contains((float) mouseX, (float) mouseY));
        }
    }
    
    @Override
    public void handleMouseRelease(double mouseX, double mouseY) {
        // No action needed for mouse release in this screen
    }
}