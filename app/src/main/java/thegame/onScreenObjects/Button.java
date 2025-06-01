package thegame.onScreenObjects;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import thegame.utils.FontRenderer;
import thegame.utils.GLDebugger;

/**
 * A button UI component that can be clicked.
 * Provides visual feedback when hovered and supports text captions.
 */
public class Button {
    private float x, y, width, height;
    private float[] baseColor;
    private float[] hoverColor;
    private float[] pressColor;
    private int vaoId, vboId;
    private int shaderProgram;
    private boolean isHovered = false; // Track hover state
    private boolean isPressed = false; // Track press state
    
    // Caption properties
    private String caption;
    private float captionScale = 1.0f;
    private float textR = 1.0f, textG = 1.0f, textB = 1.0f, textA = 1.0f;
    private static FontRenderer sharedFontRenderer; // Static shared font renderer
    private FontRenderer fontRenderer;
    private float lineSpacing = 5.0f; // Spacing between lines for multiline text

    /**
     * Creates a new button with the specified position, size, color, and caption.
     *
     * @param x The x-coordinate of the top-left corner of the button
     * @param y The y-coordinate of the top-left corner of the button
     * @param width The width of the button
     * @param height The height of the button
     * @param r The red component of the button's color (0-1)
     * @param g The green component of the button's color (0-1)
     * @param b The blue component of the button's color (0-1)
     * @param caption The text to display on the button
     */
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
            sharedFontRenderer.loadFont("/fonts/pf_tempesta_seven_bold.ttf");
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

    /**
     * Renders the button at its current position.
     *
     * @param mouseX The current mouse x-coordinate
     * @param mouseY The current mouse y-coordinate
     */
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
            GL30.glBindVertexArray(0); // Use 0 instead of App.defaultVao to reset to default VAO
            GL20.glUseProgram(0); // Unbind program
            
