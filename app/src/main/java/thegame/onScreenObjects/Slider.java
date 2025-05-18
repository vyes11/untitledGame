package thegame.onScreenObjects;

import static org.lwjgl.opengl.GL11.*;

public class Slider {
    private float x, y, width, height;
    private float value; // Between 0.0 and 1.0
    private boolean isDragging = false;
    
    public Slider(float x, float y, float width, float height, float initialValue) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.value = Math.max(0.0f, Math.min(1.0f, initialValue));
    }
    
    public void render(float mouseX, float mouseY) {
        // Draw background track
        glColor3f(0.2f, 0.2f, 0.2f);
        glBegin(GL_QUADS);
        glVertex2f(x, y + height / 3);
        glVertex2f(x + width, y + height / 3);
        glVertex2f(x + width, y + height * 2 / 3);
        glVertex2f(x, y + height * 2 / 3);
        glEnd();
        
        // Draw filled part of track
        float fillWidth = width * value;
        glColor3f(0.4f, 0.6f, 0.8f);
        glBegin(GL_QUADS);
        glVertex2f(x, y + height / 3);
        glVertex2f(x + fillWidth, y + height / 3);
        glVertex2f(x + fillWidth, y + height * 2 / 3);
        glVertex2f(x, y + height * 2 / 3);
        glEnd();
        
        // Draw handle
        float handleX = x + fillWidth - height / 2;
        float handleWidth = height;
        
        // Check if mouse is over the handle
        boolean isHovered = mouseX >= handleX && mouseX <= handleX + handleWidth &&
                          mouseY >= y && mouseY <= y + height;
        
        // Draw handle with highlight if hovered or being dragged
        if (isHovered || isDragging) {
            glColor3f(0.9f, 0.9f, 1.0f);
        } else {
            glColor3f(0.7f, 0.7f, 0.8f);
        }
        
        glBegin(GL_QUADS);
        glVertex2f(handleX, y);
        glVertex2f(handleX + handleWidth, y);
        glVertex2f(handleX + handleWidth, y + height);
        glVertex2f(handleX, y + height);
        glEnd();
        
        // Draw handle border
        glColor3f(0.4f, 0.4f, 0.5f);
        glLineWidth(1.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(handleX, y);
        glVertex2f(handleX + handleWidth, y);
        glVertex2f(handleX + handleWidth, y + height);
        glVertex2f(handleX, y + height);
        glEnd();
    }
    
    public boolean handleMouseClick(float mouseX, float mouseY) {
        // Calculate handle position
        float handleX = x + width * value - height / 2;
        float handleWidth = height;
        
        // Check if click is on the handle
        if (mouseX >= handleX && mouseX <= handleX + handleWidth &&
            mouseY >= y && mouseY <= y + height) {
            isDragging = true;
            return true;
        }
        
        // Check if click is on the track
        if (mouseX >= x && mouseX <= x + width &&
            mouseY >= y && mouseY <= y + height) {
            // Set the value directly based on click position
            setValue((mouseX - x) / width);
            isDragging = true;
            return true;
        }
        
        return false;
    }
    
    public void handleMouseMove(double mouseX, double mouseY) {
        if (isDragging) {
            setValue((float)((mouseX - x) / width));
        }
    }
    
    public void handleMouseRelease(double mouseX, double mouseY) {
        isDragging = false;
    }
    
    private void setValue(float newValue) {
        // Clamp value between 0 and 1
        value = Math.max(0.0f, Math.min(1.0f, newValue));
    }
    
    public float getValue() {
        return value;
    }
    
    public boolean isDragging() {
        return isDragging;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public float getWidth() {
        return width;
    }
    
    public float getHeight() {
        return height;
    }
}
