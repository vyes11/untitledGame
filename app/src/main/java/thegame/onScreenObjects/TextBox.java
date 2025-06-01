package thegame.onScreenObjects;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glVertex2f;

import thegame.utils.FontRenderer;

/**
 * A text input field that can be rendered on screen.
 * Provides user interaction features including text entry, focus handling,
 * and password masking.
 */
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
    
    /**
     * Creates a new text box with the specified position, size, and placeholder text.
     *
     * @param x The x-coordinate of the top-left corner
     * @param y The y-coordinate of the top-left corner
     * @param width The width of the text box
     * @param height The height of the text box
     * @param placeholder The placeholder text to display when the text box is empty
     */
    public TextBox(float x, float y, float width, float height, String placeholder) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.placeholder = placeholder;
        
        // Create shared FontRenderer if it doesn't exist
        if (fontRenderer == null) {
            fontRenderer = new FontRenderer();
            fontRenderer.loadFont("/fonts/pf_tempesta_seven_bold.ttf");
        }
    }
    
    /**
     * Renders the text box with appropriate styling based on its state (focused, hovered).
     * Also handles cursor blinking when the text box is focused.
     *
     * @param mouseX The current x-coordinate of the mouse
     * @param mouseY The current y-coordinate of the mouse
     */
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
    
    /**
     * Handles mouse click events on the text box.
     * Sets focus to the text box if clicked within its bounds.
     *
     * @param mouseX The x-coordinate of the mouse click
     * @param mouseY The y-coordinate of the mouse click
     * @return true if the click was within the text box bounds, false otherwise
     */
    public boolean handleMouseClick(float mouseX, float mouseY) {
        boolean wasClicked = mouseX >= x && mouseX <= x + width &&
                           mouseY >= y && mouseY <= y + height;
        
        isFocused = wasClicked;
        return wasClicked;
    }
    
    /**
     * Handles keyboard key press events, such as backspace for text deletion.
     * Only processes events when the text box is focused.
     *
     * @param key The key code of the pressed key
     * @param action The action (press, release, etc.)
     */
    public void handleKeyPress(int key, int action) {
        if (!isFocused || action != GLFW_PRESS && action != GLFW_REPEAT) {
            return;
        }
        
        if (key == GLFW_KEY_BACKSPACE && !text.isEmpty()) {
            text = text.substring(0, text.length() - 1);
        }
    }
    
    /**
     * Handles character input events for text entry.
     * Only processes input when the text box is focused.
     *
     * @param codepoint The Unicode code point of the character
     */
    public void handleCharInput(int codepoint) {
        if (!isFocused) {
            return;
        }
        
        // Only append printable ASCII characters
        if (codepoint >= 32 && codepoint <= 126) {
            text += (char)codepoint;
        }
    }
    
    /**
     * Gets the current text content of the text box.
     *
     * @return The current text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Sets the text content of the text box.
     *
     * @param text The new text content
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Checks if the text box currently has focus.
     *
     * @return true if the text box is focused, false otherwise
     */
    public boolean isFocused() {
        return isFocused;
    }
    
    /**
     * Sets the focus state of the text box.
     *
     * @param focused true to give focus to the text box, false to remove focus
     */
    public void setFocused(boolean focused) {
        this.isFocused = focused;
    }
    
    /**
     * Sets whether the text box should mask input (for passwords).
     * When in password mode, text is displayed as asterisks.
     *
     * @param passwordMode true to enable password masking, false to show plain text
     */
    public void setPasswordMode(boolean passwordMode) {
        this.isPasswordMode = passwordMode;
    }
    
    /**
     * Gets the x-coordinate of the text box.
     *
     * @return The x-coordinate
     */
    public float getX() {
        return x;
    }
    
    /**
     * Gets the y-coordinate of the text box.
     *
     * @return The y-coordinate
     */
    public float getY() {
        return y;
    }
    
    /**
     * Gets the width of the text box.
     *
     * @return The width
     */
    public float getWidth() {
        return width;
    }
    
    /**
     * Gets the height of the text box.
     *
     * @return The height
     */
    public float getHeight() {
        return height;
    }
}
