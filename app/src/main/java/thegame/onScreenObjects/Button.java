package thegame.onScreenObjects;

import thegame.App;
import thegame.utils.FontRenderer; // Ensure this import is correct and the FontRenderer class exists in the specified package
import static org.lwjgl.opengl.GL11.*;

public class Button {
    // Reduce button text scale
    private static final float BUTTON_TEXT_SCALE = 0.004f;

    private final App app;
    private String text;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final Runnable onClick;
    private boolean isHovered;

    public Button(App app, String text, float x, float y, float width, float height, Runnable onClick) {
        this.app = app;
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onClick = onClick;
        this.isHovered = false;
        
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void render() {
        // Draw button background
        glBegin(GL_QUADS);
        if (isHovered) {
            glColor3f(0.4f, 0.4f, 0.5f); // Lighter when hovered
        } else {
            glColor3f(0.3f, 0.3f, 0.4f); // Normal state
        }
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();

        // Draw button border
        glColor3f(0.5f, 0.5f, 0.6f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();

        // Draw button hitbox for debugging 
        glColor3f(1, 0, 0); // Red debug box
        glBegin(GL_LINE_LOOP);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();

        // Center text in button
        float textWidth = text.length() * BUTTON_TEXT_SCALE; // Adjust text positioning for new scale
        float textX = x + (width - textWidth) / 2;
        float textY = y + (height - 0.008f) / 2;
        
        app.getFontRenderer().renderText(text, textX, textY, BUTTON_TEXT_SCALE);
    }

    public boolean contains(float mouseX, float mouseY) {
        // Remove aspect ratio scaling from mouseX
        float aspectRatio = (float) App.WINDOW_WIDTH / App.WINDOW_HEIGHT;
        float correctedX = mouseX / aspectRatio;  // Undo glOrtho stretch

        // Check bounds (using original button coordinates)
        return correctedX >= this.x && correctedX <= this.x + width &&
            mouseY >= this.y && mouseY <= this.y + height;
    }

    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    public boolean isHovered() {
        return isHovered;
    }

    public void click() {
        if (onClick != null) {
            onClick.run();
        }
    }
}