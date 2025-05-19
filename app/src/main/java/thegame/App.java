package thegame;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.opengl.GL30;
import org.bson.Document;
/**
 * The main application class for the game.
 * Handles window creation, input, and game loop.
 */
public class App {
    // Window dimensions
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;

    // Default VAO for resource management
    public static int defaultVao = 0;

    // Window title
    private String windowTitle = "Flauliss Audits";

    // GLFW window handle
    private long window;

    // Current screen
    private Screen currentScreen;

    // Window state
    private boolean fullscreen = false;
    private int windowedWidth = WINDOW_WIDTH;
    private int windowedHeight = WINDOW_HEIGHT;
    private int windowedPosX, windowedPosY;

    // User state
    private boolean loggedIn = false;
    private String username = null;
    private Document userData = null;
    
    // Scaling for different resolutions
    private float widthScale = 1.0f;
    private float heightScale = 1.0f;
    
    // Most recently edited level ID (for verification process)
    private int mostRecentlyEditedLevelId = -1;

    /**
     * Creates a new App instance.
     */
    public App() {
        System.out.println("LWJGL Version: " + org.lwjgl.Version.getVersion());
    }

    /**
     * Initializes the application.
     */
    public void init() {
        // Set up error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, 4); // Anti-aliasing

        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, windowTitle, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Set up input callbacks
        setupInputCallbacks();

        // Get the primary monitor
        long monitor = glfwGetPrimaryMonitor();
        org.lwjgl.glfw.GLFWVidMode vidMode = glfwGetVideoMode(monitor);

        // Center the window
        glfwSetWindowPos(
            window,
            (vidMode.width() - WINDOW_WIDTH) / 2,
            (vidMode.height() - WINDOW_HEIGHT) / 2
        );

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        
        // Enable VSync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
        
        // Create capabilities (must be done after making the context current)
        GL.createCapabilities();
        
        // Initialize default VAO
        defaultVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(defaultVao);
        GL30.glBindVertexArray(0);
        
        // Set up scaling factors
        updateScalingFactors();
        
        // Set initial screen
        setCurrentScreen(new thegame.screens.TitleScreen(this));
    }

