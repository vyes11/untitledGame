package thegame.utils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;

public class TextInput {
    private float x, y, width, height;
    private StringBuilder text = new StringBuilder();
    private boolean isFocused = false;
    private boolean isPasswordMode = false;
    private long lastBlinkTime = System.currentTimeMillis();
    private boolean showCursor = true;
    
    public TextInput(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public void render(FontRenderer fontRenderer, float mouseX, float mouseY) {
        // Update cursor blink
        if (System.currentTimeMillis() - lastBlinkTime > 500) {
            showCursor = !showCursor;
            lastBlinkTime = System.currentTimeMillis();
        }
        
        // Draw background
        float borderColor = isFocused ? 0.5f : 0.3f;
        
        // Draw border
        glColor3f(borderColor, borderColor, borderColor);
        glLineWidth(2.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();
        glLineWidth(1.0f);
        
        // Draw background
        glColor3f(0.1f, 0.1f, 0.15f);
        glBegin(GL_QUADS);
        glVertex2f(x + 2, y + 2);
        glVertex2f(x + width - 2, y + 2);
        glVertex2f(x + width - 2, y + height - 2);
        glVertex2f(x + 2, y + height - 2);
        glEnd();
        
        // Draw text
        String displayText = isPasswordMode ? "•".repeat(text.length()) : text.toString();
        float textY = y + (height - fontRenderer.getTextHeight(1.0f)) / 2;
        fontRenderer.renderText(displayText, x + 10, textY, 1.0f);
        
        // Draw cursor
        if (isFocused && showCursor) {
            float cursorX = x + 10 + fontRenderer.getTextWidth(displayText, 1.0f);
            glColor3f(1.0f, 1.0f, 1.0f);
            glLineWidth(2.0f);
            glBegin(GL_LINES);
            glVertex2f(cursorX, y + 5);
            glVertex2f(cursorX, y + height - 5);
            glEnd();
            glLineWidth(1.0f);
        }
    }
    
    public void handleMouseClick(float mouseX, float mouseY) {
        boolean wasInside = isInside(mouseX, mouseY);
        
        // Clear focus from all text inputs when clicking elsewhere
        if (!wasInside) {
            isFocused = false;
        } else {
            isFocused = true;
        }
    }
    
    public void handleCharInput(char c) {
        if (isFocused && isValidChar(c)) {
            text.append(c);
        }
    }
    
    public void handleBackspace() {
        if (isFocused && text.length() > 0) {
            text.deleteCharAt(text.length() - 1);
        }
    }
    
    private boolean isValidChar(char c) {
        return c >= 32 && c < 127; // Printable ASCII
    }
    
    private boolean isInside(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && 
               mouseY >= y && mouseY <= y + height;
    }
    
    public void setFocused(boolean focused) {
        this.isFocused = focused;
    }
    
    public boolean isFocused() {
        return isFocused;
    }
    
    public void setPasswordMode(boolean passwordMode) {
        this.isPasswordMode = passwordMode;
    }
    
    public String getText() {
        return text.toString();
    }
    
    public void setText(String text) {
        this.text = new StringBuilder(text);
    }
}
