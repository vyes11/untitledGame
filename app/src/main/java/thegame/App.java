package thegame;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import thegame.screens.TitleScreen;
import thegame.utils.FontRenderer;

import java.nio.FloatBuffer;

public class App {
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 720;
    private static final float ASPECT_RATIO = (float) WINDOW_WIDTH / WINDOW_HEIGHT;

    private long window;
    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private Screen currentScreen;
    private FontRenderer fontRenderer;
    private boolean isLoggedIn = false;

    private int vaoId;
    private int vboId;
    private int shaderProgram;

    private Matrix4f projectionMatrix;
    private int projectionMatrixLocation;

    public void run() {
        System.out.println("LWJGL " + Version.getVersion());
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

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "OpenGL with ImGui Support", NULL, NULL);
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

        // Initialize ImGui
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);

        // Add a default font (or your custom font)
        io.getFonts().addFontDefault();

        // Build the font atlas
        io.getFonts().build();

        // Check if fonts were loaded
        
        System.out.println("ImGui fonts loaded: " + (ImGui.getIO().getFonts().isBuilt() ? "Yes" : "No"));


        // Optional: Set global font scaling
        io.setFontGlobalScale(1.0f);

        // Set the initial screen
        currentScreen = new TitleScreen(this);

        // Set up callbacks
        setupCallbacks();

        // Initialize OpenGL objects
        initOpenGL();
        
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGl3 = new ImGuiImplGl3();
        imGuiGlfw.init(window, true);
        imGuiGl3.init("#version 330 core");
    }

    private void initOpenGL() {
        // After creating OpenGL context
        System.out.println("OpenGL version: " + glGetString(GL_VERSION));
        System.out.println("GLSL version: " + glGetString(GL_SHADING_LANGUAGE_VERSION));
        System.out.println("Vendor: " + glGetString(GL_VENDOR));
        System.out.println("Renderer: " + glGetString(GL_RENDERER));
        
        // We already know we're using Core Profile from our window hints
        System.out.println("Using Core Profile: Yes (set via GLFW_OPENGL_CORE_PROFILE)");
        
        // Set up the projection matrix
        setupProjectionMatrix();

        // Create and compile the shader program
        createShaderProgram();

        // Define vertices for a triangle (example)
        float[] vertices = {
            -0.5f, -0.5f, 0.0f, // Bottom-left
             0.5f, -0.5f, 0.0f, // Bottom-right
             0.0f,  0.5f, 0.0f  // Top-center
        };

        // Create a VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Create a VBO
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // Define vertex attributes
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        // Unbind the VAO and VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void setupProjectionMatrix() {
        // Create an orthographic projection matrix
        projectionMatrix = new Matrix4f().ortho2D(0, WINDOW_WIDTH, WINDOW_HEIGHT, 0);

        // Pass the projection matrix to the shader
        FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
        projectionMatrix.get(matrixBuffer);

        glUseProgram(shaderProgram); // Use the shader program
        glUniformMatrix4fv(projectionMatrixLocation, false, matrixBuffer);
        glUseProgram(0); // Unbind the shader program
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            // Poll events first (mouse, keyboard, etc.)
            glfwPollEvents();
            
            // Clear framebuffer 
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            // Start ImGui frame
            glViewport(0, 0, App.WINDOW_WIDTH, App.WINDOW_HEIGHT);
            glDisable(GL_SCISSOR_TEST);
            glDisable(GL_STENCIL_TEST);
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glUseProgram(0);
            glBindVertexArray(0);
            imGuiGlfw.newFrame();
            ImGui.newFrame();
            
            ImGuiIO io = ImGui.getIO();
            System.out.println("ImGui DisplaySize: " + io.getDisplaySizeX() + " x " + io.getDisplaySizeY());
            System.out.println("ImGui FramebufferScale: " + io.getDisplayFramebufferScaleX() + " x " + io.getDisplayFramebufferScaleY());
            
            // Render current screen
            if (currentScreen != null) {
                currentScreen.render();
            }
            
            // End ImGui frame
            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());
            
            // Swap buffers
            glfwSwapBuffers(window);
        }
    }

    private void setupCallbacks() {
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(win, true);
            }
            
            if (currentScreen != null) {
                currentScreen.handleKeyPress(key, action);
            }
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            System.out.println("Mouse button pressed: " + button + ", action: " + action);
            
            // Get cursor position
            double[] xpos = new double[1];
            double[] ypos = new double[1];
            glfwGetCursorPos(win, xpos, ypos);
            System.out.println("Mouse position: " + xpos[0] + ", " + ypos[0]);
            
            // Check if ImGui wants the mouse
            boolean imguiWantsMouse = ImGui.getIO().getWantCaptureMouse();
            System.out.println("ImGui wants mouse: " + imguiWantsMouse);
            
            // Always pass to your screen handler for debugging (remove the ImGui check)
            if (currentScreen != null && action == GLFW_PRESS) {
                currentScreen.handleMouseClick(xpos[0], ypos[0]);
            }
        });
        
        // Add mouse move callback for hover effects
        glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            if (ImGui.getIO().getWantCaptureMouse()) {
                return;
            }
            if (currentScreen != null) {
                currentScreen.handleMouseMove(xpos, ypos);
            }
        });
    }

    private void cleanup() {
        // Cleanup OpenGL objects
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);

        // Cleanup ImGui
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void createShaderProgram() {
        // Vertex shader source
        String vertexShaderSource = """
            #version 330 core
            layout(location = 0) in vec3 aPos;

            uniform mat4 projection;

            void main() {
                gl_Position = projection * vec4(aPos, 1.0);
            }
        """;

        // Fragment shader source
        String fragmentShaderSource = """
            #version 330 core
            out vec4 fragColor;

            void main() {
                fragColor = vec4(1.0, 0.5, 0.2, 1.0); // Orange color
            }
        """;

        // Compile vertex shader
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        checkShaderCompileStatus(vertexShader);

        // Compile fragment shader
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);
        checkShaderCompileStatus(fragmentShader);

        // Link shaders into a program
        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        checkProgramLinkStatus(shaderProgram);

        // Clean up shaders (no longer needed after linking)
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private void checkShaderCompileStatus(int shader) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compilation failed: " + glGetShaderInfoLog(shader));
        }
    }

    private void checkProgramLinkStatus(int program) {
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Program linking failed: " + glGetProgramInfoLog(program));
        }
    }

    public void setCurrentScreen(Screen screen) {
        this.currentScreen = screen;
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