    /**
     * Sets up input callbacks for the window.
     */
    private void setupInputCallbacks() {
        // Mouse button callback
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                double[] xpos = new double[1];
                double[] ypos = new double[1];
                glfwGetCursorPos(window, xpos, ypos);
                
                if (currentScreen != null) {
                    currentScreen.handleMouseClick(xpos[0], ypos[0]);
                }
            } else if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_RELEASE) {
                double[] xpos = new double[1];
                double[] ypos = new double[1];
                glfwGetCursorPos(window, xpos, ypos);
                
                if (currentScreen != null) {
                    currentScreen.handleMouseRelease(xpos[0], ypos[0]);
                }
            }
        });
        
        // Mouse position callback
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (currentScreen != null) {
                currentScreen.handleMouseMove(xpos, ypos);
            }
        });
        
        // Key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
                toggleFullscreen();
            }
            
            if (currentScreen != null) {
                currentScreen.handleKeyPress(key, action);
            }
        });
        
        // Character callback
        glfwSetCharCallback(window, (window, codepoint) -> {
            if (currentScreen != null) {
                currentScreen.handleCharInput(codepoint);
            }
        });
    }

    /**
     * Toggles between fullscreen and windowed mode.
     */
    public void toggleFullscreen() {
        if (fullscreen) {
            // Switch to windowed mode with original 800x600 dimensions
            glfwSetWindowMonitor(window, NULL, windowedPosX, windowedPosY, 
                                WINDOW_WIDTH, WINDOW_HEIGHT, GLFW_DONT_CARE);
            fullscreen = false;
        } else {
            // Save windowed mode dimensions
            int[] width = new int[1];
            int[] height = new int[1];
            int[] xpos = new int[1];
            int[] ypos = new int[1];
            
            glfwGetWindowSize(window, width, height);
            glfwGetWindowPos(window, xpos, ypos);
            
            windowedWidth = width[0];
            windowedHeight = height[0];
            windowedPosX = xpos[0];
            windowedPosY = ypos[0];
            
            // Switch to fullscreen
            long monitor = glfwGetPrimaryMonitor();
            org.lwjgl.glfw.GLFWVidMode vidMode = glfwGetVideoMode(monitor);
            
            // Calculate fullscreen dimensions that preserve the 4:3 aspect ratio
            int fullscreenHeight = vidMode.height();
            int fullscreenWidth = (fullscreenHeight * WINDOW_WIDTH) / WINDOW_HEIGHT;
            
            if (fullscreenWidth > vidMode.width()) {
                // If too wide, constrain by width instead
                fullscreenWidth = vidMode.width();
                fullscreenHeight = (fullscreenWidth * WINDOW_HEIGHT) / WINDOW_WIDTH;
            }
            
            // Center on screen
            int xOffset = (vidMode.width() - fullscreenWidth) / 2;
            int yOffset = (vidMode.height() - fullscreenHeight) / 2;
            
            glfwSetWindowMonitor(window, monitor, xOffset, yOffset, 
                                fullscreenWidth, fullscreenHeight, 
                                vidMode.refreshRate());
            fullscreen = true;
        }
        
        // Update scaling factors
        updateScalingFactors();
    }
    
    /**
     * Updates the scaling factors used for UI elements based on window size.
     * This method maintains the original 800x600 positioning.
     */
    private void updateScalingFactors() {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetWindowSize(window, width, height);
        
        // Keep original 800x600 ratio for positioning calculations
        widthScale = (float)width[0] / WINDOW_WIDTH;
        heightScale = (float)height[0] / WINDOW_HEIGHT;
    }
    
    /**
     * Gets the horizontal scale factor.
     *
     * @return The horizontal scale factor
     */
    public float getWidthScale() {
        return widthScale;
    }
    
    /**
     * Gets the vertical scale factor.
     *
     * @return The vertical scale factor
     */
    public float getHeightScale() {
        return heightScale;
    }
    
    /**
     * Converts a normalized x-coordinate to window coordinates.
     *
     * @param normalizedX The normalized x-coordinate (0-1)
     * @return The x-coordinate in window coordinates
     */
    public float denormalizeX(float normalizedX) {
        return normalizedX * WINDOW_WIDTH * widthScale;
    }
    
    /**
     * Converts a normalized y-coordinate to window coordinates.
     *
     * @param normalizedY The normalized y-coordinate (0-1)
     * @return The y-coordinate in window coordinates
     */
    public float denormalizeY(float normalizedY) {
        return normalizedY * WINDOW_HEIGHT * heightScale;
    }

    /**
     * Runs the main game loop.
     */
    public void run() {
        try {
            init();
            loop();
            
            // Release resources
            cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The main game loop.
     */
    private void loop() {
        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        // Enable alpha blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Main loop
        while (!glfwWindowShouldClose(window)) {
            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT);
            
            // Render the current screen
            if (currentScreen != null) {
                currentScreen.render();
            }
            
            // Swap buffers
            glfwSwapBuffers(window);
            
            // Poll for events
            glfwPollEvents();
        }
    }

    /**
     * Cleans up resources.
     */
    private void cleanup() {
        // Clean up default VAO
        if (defaultVao != 0) {
            GL30.glDeleteVertexArrays(defaultVao);
            defaultVao = 0;
        }
        
        // Destroy the window
        glfwDestroyWindow(window);
        
        // Terminate GLFW
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    /**
     * Sets the current screen.
     *
     * @param screen The screen to set as current
     */
    public void setCurrentScreen(Screen screen) {
        this.currentScreen = screen;
    }

    /**
     * Gets the GLFW window handle.
     *
     * @return The window handle
     */
    public long getWindow() {
        return window;
    }

    /**
     * Sets whether the user is logged in.
     *
     * @param loggedIn true if the user is logged in, false otherwise
     */
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    /**
     * Checks if the user is logged in.
     *
     * @return true if the user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * Sets the username of the logged-in user.
     *
     * @param username The username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the username of the logged-in user.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the logged-in user's data.
     *
     * @param userData The user's data
     */
    public void setLoggedInUser(Document userData) {
        this.userData = userData;
        if (userData != null) {
            this.username = userData.getString("username");
            this.loggedIn = true;
        }
    }

    /**
     * Clears the logged-in user's data.
     */
    public void clearLoggedInUser() {
        this.userData = null;
        this.username = null;
        this.loggedIn = false;
    }

    /**
     * Gets the logged-in user's data.
     *
     * @return The user's data
     */
    public Document getUserData() {
        return userData;
    }

    /**
     * Sets the VSync state.
     *
     * @param enabled true to enable VSync, false to disable
     */
    public void setVsync(boolean enabled) {
        glfwSwapInterval(enabled ? 1 : 0);
    }

    /**
     * Sets the anti-aliasing level.
     *
     * @param level The anti-aliasing level (0=off, 2=2x, 4=4x, 8=8x)
     */
    public void setAntiAliasing(int level) {
        // This would typically require recreating the context with new settings
        // For simplicity, we'll just print a message
        System.out.println("Set anti-aliasing to " + level + "x");
    }

    /**
     * Sets the ID of the most recently edited level (for verification).
     *
     * @param levelId The level ID
     */
    public void setMostRecentlyEditedLevelId(int levelId) {
        this.mostRecentlyEditedLevelId = levelId;
    }

    /**
     * Gets the ID of the most recently edited level.
     *
     * @return The level ID
     */
    public int getMostRecentlyEditedLevelId() {
        return mostRecentlyEditedLevelId;
    }

    /**
     * The main entry point for the application.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        new App().run();
    }
}
