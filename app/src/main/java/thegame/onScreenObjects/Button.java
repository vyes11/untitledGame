package thegame.onScreenObjects;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import thegame.*;
import thegame.utils.FontRenderer;
import thegame.utils.GLDebugger;

public class Button {
    private float x, y, width, height;
    private float[] baseColor;
    private float[] hoverColor;
    private float[] pressColor;
    private int vaoId, vboId;
    private int shaderProgram;
    private boolean isHovered = false; // Track hover state
    
    // Caption properties
    private String caption;
    private float captionScale = 1.0f;
    private float textR = 1.0f, textG = 1.0f, textB = 1.0f, textA = 1.0f;
    private static FontRenderer sharedFontRenderer; // Static shared font renderer
    private FontRenderer fontRenderer;

    public Button(float x, float y, float width, float height, float r, float g, float b) {
        this(x, y, width, height, r, g, b, "");
    }
    
    public Button(float x, float y, float width, float height, float r, float g, float b, String caption) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.baseColor = new float[] {r, g, b};
        this.hoverColor = new float[] {
            Math.min(r + 0.2f, 1.0f), 
            Math.min(g + 0.2f, 1.0f), 
            Math.min(b + 0.2f, 1.0f)
        };
        this.pressColor = new float[] {
            Math.max(r - 0.2f, 0.0f), 
            Math.max(g - 0.2f, 0.0f), 
            Math.max(b - 0.2f, 0.0f)
        };
        this.caption = caption;
        
        // Use shared font renderer or create one if needed
        // This ensures we only create font resources on the main thread
        if (sharedFontRenderer == null) {
            sharedFontRenderer = new FontRenderer();
            sharedFontRenderer.loadFont("F:/temp/theGame/untitledGame/app/src/main/resources/fonts/pf_tempesta_seven_bold.ttf");
        }
        
        this.fontRenderer = sharedFontRenderer;
        setup();
    }

    private void setup() {
        // Vertex data for a rectangle (two triangles)
        float left = x;
        float right = x + width;
        float top = y;
        float bottom = y + height;

        // Convert to normalized device coordinates (-1 to 1)
        float nx = (left / 640.0f) - 1.0f;
        float ny = 1.0f - (top / 360.0f);
        float nwidth = (width / 640.0f);
        float nheight = (height / 360.0f);

        float[] vertices = {
            // x, y
            nx, ny,
            nx + nwidth, ny,
            nx + nwidth, ny - nheight,
            nx, ny - nheight
        };

        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);

        // Simple shader
        String vertexShaderSource =
            "#version 330 core\n" +
            "layout (location = 0) in vec2 position;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(position, 0.0, 1.0);\n" +
            "}";

        String fragmentShaderSource =
            "#version 330 core\n" +
            "uniform vec3 color;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "    fragColor = vec4(color, 1.0);\n" +
            "}";

        GLDebugger.clearErrors();

        // Create and compile vertex shader
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexShaderSource);
        GL20.glCompileShader(vertexShader);

        // Check for vertex shader errors using our debugger
        if (!GLDebugger.checkShaderCompilation(vertexShader, "Vertex Shader")) {
            throw new RuntimeException("Vertex shader compilation failed");
        }

        // Create and compile fragment shader
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentShaderSource);
        GL20.glCompileShader(fragmentShader);

        // Check for fragment shader errors using our debugger
        if (!GLDebugger.checkShaderCompilation(fragmentShader, "Fragment Shader")) {
            throw new RuntimeException("Fragment shader compilation failed");
        }

        // Create shader program
        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);

        // Check for linking errors using our debugger
        if (!GLDebugger.checkProgramLinking(shaderProgram)) {
            throw new RuntimeException("Shader program linking failed");
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        
        GLDebugger.checkError("Button setup completed");
    }

    public void render(float mouseX, float mouseY) {
        try {
            // Clear any existing errors before we start rendering
            GLDebugger.clearErrors();
            
            // Update hover state
            boolean previousHover = isHovered;
            isHovered = isInside(mouseX, mouseY);
            
            // Only change color when hover state changes
            float drawR = baseColor[0], drawG = baseColor[1], drawB = baseColor[2];
            if (isHovered) {
                drawR = hoverColor[0];
                drawG = hoverColor[1];
                drawB = hoverColor[2];
            }
            
            // Use shader program
            GL20.glUseProgram(shaderProgram);
            
            // Set uniform
            int colorLoc = GL20.glGetUniformLocation(shaderProgram, "color");
            
            // Check if uniform exists
            if (colorLoc == -1) {
                System.out.println("ERROR: Uniform 'color' not found in shader!");
            } else {
                GL20.glUniform3f(colorLoc, drawR, drawG, drawB);
            }
            
            // Draw
            GL30.glBindVertexArray(vaoId);
            
            GL20.glEnableVertexAttribArray(0);
            
            GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
            
            GL20.glDisableVertexAttribArray(0);
            GL30.glBindVertexArray(App.defaultVao); // Re-bind default VAO
            GL20.glUseProgram(0); // Unbind program
            
            // Render caption text if there is one
            if (caption != null && !caption.isEmpty()) {
                // Calculate the center of the button
                float centerX = x + width / 2;
                float centerY = y + height / 2;
                
                // Render centered text with the appropriate color
                fontRenderer.renderCenteredText(caption, centerX, centerY, captionScale, 
                                               textR, textG, textB, textA);
            }
        } catch (Exception e) {
            System.err.println("Error rendering button with caption: " + caption);
            e.printStackTrace();
        }
    }

    public void cleanup() {
        GL20.glDeleteProgram(shaderProgram);
        GL20.glDeleteBuffers(vboId);
        GL30.glDeleteVertexArrays(vaoId);
        // FontRenderer is shared, so we don't cleanup here
    }

    public boolean isInside(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean handleMouseClick(float mouseX, float mouseY) {
        try {
            return isInside(mouseX, mouseY);
        } catch (Exception e) {
            System.err.println("Error handling mouse click on button: " + caption);
            e.printStackTrace();
            return false;
        }
    }

    // Getter for hover state if needed by other components
    public boolean isHovered() {
        return isHovered;
    }
    
    // Setters for caption properties
    public void setCaption(String caption) {
        this.caption = caption;
    }
    
    public void setCaptionScale(float scale) {
        this.captionScale = scale;
    }
    
    public void setCaptionColor(float r, float g, float b, float a) {
        this.textR = r;
        this.textG = g;
        this.textB = b;
        this.textA = a;
    }
    
    // Getter for caption
    public String getCaption() {
        return caption;
    }

    // Getters for color components
    public float getR() {
        return baseColor[0];
    }
    
    public float getG() {
        return baseColor[1];
    }
    
    public float getB() {
        return baseColor[2];
    }
    
    // Getter for position and size
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
    
    // Add method to change button text dynamically
    public void setText(String text) {
        this.caption = text;
    }

    public void setColors(float r, float g, float b) {
        this.baseColor = new float[] {r, g, b};
        this.hoverColor = new float[] {
            Math.min(r + 0.2f, 1.0f), 
            Math.min(g + 0.2f, 1.0f), 
            Math.min(b + 0.2f, 1.0f)
        };
        this.pressColor = new float[] {
            Math.max(r - 0.2f, 0.0f), 
            Math.max(g - 0.2f, 0.0f), 
            Math.max(b - 0.2f, 0.0f)
        };
    }
}