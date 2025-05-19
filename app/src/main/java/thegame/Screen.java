package thegame;

/**
 * Interface for game screens.
 * All screen implementations must provide these methods to handle rendering and input.
 */
public interface Screen {
    // Screen ratio constants
    float ASPECT_RATIO = 16.0f / 9.0f;
    float BASE_WIDTH = 2.0f;  // OpenGL coordinates from -1 to 1
    float BASE_HEIGHT = BASE_WIDTH / ASPECT_RATIO;
    
    
    // Convert screen coordinates to OpenGL coordinates
    default float toGLX(float screenX) {
        return (screenX * 2.0f) - 1.0f;
    }
    
    default float toGLY(float screenY) {
        return 1.0f - (screenY * 2.0f);
    }
    
    /**
     * Renders the screen.
     * Called once per frame.
     */
    void render();
    
    /**
     * Handles mouse click events.
     *
     * @param mouseX The x-coordinate of the mouse
     * @param mouseY The y-coordinate of the mouse
     */
    void handleMouseClick(double mouseX, double mouseY);
    
    /**
     * Handles mouse movement events.
     *
     * @param mouseX The x-coordinate of the mouse
     * @param mouseY The y-coordinate of the mouse
     */
    void handleMouseMove(double mouseX, double mouseY);
    
    /**
     * Handles mouse release events.
     *
     * @param mouseX The x-coordinate of the mouse
     * @param mouseY The y-coordinate of the mouse
     */
    void handleMouseRelease(double mouseX, double mouseY);
    
    /**
     * Handles key press events.
     *
     * @param key The key code
     * @param action The action (GLFW_PRESS, GLFW_RELEASE, GLFW_REPEAT)
     */
    void handleKeyPress(int key, int action);
    
    /**
     * Handles character input events.
     *
     * @param codepoint The Unicode code point of the character
     */
    void handleCharInput(int codepoint);
}