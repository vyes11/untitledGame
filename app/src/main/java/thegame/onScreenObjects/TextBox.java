package thegame.onScreenObjects;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;

import thegame.utils.FontRenderer;

public class TextBox {
    private float x, y, width, height;
    private String text = "";
    private String placeholder;
    private boolean isFocused = false;
    private boolean isPasswordMode = false;
    private static FontRenderer fontRenderer;
    
    // Cursor blinking
    private long lastBlinkTime = System.currentTimeMillis();
    private boolean showCursor = true;
    private static final int BLINK_RATE = 500; // in milliseconds
    
    public TextBox(float x, float y, float width, float height, String placeholder) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.placeholder = placeholder;
        
        // Create shared FontRenderer if it doesn't exist
        if (fontRenderer == null) {
            fontRenderer = new FontRenderer();
            fontRenderer.loadFont("F:/temp/theGame/untitledGame/app/src/main/resources/fonts/pf_tempesta_seven_bold.ttf");
        }
    }
    
    public void render(float mouseX, float mouseY) {
        // Update cursor blink
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBlinkTime > BLINK_RATE) {
            showCursor = !showCursor;
            lastBlinkTime = currentTime;
        }
        
        // Check if mouse is over the textbox
        boolean isHovered = mouseX >= x && mouseX <= x + width &&
                          mouseY >= y && mouseY <= y + height;
        
        // Draw background
        if (isFocused) {
            glColor3f(0.2f, 0.2f, 0.3f);
        } else if (isHovered) {
            glColor3f(0.15f, 0.15f, 0.2f);
        } else {
            glColor3f(0.1f, 0.1f, 0.15f);
        }
        
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();
        
        // Draw border
        glColor3f(0.5f, 0.5f, 0.6f);
        glLineWidth(1.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();
        
        // Draw text or placeholder
        String displayText;
        if (text.isEmpty()) {
            displayText = placeholder;
            glColor3f(0.5f, 0.5f, 0.5f); // Gray for placeholder
        } else {
            if (isPasswordMode) {
                displayText = "*".repeat(text.length());
            } else {
                displayText = text;
            }
            glColor3f(0.9f, 0.9f, 0.9f); // White for actual text
        }
        
        float textX = x + 10; // Padding
        float textY = y + (height - fontRenderer.getTextHeight(1.0f)) / 2; // Center vertically
        
        fontRenderer.renderText(displayText, textX, textY, 1.0f);
        
        // Draw cursor if focused
        if (isFocused && showCursor) {
            String visibleText = isPasswordMode ? "*".repeat(text.length()) : text;
            float cursorX = textX + (text.isEmpty() ? 0 : fontRenderer.getTextWidth(visibleText, 1.0f));
            
            glColor3f(0.9f, 0.9f, 0.9f);
            glLineWidth(2.0f);
            glBegin(GL_LINES);
            glVertex2f(cursorX, y + 5);
            glVertex2f(cursorX, y + height - 5);
            glEnd();
        }
    }
    
    public boolean handleMouseClick(float mouseX, float mouseY) {
        boolean wasClicked = mouseX >= x && mouseX <= x + width &&
                           mouseY >= y && mouseY <= y + height;
        
        isFocused = wasClicked;
        return wasClicked;
    }
    
    public void handleKeyPress(int key, int action) {
        if (!isFocused || action != GLFW_PRESS && action != GLFW_REPEAT) {
            return;
        }
        
        if (key == GLFW_KEY_BACKSPACE && !text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
        }
    }
    
    public void handleCharInput(int codepoint) {
        if (!isFocused) {
            return;
        }
        
        // Only append printable ASCII characters
        if (codepoint >= 32 && codepoint <= 126) {
            text += (char)codepoint;
        }
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public boolean isFocused() {
        return isFocused;
    }
    
    public void setFocused(boolean focused) {
        this.isFocused = focused;
    }
    
    public void setPasswordMode(boolean passwordMode) {
        this.isPasswordMode = passwordMode;
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
