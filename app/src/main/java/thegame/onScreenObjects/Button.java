package thegame.onScreenObjects;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import imgui.ImGui;
import imgui.ImDrawList;
import imgui.flag.ImGuiCol;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class Button {
    private float x, y, width, height;
    private float r, g, b; // Color
    private String text;
    
    // OpenGL objects
    private int vaoId;
    private int vboId;
    private int shaderProgram;
    private int projectionMatrixLocation;
    
    // Projection matrix (passed from App)
    private Matrix4f projectionMatrix;

    public Button(float x, float y, float width, float height, float r, float g, float b, String text) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.r = r;
        this.g = g;
        this.b = b;
        this.text = text;
        
        // Initialize with default projection matrix
        this.projectionMatrix = new Matrix4f().identity();
        
        // Initialize OpenGL objects
        initOpenGL();
    }
    
    private void initOpenGL() {
        // Create and compile the shader program
        createShaderProgram();
        
        // Create vertex and color data for the button (rectangle)
        float[] vertices = {
            // Position (x, y)           // Color (r, g, b)
            x,          y,          0.0f, r, g, b, // Bottom-left
            x + width,  y,          0.0f, r, g, b, // Bottom-right
            x + width,  y + height, 0.0f, r, g, b, // Top-right
            x,          y + height, 0.0f, r, g, b  // Top-left
        };
        
        // Create a VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // Create a VBO
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        
        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Color attribute
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // Unbind the VAO and VBO
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
    
    private void createShaderProgram() {
        // Vertex shader
        String vertexShaderSource = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aColor;
            
            uniform mat4 projection;
            
            out vec3 vertexColor;
            
            void main() {
                gl_Position = projection * vec4(aPos, 1.0);
                vertexColor = aColor;
            }
        """;
        
        // Fragment shader
        String fragmentShaderSource = """
            #version 330 core
            in vec3 vertexColor;
            out vec4 fragColor;
            
            void main() {
                fragColor = vec4(vertexColor, 1.0);
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
        
        // Get the location of the projection matrix uniform
        projectionMatrixLocation = glGetUniformLocation(shaderProgram, "projection");
        
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
    
    public void setProjectionMatrix(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }
    
    public void render() {
        // Use the shader program
        glUseProgram(shaderProgram);
        
        // Pass the projection matrix to the shader
        FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
        projectionMatrix.get(projectionBuffer);
        glUniformMatrix4fv(projectionMatrixLocation, false, projectionBuffer);
        
        // Bind the VAO
        glBindVertexArray(vaoId);
        
        // Draw the button (rectangle)
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        
        // Unbind the VAO
        glBindVertexArray(0);
        
        // Unbind the shader program
        glUseProgram(0);
        
        // Center text on the button using ImGui
        float textWidth = ImGui.calcTextSize(text).x;
        float textHeight = ImGui.getTextLineHeight();
        float textX = x + (width - textWidth) / 2;
        float textY = y + (height - textHeight) / 2;

        ImGui.getWindowDrawList().addText(
            textX, textY,
            ImGui.colorConvertFloat4ToU32(1.0f, 1.0f, 1.0f, 1.0f), // White text
            text
        );
    }
    
    public boolean isClicked(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public void cleanup() {
        // Clean up OpenGL resources
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
        glDeleteProgram(shaderProgram);
    }
    
    // Getters for button properties
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public float getWidth() {
        return width;
    }
    
    public float getHeight() {
        return height;
    }
    
    public String getText() {
        return text;
    }
}