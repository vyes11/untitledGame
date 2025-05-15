package thegame;

import org.lwjgl.Version;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT;
import static org.lwjgl.opengl.GL43.glDebugMessageCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import static org.lwjgl.system.MemoryUtil.NULL;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import thegame.screens.TitleScreen;
import thegame.utils.FontRenderer;
import thegame.utils.MongoDBConnection;

public class App {
    public static final int WINDOW_HEIGHT = 800;
    public static final int WINDOW_WIDTH = 1422; // 16:9 ratio


    private static final float ASPECT_RATIO = (float) WINDOW_WIDTH / WINDOW_HEIGHT;

    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private long window;
    private GLFWErrorCallback errorCallback;
    private Screen currentScreen;
    private FontRenderer fontRenderer;
    private boolean isLoggedIn = false;
    static { 
        System.setProperty("joml.nounsafe", "true");
        System.setProperty("org.lwjgl.util.DebugLoader", "true");
        System.setProperty("org.lwjgl.opengl.libname", "opengl32.dll");
    }
    public void run() {
        System.out.println("LWJGL " + Version.getVersion());
        
        try (MongoDBConnection mongodb = new MongoDBConnection()) {
            System.out.println("Connected to MongoDB successfully");
        } catch (Exception e) {
            System.err.println("Failed to connect to MongoDB: " + e.getMessage());
        }

        init();
        
        // 1. Create ImGui context first
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        
        // 2. Initialize platform/renderer BEFORE loading fonts
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGl3 = new ImGuiImplGl3();
        imGuiGlfw.init(window, true);
        imGuiGl3.init("#version 330");
        
        // 3. Now it's safe to create the FontRenderer and load fonts
        fontRenderer = new FontRenderer();
        fontRenderer.loadImGuiFont();
        
        // Optional global scaling (after font loading)
        io.setFontGlobalScale(1.0f);
        
        // 4. Setup debug callback
        glEnable(GL_DEBUG_OUTPUT);
        glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
            System.err.println("GL DEBUG: " + GLDebugMessageCallback.getMessage(length, message));
        }, 0);
        
        // 5. Set initial screen and start the loop
        setCurrentScreen(new TitleScreen(this));
        loop();
        
        // Cleanup resources in this order
        if (fontRenderer != null) {
            fontRenderer.cleanup();
        }
        cleanup();
    }

    private void init() {
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Set OpenGL version to 3.3 with CORE profile
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "The Game", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1); // Enable V-Sync
        setupCallbacks();
        
    }
    
    public long getWindow() {
        return window;
    }

    private void setupCallbacks() {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(win, true);
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (currentScreen != null) {
                double[] xpos = new double[1];
                double[] ypos = new double[1];
                glfwGetCursorPos(win, xpos, ypos);

                int[] width = new int[1];
                int[] height = new int[1];
                glfwGetWindowSize(win, width, height);

                // Adjust for aspect ratio
                double glX = ((xpos[0] / width[0]) * 2 - 1) * ASPECT_RATIO;
                double glY = -((ypos[0] / height[0]) * 2 - 1);

                if (action == GLFW_PRESS) {
                    currentScreen.handleMouseClick(glX, glY);
                } else if (action == GLFW_RELEASE) {
                    currentScreen.handleMouseRelease(glX, glY);
                }
            }
        });

        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (currentScreen != null) {
                int[] width = new int[1];
                int[] height = new int[1];
                glfwGetWindowSize(win, width, height);

                // Adjust for aspect ratio
                double glX = ((xpos / width[0]) * 2 - 1) * ASPECT_RATIO;
                double glY = -((ypos / height[0]) * 2 - 1);
                currentScreen.handleMouseMove(glX, glY);
            }
        });
    }

    public void setCurrentScreen(Screen screen) {
        this.currentScreen = screen;
    }

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    private void cleanup() {
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void loop() {

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Start ImGui frame
            imGuiGlfw.newFrame();
            ImGui.newFrame();

            // Render current screen
            currentScreen.render();

            // End ImGui frame
            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
        // REMOVE THE cleanup() CALL HERE!
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.isLoggedIn = loggedIn;
    }

    public static void main(String[] args) {
        new App().run();
    }
}
