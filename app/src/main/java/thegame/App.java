package thegame;

import java.nio.IntBuffer;

import org.lwjgl.Version;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glViewport;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import thegame.screens.TitleScreen;
import thegame.utils.FontRenderer;
import thegame.utils.MongoDBConnection;

public class App {
    private long window;
    private GLFWErrorCallback errorCallback;
    private Screen currentScreen;
    private FontRenderer fontRenderer;

    public void run() {
        System.out.println("LWJGL " + Version.getVersion());

        try (MongoDBConnection mongodb = new MongoDBConnection()) {
            // Connection successful if no exception is thrown
            System.out.println("Connected to MongoDB successfully");
        } catch (Exception e) {
            System.err.println("Failed to connect to MongoDB: " + e.getMessage());
        }

        init();
        fontRenderer = new FontRenderer();  // Initialize FontRenderer after OpenGL context is created
        setCurrentScreen(new TitleScreen(this));
        loop();

        // Cleanup
        if (fontRenderer != null) {
            fontRenderer.cleanup();
        }
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        if (errorCallback != null) {
            errorCallback.free();
        }
    }

    private void init() {
        errorCallback = GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(800, 800, "Color Game", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        setupCallbacks();

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
                );
            }
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
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

                // Convert screen coordinates to OpenGL coordinates
                int[] width = new int[1];
                int[] height = new int[1];
                glfwGetWindowSize(win, width, height);
                double glX = (xpos[0] / width[0]) * 2 - 1;
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
                double glX = (xpos / width[0]) * 2 - 1;
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

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glViewport(0, 0, 800, 800);
            
            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            if (currentScreen != null) {
                currentScreen.render();
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new App().run();
    }
}
