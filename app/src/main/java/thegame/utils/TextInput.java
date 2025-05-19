package thegame.utils;

import static org.lwjgl.opengl.GL11.*;

/**
 * A text input field UI component.
 * Supports regular text and password field modes.
 */
public class TextInput {
    private float x, y, width, height;
    private StringBuilder text = new StringBuilder();
    private boolean isFocused = false;
    private boolean isPasswordMode = false;
    private long lastBlinkTime = System.currentTimeMillis();
    private boolean showCursor = true;
    
    /**
     * Creates a new text input field.
     *
     * @param x The x-coordinate of the input field
     * @param y The y-coordinate of the input field
     * @param width The width of the input field
     * @param height The height of the input field
     */
    public TextInput(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Renders the text input field.
     *
     * @param fontRenderer The font renderer to use for text rendering
     * @param mouseX The current mouse x-coordinate
     * @param mouseY The current mouse y-coordinate
     */
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
    
    /**
     * Handles mouse clicks on the input field.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     */
    public void handleMouseClick(float mouseX, float mouseY) {
        boolean wasInside = isInside(mouseX, mouseY);
        
        // Clear focus from all text inputs when clicking elsewhere
        if (!wasInside) {
            isFocused = false;
        } else {
            isFocused = true;
        }
    }
    
    /**
     * Handles character input for the text field.
     *
     * @param c The character to add
     */
    public void handleCharInput(char c) {
        if (isFocused && isValidChar(c)) {
            text.append(c);
        }
    }
    
    /**
     * Handles backspace key for text deletion.
     */
    public void handleBackspace() {
        if (isFocused && text.length() > 0) {
            text.deleteCharAt(text.length() - 1);
        }
    }
    
    /**
     * Checks if a character is valid for input.
     *
     * @param c The character to check
     * @return true if the character is valid, false otherwise
     */
    private boolean isValidChar(char c) {
        return c >= 32 && c < 127; // Printable ASCII
    }
    
    /**
     * Checks if the mouse is inside the input field.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @return true if the mouse is inside the field, false otherwise
     */
    private boolean isInside(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && 
               mouseY >= y && mouseY <= y + height;
    }
    
    /**
     * Sets the focus state of the input field.
     *
     * @param focused true to set focus, false to remove focus
     */
    public void setFocused(boolean focused) {
        this.isFocused = focused;
    }
    
    /**
     * Checks if the input field has focus.
     *
     * @return true if the field is focused, false otherwise
     */
    public boolean isFocused() {
        return isFocused;
    }
    
    /**
     * Sets password mode for the input field.
     *
     * @param passwordMode true to enable password mode (displaying dots), false for normal text
     */
    public void setPasswordMode(boolean passwordMode) {
        this.isPasswordMode = passwordMode;
    }
    
    /**
     * Gets the current text value.
     *
     * @return The text content of the field
     */
    public String getText() {
        return text.toString();
    }
    
    /**
     * Sets the text value.
     *
     * @param text The text to set
     */
    public void setText(String text) {
        this.text = new StringBuilder(text);
    }
}