            // Render caption text if there is one
            if (caption != null && !caption.isEmpty()) {
                // Calculate the center of the button
                float centerX = x + width / 2;
                float centerY = y + height / 2;
                
                // Check if caption contains newlines
                if (caption.contains("\n")) {
                    // Split the text by newline character
                    String[] lines = caption.split("\n");
                    int numLines = lines.length;
                    
                    // Calculate line height and spacing
                    float lineHeight = fontRenderer.getTextHeight(captionScale);
                    // Include spacing in total height calculation
                    float totalHeight = (lineHeight * numLines) + (lineSpacing * (numLines - 1));
                    
                    // Properly calculate vertical starting position to center the text block
                    float startY = centerY - (totalHeight / 2);
                    
                    // Render each line
                    for (int i = 0; i < lines.length; i++) {
                        float lineY = startY + (i * (lineHeight + lineSpacing));
                        fontRenderer.renderCenteredText(lines[i], centerX, lineY, 
                                                       captionScale, textR, textG, textB, textA);
                    }
                } else {
                    // Single line text - render as before
                    fontRenderer.renderCenteredText(caption, centerX, centerY, captionScale, 
                                                   textR, textG, textB, textA);
                }
            }
        } catch (Exception e) {
            System.err.println("Error rendering button with caption: " + caption);
            e.printStackTrace();
        }
    }

    /**
     * Renders the button with scaling support for different screen resolutions.
     *
     * @param mouseX The current mouse x-coordinate
     * @param mouseY The current mouse y-coordinate
     * @param scaleX The horizontal scale factor
     * @param scaleY The vertical scale factor
     */
    public void render(float mouseX, float mouseY, float scaleX, float scaleY) {
        // Scale coordinates
        float scaledX = x * scaleX;
        float scaledY = y * scaleY;
        float scaledWidth = width * scaleX;
        float scaledHeight = height * scaleY;
        
        boolean isHovered = (mouseX >= scaledX && mouseX <= scaledX + scaledWidth && 
                            mouseY >= scaledY && mouseY <= scaledY + scaledHeight);
        
        try {
            // Clear any existing errors before we start rendering
            GLDebugger.clearErrors();
            
            // Determine color based on state
            float[] currentColor = baseColor;
            if (isPressed) {
                currentColor = pressColor;
            } else if (isHovered) {
                currentColor = hoverColor;
            }
            
            // Skip the shader program and use immediate mode for scaled rendering
            // This ensures compatibility with scale factors
            GL11.glColor3f(currentColor[0], currentColor[1], currentColor[2]);
            
            // Draw button background
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(scaledX, scaledY);
            GL11.glVertex2f(scaledX + scaledWidth, scaledY);
            GL11.glVertex2f(scaledX + scaledWidth, scaledY + scaledHeight);
            GL11.glVertex2f(scaledX, scaledY + scaledHeight);
            GL11.glEnd();
            
            // Draw button border
            GL11.glLineWidth(2.0f);
            GL11.glColor3f(currentColor[0] * 0.7f, currentColor[1] * 0.7f, currentColor[2] * 0.7f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(scaledX, scaledY);
            GL11.glVertex2f(scaledX + scaledWidth, scaledY);
            GL11.glVertex2f(scaledX + scaledWidth, scaledY + scaledHeight);
            GL11.glVertex2f(scaledX, scaledY + scaledHeight);
            GL11.glEnd();
            
            // Draw button caption
            if (caption != null && !caption.isEmpty()) {
                // Center the text
                String[] lines = caption.split("\n");
                float lineHeight = fontRenderer.getTextHeight(captionScale * scaleY);
                float startY = scaledY + (scaledHeight - (lines.length * lineHeight)) / 2;
                
                for (int i = 0; i < lines.length; i++) {
                    float textWidth = fontRenderer.getTextWidth(lines[i], captionScale * scaleY);
                    float textX = scaledX + (scaledWidth - textWidth) / 2;
                    float textY = startY + i * lineHeight;
                    
                    fontRenderer.renderText(lines[i], textX, textY, captionScale * scaleY, 
                                          textR, textG, textB, textA);
                }
            }
        } catch (Exception e) {
            System.err.println("Error rendering scaled button with caption: " + caption);
            e.printStackTrace();
        }
    }

    /**
     * Handles mouse clicks on the button.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @return true if the button was clicked, false otherwise
     */
    public boolean handleMouseClick(float mouseX, float mouseY) {
        try {
            return isInside(mouseX, mouseY);
        } catch (Exception e) {
            System.err.println("Error handling mouse click on button: " + caption);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Handles mouse clicks on the button with scaling support.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @param scaleX The horizontal scale factor
     * @param scaleY The vertical scale factor
     * @return true if the button was clicked, false otherwise
     */
    public boolean handleMouseClick(float mouseX, float mouseY, float scaleX, float scaleY) {
        // Scale x and y coordinates for the button
        float scaledX = x * scaleX;
        float scaledY = y * scaleY;
        float scaledWidth = width * scaleX;
        float scaledHeight = height * scaleY;
        
        // Check if click is inside scaled bounds
        if (mouseX >= scaledX && mouseX <= scaledX + scaledWidth && 
            mouseY >= scaledY && mouseY <= scaledY + scaledHeight) {
            isPressed = true;
            return true;
        }
        return false;
    }
    
    /**
     * Checks if the mouse is hovering over the button.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @return true if the mouse is hovering over the button, false otherwise
     */
    private boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /**
     * Checks if the mouse is hovering over the button with scaling support.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @param scaleX The horizontal scale factor
     * @param scaleY The vertical scale factor
     * @return true if the mouse is hovering over the button, false otherwise
     */
    private boolean isHovered(float mouseX, float mouseY, float scaleX, float scaleY) {
        float scaledX = x * scaleX;
        float scaledY = y * scaleY;
        float scaledWidth = width * scaleX;
        float scaledHeight = height * scaleY;
        
        return mouseX >= scaledX && mouseX <= scaledX + scaledWidth && 
               mouseY >= scaledY && mouseY <= scaledY + scaledHeight;
    }
    
    /**
     * Checks if the mouse coordinates are inside the button boundaries.
     * This is an alias for isHovered to maintain compatibility with existing code.
     *
     * @param mouseX The mouse x-coordinate
     * @param mouseY The mouse y-coordinate
     * @return true if the mouse is inside the button, false otherwise
     */
    private boolean isInside(float mouseX, float mouseY) {
        return isHovered(mouseX, mouseY);
    }

    /**
     * Sets the caption text of the button.
     *
     * @param caption The text to display on the button
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }
    
    /**
     * An alias for setCaption for compatibility with existing code.
     *
     * @param text The text to display on the button
     */
    public void setText(String text) {
        this.caption = text;
    }

    /**
     * Sets the color of the button.
     *
     * @param r The red component of the button's color (0-1)
     * @param g The green component of the button's color (0-1)
     * @param b The blue component of the button's color (0-1)
     */
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

    /**
     * Sets the color of the button's caption text.
     *
     * @param r The red component of the text color (0-1)
     * @param g The green component of the text color (0-1)
     * @param b The blue component of the text color (0-1)
     * @param a The alpha component of the text color (0-1)
     */
    public void setCaptionColor(float r, float g, float b, float a) {
        this.textR = r;
        this.textG = g;
        this.textB = b;
        this.textA = a;
    }

    /**
     * Gets the x-coordinate of the button.
     *
     * @return The x-coordinate
     */
    public float getX() {
        return x;
    }

    /**
     * Gets the y-coordinate of the button.
     *
     * @return The y-coordinate
     */
    public float getY() {
        return y;
    }

    /**
     * Gets the width of the button.
     *
     * @return The width
     */
    public float getWidth() {
        return width;
    }

    /**
     * Gets the height of the button.
     *
     * @return The height
     */
    public float getHeight() {
        return height;
    }

    /**
     * Gets the red component of the button's color.
     *
     * @return The red component (0-1)
     */
    public float getR() {
        return baseColor[0];
    }

    /**
     * Gets the green component of the button's color.
     *
     * @return The green component (0-1)
     */
    public float getG() {
        return baseColor[1];
    }

    /**
     * Gets the blue component of the button's color.
     *
     * @return The blue component (0-1)
     */
    public float getB() {
        return baseColor[2];
    }

    /**
     * Gets the caption text of the button.
     *
     * @return The caption text
     */
    public String getCaption() {
        return caption;
    }
}