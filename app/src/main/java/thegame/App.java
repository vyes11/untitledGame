package thegame;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.opengl.GL30;
import org.bson.Document;

import thegame.utils.GLDebugger;

public class App {
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    public static int defaultVao = 0;

    private long window;
    private boolean lastMousePressed = false;
    private Screen currentScreen;
    private boolean loggedIn = false; // Track login state
    
    // User data cache
    private Document userData = null;
    private String username = null;

    public void run() {
        System.out.println("LWJGL " + org.lwjgl.Version.getVersion());
        init();
        loop();
        cleanup();
    }

    private void init() {
        // Set up an error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure OpenGL core profile for compatibility
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
        // Use compatibility profile instead of core for more reliable immediate mode
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE); // Use compatibility profile

        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Modern OpenGL", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable v-sync

        // Show the window
        glfwShowWindow(window);

        // Initialize OpenGL capabilities
        GL.createCapabilities();
        
        // Enable OpenGL debugging
        GLDebugger.setDebugMode(true);
        GLDebugger.clearErrors();
        System.out.println("OpenGL Debugging enabled");

        // Set the initial screen (TitleScreen)
        setCurrentScreen(new thegame.screens.TitleScreen(this));
        
        App.defaultVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(defaultVao);
        GLDebugger.checkError("After default VAO creation");

        // Set up input callbacks
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                double[] xpos = new double[1];
                double[] ypos = new double[1];
                glfwGetCursorPos(window, xpos, ypos);
                
                if (action == GLFW_PRESS) {
                    if (currentScreen != null) {
                        currentScreen.handleMouseClick(xpos[0], ypos[0]);
                    }
                } else if (action == GLFW_RELEASE) {
                    if (currentScreen != null) {
                        currentScreen.handleMouseRelease(xpos[0], ypos[0]);
                    }
                }
            }
        });
        
        // Add key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (currentScreen != null) {
                currentScreen.handleKeyPress(key, action);
            }
        });
        
        // Add character callback for text input
        glfwSetCharCallback(window, (window, codepoint) -> {
            if (currentScreen != null) {
                currentScreen.handleCharInput(codepoint);
            }
        });
    }

    public void setCurrentScreen(Screen screen) {
        try {
            // Clean up the previous screen if it implements AutoCloseable
            if (currentScreen instanceof AutoCloseable) {
                try {
                    ((AutoCloseable)currentScreen).close();
                } catch (Exception e) {
                    System.err.println("Error closing previous screen: " + e.getMessage());
                }
            }
            
            // Before switching screens, ensure we have a valid OpenGL context
            if (GL.getCapabilities() == null || !GL.getCapabilities().OpenGL11) {
                System.err.println("WARNING: OpenGL context is not valid before screen switch!");
                // Try to restore context if lost
                glfwMakeContextCurrent(window);
                GL.createCapabilities();
            }
            
            this.currentScreen = screen;
            
            // Log screen transition for debugging
            System.out.println("Screen changed to: " + screen.getClass().getSimpleName());
        } catch (Exception e) {
            System.err.println("Error changing screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            try {
                // Poll for window events
                glfwPollEvents();
                
                // Check context before rendering - more robust checking
                boolean contextValid = GL.getCapabilities() != null && 
                                     GL.getCapabilities().OpenGL11;
                
                if (!contextValid) {
                    System.err.println("WARNING: Lost OpenGL context, attempting to restore...");
                    glfwMakeContextCurrent(window);
                    GL.createCapabilities();
                    
                    if (GL.getCapabilities() == null) {
                        System.err.println("CRITICAL: Failed to restore OpenGL context!");
                        continue; // Skip this frame, try again next time
                    }
                    System.out.println("OpenGL context restored successfully");
                }

                // Clear the screen
                GL11.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                
                // Enable alpha blending for transparent elements
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                // Render current screen inside try-catch to prevent crashing
                if (currentScreen != null) {
                    try {
                        currentScreen.render();
                    } catch (Exception e) {
                        System.err.println("FATAL ERROR in screen rendering: " + e.getMessage());
                        e.printStackTrace();
                        
                        // Try to recover by reverting to title screen
                        if (!(currentScreen instanceof thegame.screens.TitleScreen)) {
                            System.out.println("Attempting recovery by returning to title screen");
                            setCurrentScreen(new thegame.screens.TitleScreen(this));
                        }
                    }
                }
                
                GL11.glDisable(GL11.GL_BLEND);
                
                // Check for OpenGL errors after rendering
                int errorCode = GL11.glGetError();
                if (errorCode != GL11.GL_NO_ERROR) {
                    System.err.println("OpenGL error after rendering: " + errorCode);
                }

                // Swap buffers
                glfwSwapBuffers(window);
            } catch (Exception e) {
                System.err.println("Error in main loop: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void cleanup() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public long getWindow() {
        return window;
    }

    // Add methods to manage user data
    public void setLoggedInUser(Document userData) {
        this.userData = userData;
        this.username = userData.getString("username");
        this.loggedIn = true;
        System.out.println("User logged in: " + username);
    }
    
    public void clearLoggedInUser() {
        this.userData = null;
        this.username = null;
        this.loggedIn = false;
        System.out.println("User logged out");
    }
    
    public Document getUserData() {
        return userData;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setLoggedIn(boolean loggedIn) {
        if (!loggedIn) {
            clearLoggedInUser();
        } else if (this.userData == null) {
            // If setting to logged in but no user data, create minimal data
            this.loggedIn = true;
            System.out.println("Warning: Setting logged in without user data");
        }
    }
    
    public boolean isLoggedIn() {
        return loggedIn;
    }

    public static void main(String[] args) {
        new App().run();
    }
}
