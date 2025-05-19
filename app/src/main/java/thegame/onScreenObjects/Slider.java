package thegame.onScreenObjects;

import static org.lwjgl.opengl.GL11.*;

/**
 * A slider UI component that allows users to select a value within a range.
 * Used primarily in the SettingsScreen for adjusting volume and other settings.
 */
public class Slider {
    private float x;
    private float y;
    private float width;
    private float height;
    private float value; // Value between 0 and 1
    private boolean isDragging = false;
    private float knobRadius = 10.0f;
    
    // Colors for the slider components
    private float backgroundR = 0.2f;
    private float backgroundG = 0.2f;
    private float backgroundB = 0.2f;
    
    private float fillR = 0.4f;
    private float fillG = 0.4f;
    private float fillB = 0.7f;
    
    private float knobR = 0.7f;
    private float knobG = 0.7f;
    private float knobB = 0.9f;

    /**
     * Creates a new slider with the specified position, size, and initial value.
     *
     * @param x The x-coordinate of the top-left corner of the slider
     * @param y The y-coordinate of the top-left corner of the slider
     * @param width The width of the slider
     * @param height The height of the slider
     * @param initialValue The initial value of the slider (between 0 and 1)
     */
    public Slider(float x, float y, float width, float height, float initialValue) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.value = Math.max(0.0f, Math.min(1.0f, initialValue)); // Clamp to [0, 1]
    }

    /**
     * Sets the colors for the slider, allowing for customization of its appearance.
     *
     * @param r The red component of the slider's fill color (0-1)
     * @param g The green component of the slider's fill color (0-1)
     * @param b The blue component of the slider's fill color (0-1)
     */
    public void setColors(float r, float g, float b) {
        // Update fill color
        this.fillR = r;
        this.fillG = g;
        this.fillB = b;
        
        // Adjust knob color to be a lighter version of the fill color
        this.knobR = Math.min(1.0f, r + 0.2f);
        this.knobG = Math.min(1.0f, g + 0.2f);
        this.knobB = Math.min(1.0f, b + 0.2f);
    }

    /**
     * Renders the slider at its current position and with its current value.
     *
     * @param mouseX The current mouse x-coordinate
     * @param mouseY The current mouse y-coordinate
     */
    public void render(float mouseX, float mouseY) {
        render(mouseX, mouseY, 1.0f, 1.0f);
    }

    /**
     * Renders the slider with scaling support for different screen resolutions.
     *
     * @param mouseX The current mouse x-coordinate
     * @param mouseY The current mouse y-coordinate
     * @param scaleX The horizontal scale factor
     * @param scaleY The vertical scale factor
     */
    public void render(float mouseX, float mouseY, float scaleX, float scaleY) {
        float scaledX = x * scaleX;
        float scaledY = y * scaleY;
        float scaledWidth = width * scaleX;
        float scaledHeight = height * scaleY;
        float scaledKnobRadius = knobRadius * Math.min(scaleX, scaleY);
        
        // Draw slider background (track)
        glColor3f(backgroundR, backgroundG, backgroundB);
        glBegin(GL_QUADS);
        glVertex2f(scaledX, scaledY);
        glVertex2f(scaledX + scaledWidth, scaledY);
        glVertex2f(scaledX + scaledWidth, scaledY + scaledHeight);
        glVertex2f(scaledX, scaledY + scaledHeight);
        glEnd();
        
        // Draw filled part of slider
        glColor3f(fillR, fillG, fillB);
        glBegin(GL_QUADS);
        glVertex2f(scaledX, scaledY);
        glVertex2f(scaledX + scaledWidth * value, scaledY);
        glVertex2f(scaledX + scaledWidth * value, scaledY + scaledHeight);
        glVertex2f(scaledX, scaledY + scaledHeight);
        glEnd();
        
        // Draw slider knob
        float knobX = scaledX + scaledWidth * value;
        float knobY = scaledY + scaledHeight / 2;
        
        glColor3f(knobR, knobG, knobB);
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(knobX, knobY); // Center
        for (int i = 0; i <= 20; i++) {
            float angle = (float) (i / 20.0 * Math.PI * 2);
            glVertex2f(
                knobX + (float) Math.cos(angle) * scaledKnobRadius,
                knobY + (float) Math.sin(angle) * scaledKnobRadius
            );
        }
        glEnd();
        
        // Draw knob border
        glColor3f(0.0f, 0.0f, 0.0f);
        glLineWidth(1.0f);
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i <= 20; i++) {
            float angle = (float) (i / 20.0 * Math.PI * 2);
            glVertex2f(
                knobX + (float) Math.cos(angle) * scaledKnobRadius,
                knobY + (float) Math.sin(angle) * scaledKnobRadius
            );
        }
        glEnd();
    }

    /**
     * Handles mouse clicks on the slider.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @return true if the slider was clicked, false otherwise
     */
    public boolean handleMouseClick(float mouseX, float mouseY) {
        return handleMouseClick(mouseX, mouseY, 1.0f, 1.0f);
    }

    /**
     * Handles mouse clicks on the slider with scaling support.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @param scaleX The horizontal scale factor
     * @param scaleY The vertical scale factor
     * @return true if the slider was clicked, false otherwise
     */
    public boolean handleMouseClick(float mouseX, float mouseY, float scaleX, float scaleY) {
        float scaledX = x * scaleX;
        float scaledY = y * scaleY;
        float scaledWidth = width * scaleX;
        float scaledHeight = height * scaleY;
        
        if (mouseX >= scaledX && mouseX <= scaledX + scaledWidth &&
            mouseY >= scaledY && mouseY <= scaledY + scaledHeight) {
            
            // Update value based on click position
            value = (mouseX - scaledX) / scaledWidth;
            value = Math.max(0.0f, Math.min(1.0f, value)); // Clamp to [0, 1]
            isDragging = true;
            return true;
        }
        return false;
    }

    /**
     * Handles mouse movement when dragging the slider.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @param scaleX The horizontal scale factor
     * @param scaleY The vertical scale factor
     */
    public void handleMouseMove(double mouseX, double mouseY, float scaleX, float scaleY) {
        if (isDragging) {
            float scaledX = x * scaleX;
            float scaledWidth = width * scaleX;
            
            // Update value based on mouse position
            value = (float) ((mouseX - scaledX) / scaledWidth);
            value = Math.max(0.0f, Math.min(1.0f, value)); // Clamp to [0, 1]
        }
    }

    /**
     * Handles mouse release to stop dragging.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     */
    public void handleMouseRelease(double mouseX, double mouseY) {
        isDragging = false;
    }

    /**
     * Gets the current value of the slider.
     *
     * @return The current value (between 0 and 1)
     */
    public float getValue() {
        return value;
    }

    /**
     * Sets the value of the slider.
     *
     * @param value The new value (will be clamped between 0 and 1)
     */
    public void setValue(float value) {
        this.value = Math.max(0.0f, Math.min(1.0f, value));
    }

    /**
     * Gets the x-coordinate of the slider.
     *
     * @return The x-coordinate
     */
    public float getX() {
        return x;
    }

    /**
     * Gets the y-coordinate of the slider.
     *
     * @return The y-coordinate
     */
    public float getY() {
        return y;
    }

    /**
     * Gets the width of the slider.
     *
     * @return The width
     */
    public float getWidth() {
        return width;
    }

    /**
     * Gets the height of the slider.
     *
     * @return The height
     */
    public float getHeight() {
        return height;
    }

    /**
     * Checks if the slider is currently being dragged.
     *
     * @return true if the slider is being dragged, false otherwise
     */
    public boolean isDragging() {
        return isDragging;
    }
}
