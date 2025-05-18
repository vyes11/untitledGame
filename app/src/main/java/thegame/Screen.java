package thegame;

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
    
    // Required methods
    void render();
    void handleMouseClick(double mouseX, double mouseY); // Return true if click was handled
    void handleMouseRelease(double mouseX, double mouseY);
    void handleMouseMove(double mouseX, double mouseY);
    void handleKeyPress(int key, int action);
    void handleCharInput(int codepoint); // Add this method for character input
    
}