package thegame.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import thegame.App;

/**
 * A utility class for rendering TrueType fonts using STB TrueType.
 * Supports loading and rendering text with various sizes and colors.
 */
public class FontRenderer {
    // Increase bitmap size for better quality
    private static final int BITMAP_W = 1024;
    private static final int BITMAP_H = 1024;
    // Make font height smaller for smoother text
    private static final int FONT_HEIGHT = 24;

    private int fontTextureID;
    private STBTTBakedChar.Buffer cdata;
    private int vao, vbo, shaderProgram;

    /**
     * Loads a TrueType font from a classpath resource.
     *
     * @param resourcePath The path to the font resource (e.g., "/fonts/myfont.ttf")
     */
    /**
 * Loads a TrueType font from a file or classpath resource.
 *
 * @param path The path to the font file or resource
 *            (use prefix "/" for classpath resources, e.g. "/fonts/myfont.ttf")
 */
public void loadFont(String path) {
    try {
        ByteBuffer ttf = ioResourceToByteBuffer(path, 160 * 1024);
        // Allocate more characters to support extended ASCII
        cdata = STBTTBakedChar.malloc(128);
        ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);

        // Bake the font with better parameters
        int result = STBTruetype.stbtt_BakeFontBitmap(ttf, FONT_HEIGHT, bitmap, BITMAP_W, BITMAP_H, 32, cdata);
        if (result <= 0) {
            throw new RuntimeException("Font baking failed with result: " + result);
        }

        // Create and configure texture
        fontTextureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureID);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RED, BITMAP_W, BITMAP_H, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, bitmap);
        
        // Use better texture filtering for smoother text
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        // Unbind texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // Setup VAO/VBO for a quad
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 24 * 4, GL15.GL_DYNAMIC_DRAW);
        
        // Position attribute
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 4, 0);
        GL20.glEnableVertexAttribArray(0);
        
        // Texture coordinate attribute
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * 4, 2 * 4);
        GL20.glEnableVertexAttribArray(1);
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        
        // Create shader program
        initShaders();

    } catch (IOException e) {
        throw new RuntimeException("Failed to load font: " + path, e);
    }
}

