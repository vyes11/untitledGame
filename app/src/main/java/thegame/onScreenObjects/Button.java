package thegame.onScreenObjects;

import thegame.App;
import thegame.utils.FontRenderer; // Ensure this import is correct and the FontRenderer class exists in the specified package
import static org.lwjgl.opengl.GL11.*;

public class Button {
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final String text;
    private boolean isHovered;
    private final App app;
    private final Runnable onClick;

    public Button(App app, String text, float x, float y, float width, float height, Runnable onClick) {
        this.app = app;
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.onClick = onClick;
        
        // Debug print
        System.out.printf("Button created: text=%s, x=%.2f, y=%.2f, width=%.2f, height=%.2f%n",
                         text, x, y, width, height);
    }

    public void render() {
        try {
            // Draw button background
            glBegin(GL_QUADS);
            if (isHovered) {
                glColor3f(0.6f, 0.6f, 0.6f);
            } else {
                glColor3f(0.5f, 0.5f, 0.5f);
            }
            glVertex2f(x, y);
            glVertex2f(x + width, y);
            glVertex2f(x + width, y + height);
            glVertex2f(x, y + height);
            glEnd();

            // Draw button border
            glColor3f(0.3f, 0.3f, 0.3f);
            glBegin(GL_LINE_LOOP);
            glVertex2f(x, y);
            glVertex2f(x + width, y);
            glVertex2f(x + width, y + height);
            glVertex2f(x, y + height);
            glEnd();

            // Render text if FontRenderer is available
            FontRenderer fontRenderer = app.getFontRenderer(); // Ensure getFontRenderer() returns a valid FontRenderer instance
            if (fontRenderer != null) {
                float textScale = 0.003f;
                float textWidth = fontRenderer.getTextWidth(text, textScale);
                float textHeight = fontRenderer.getTextHeight(textScale);
                float textX = x + (width - textWidth) / 2;
                float textY = y + (height - textHeight) / 2;

                glColor3f(0.1f, 0.1f, 0.1f);  // Text color (dark grey)
                fontRenderer.renderText(text, textX, textY, textScale, 0.0f, 0.0f, 0.0f);
            } else {
                System.err.println("FontRenderer is null for button: " + text);
            }

        } catch (Exception e) {
            System.err.println("Error rendering button '" + text + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean contains(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + height;
    }

    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    public void click() {
        if (onClick != null) {
            onClick.run();
        }
    }

    
}