private void initShaders() {
    // Vertex shader: transforms coordinates and passes texture coordinates
    String vertexShaderSrc = 
        "#version 330 core\n" +
        "layout (location = 0) in vec2 position;\n" +
        "layout (location = 1) in vec2 texCoord;\n" +
        "out vec2 TexCoord;\n" +
        "uniform vec2 screenSize;\n" +
        "void main() {\n" +
        "    vec2 pos = position;\n" +
        "    pos.x = pos.x / screenSize.x * 2.0 - 1.0;\n" +
        "    pos.y = 1.0 - pos.y / screenSize.y * 2.0;\n" +
        "    gl_Position = vec4(pos, 0.0, 1.0);\n" +
        "    TexCoord = texCoord;\n" +
        "}\n";

    // Fragment shader: samples texture and applies color
    String fragmentShaderSrc = 
        "#version 330 core\n" +
        "in vec2 TexCoord;\n" +
        "out vec4 FragColor;\n" +
        "uniform sampler2D fontTex;\n" +
        "uniform vec4 color;\n" +
        "void main() {\n" +
        "    float alpha = texture(fontTex, TexCoord).r;\n" +
        "    FragColor = vec4(color.rgb, color.a * alpha);\n" +
        "}\n";

    // Compile and link shaders
    int vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexShaderSrc);
    int fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentShaderSrc);
    
    shaderProgram = GL20.glCreateProgram();
    GL20.glAttachShader(shaderProgram, vertexShader);
    GL20.glAttachShader(shaderProgram, fragmentShader);
    GL20.glLinkProgram(shaderProgram);
    
    if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
        throw new RuntimeException("Shader program linking failed: " + 
                                   GL20.glGetProgramInfoLog(shaderProgram));
    }
    
    // Clean up shader objects (they're linked to the program now)
    GL20.glDeleteShader(vertexShader);
    GL20.glDeleteShader(fragmentShader);
}

    private int compileShader(int type, String src) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, src);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Shader compile failed: " + GL20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    // Add overload with default white color for convenience
    public void renderText(String text, float x, float y, float scale) {
        renderText(text, x, y, scale, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Renders text at the specified position with the specified color.
     *
     * @param text The text to render
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param scale The scale of the text
     * @param r The red component of the text color (0-1)
     * @param g The green component of the text color (0-1)
     * @param b The blue component of the text color (0-1)
     * @param a The alpha component of the text color (0-1)
     */
    public void renderText(String text, float x, float y, float scale, float r, float g, float b, float a) {
        if (cdata == null) return;
        
        // Enable blending for better text rendering
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL20.glUseProgram(shaderProgram);
        GL30.glBindVertexArray(vao);

        // Set uniforms
        int screenSizeLoc = GL20.glGetUniformLocation(shaderProgram, "screenSize");
        GL20.glUniform2f(screenSizeLoc, App.WINDOW_WIDTH, App.WINDOW_HEIGHT); // Use actual window size 
        int colorLoc = GL20.glGetUniformLocation(shaderProgram, "color");
        GL20.glUniform4f(colorLoc, r, g, b, a);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureID);
        int fontTexLoc = GL20.glGetUniformLocation(shaderProgram, "fontTex");
        GL20.glUniform1i(fontTexLoc, 0);

        float xpos = x;
        float ypos = y;
        
        // Use Y-offset to align text properly (adjust as needed)
        ypos += FONT_HEIGHT * 0.7f * scale;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(xpos);
            FloatBuffer yb = stack.floats(ypos);
            org.lwjgl.stb.STBTTAlignedQuad q = org.lwjgl.stb.STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 32 || c >= 128) continue;
                
                STBTruetype.stbtt_GetBakedQuad(cdata, BITMAP_W, BITMAP_H, c - 32, xb, yb, q, true);

                // Draw vertices with proper positioning
                float[] vertices = {
                    q.x0() * scale, q.y0() * scale, q.s0(), q.t0(),
                    q.x1() * scale, q.y0() * scale, q.s1(), q.t0(),
                    q.x1() * scale, q.y1() * scale, q.s1(), q.t1(),

                    q.x0() * scale, q.y0() * scale, q.s0(), q.t0(),
                    q.x1() * scale, q.y1() * scale, q.s1(), q.t1(),
                    q.x0() * scale, q.y1() * scale, q.s0(), q.t1()
                };

                // Upload quad vertices
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_DYNAMIC_DRAW);

                // Draw quad
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            }
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
        GL11.glDisable(GL11.GL_BLEND);
    }

    /**
     * Renders text centered at the specified position.
     *
     * @param text The text to render
     * @param x The x-coordinate of the center of the text
     * @param y The y-coordinate
     * @param scale The scale of the text
     */
    public void renderCenteredText(String text, float x, float y, float scale) {
        renderCenteredText(text, x, y, scale, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Renders text centered at the specified position with the specified color.
     *
     * @param text The text to render
     * @param x The x-coordinate of the center of the text
     * @param y The y-coordinate
     * @param scale The scale of the text
     * @param r The red component of the text color (0-1)
     * @param g The green component of the text color (0-1)
     * @param b The blue component of the text color (0-1)
     * @param a The alpha component of the text color (0-1)
     */
    public void renderCenteredText(String text, float x, float y, float scale, float r, float g, float b, float a) {
        float width = getTextWidth(text, scale);
        float height = getTextHeight(scale);
        renderText(text, x - width / 2, y - height / 2, scale, r, g, b, a);
    }

    /**
     * Gets the width of the specified text at the specified scale.
     *
     * @param text The text to measure
     * @param scale The scale of the text
     * @return The width of the text in pixels
     */
    public float getTextWidth(String text, float scale) {
        if (cdata == null) return 0;
        float width = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(0.0f);
            FloatBuffer yb = stack.floats(0.0f);
            org.lwjgl.stb.STBTTAlignedQuad q = org.lwjgl.stb.STBTTAlignedQuad.malloc(stack);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 32 || c >= 128) continue;
                STBTruetype.stbtt_GetBakedQuad(cdata, BITMAP_W, BITMAP_H, c - 32, xb, yb, q, true);
            }
            width = xb.get(0);
        }
        return width * scale;
    }

    /**
     * Gets the height of the text at the specified scale.
     *
     * @param scale The scale of the text
     * @return The height of the text in pixels
     */
    public float getTextHeight(float scale) {
        return FONT_HEIGHT * scale;
    }

    /**
     * Cleans up resources used by the font renderer.
     */
    public void cleanup() {
        if (fontTextureID != 0) {
            GL11.glDeleteTextures(fontTextureID);
            fontTextureID = 0;
        }
        if (cdata != null) {
            cdata.free();
            cdata = null;
        }
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        if (vbo != 0) GL15.glDeleteBuffers(vbo);
        if (shaderProgram != 0) GL20.glDeleteProgram(shaderProgram);
    }

    // Replace the existing ioResourceToByteBuffer method with this version
    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        
        // Check if resource path starts with "/" for class resource loading
        if (resource.startsWith("/")) {
            // Load from classpath resource
            try (java.io.InputStream is = FontRenderer.class.getResourceAsStream(resource)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + resource);
                }
                
                // Read resource data into a byte array
                java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int read;
                while ((read = is.read(buf)) != -1) {
                    os.write(buf, 0, read);
                }
                
                // Convert to ByteBuffer
                byte[] bytes = os.toByteArray();
                buffer = BufferUtils.createByteBuffer(bytes.length + 1);
                buffer.put(bytes);
                buffer.flip();
            }
        } else {
            // Original file loading code (kept for backward compatibility)
            try (FileChannel fc = (FileChannel) Files.newByteChannel(Paths.get(resource), StandardOpenOption.READ)) {
                buffer = BufferUtils.createByteBuffer((int) fc.size() + 1);
                while (fc.read(buffer) != -1);
                buffer.flip();
            }
        }
        
        return buffer;
    }
}