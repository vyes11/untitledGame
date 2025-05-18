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

public class FontRenderer {
    // Increase bitmap size for better quality
    private static final int BITMAP_W = 1024;
    private static final int BITMAP_H = 1024;
    // Make font height smaller for smoother text
    private static final int FONT_HEIGHT = 24;

    private int fontTextureID;
    private STBTTBakedChar.Buffer cdata;
    private int vao, vbo, shaderProgram;

    public void loadFont(String fontPath) {
        try {
            ByteBuffer ttf = ioResourceToByteBuffer(fontPath, 160 * 1024);
            // Allocate more characters to support extended ASCII
            cdata = STBTTBakedChar.malloc(128);
            ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);

            // Bake the font with better parameters
            int result = STBTruetype.stbtt_BakeFontBitmap(ttf, FONT_HEIGHT, bitmap, BITMAP_W, BITMAP_H, 32, cdata);
            if (result <= 0) {
                throw new RuntimeException("Font baking failed with result: " + result);
            }

            fontTextureID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureID);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RED, BITMAP_W, BITMAP_H, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, bitmap);
            
            // Use better texture filtering for smoother text
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            // Unbind
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            // Setup VAO/VBO for a quad (6 vertices per character)
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            // 4 floats per vertex: x, y, u, v
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
            GL20.glEnableVertexAttribArray(1);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);

            // Simple shader for font rendering
            String vertSrc =
                "#version 330 core\n" +
                "layout(location = 0) in vec2 aPos;\n" +
                "layout(location = 1) in vec2 aUV;\n" +
                "out vec2 vUV;\n" +
                "uniform vec2 screenSize;\n" +
                "void main() {\n" +
                "    vec2 pos = aPos / screenSize * 2.0 - 1.0;\n" +
                "    gl_Position = vec4(pos.x, -pos.y, 0.0, 1.0);\n" +
                "    vUV = aUV;\n" +
                "}\n";
            String fragSrc =
                "#version 330 core\n" +
                "in vec2 vUV;\n" +
                "out vec4 FragColor;\n" +
                "uniform sampler2D fontTex;\n" +
                "uniform vec4 color;\n" +
                "void main() {\n" +
                "    float alpha = texture(fontTex, vUV).r;\n" +
                "    // Apply gamma correction for smoother edges\n" +
                "    alpha = pow(alpha, 1.0/2.2);\n" +
                "    // Use pre-multiplied alpha for better blending\n" +
                "    FragColor = vec4(color.rgb * alpha, alpha * color.a);\n" +
                "}\n";

            int vertShader = compileShader(GL20.GL_VERTEX_SHADER, vertSrc);
            int fragShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragSrc);
            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vertShader);
            GL20.glAttachShader(shaderProgram, fragShader);
            GL20.glLinkProgram(shaderProgram);
            GL20.glDeleteShader(vertShader);
            GL20.glDeleteShader(fragShader);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + fontPath, e);
        }
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

    // Add overload with color parameters for consistent API
    public void renderCenteredText(String text, float centerX, float centerY, float scale, 
                                  float r, float g, float b, float a) {
        float width = getTextWidth(text, scale);
        float height = getTextHeight(scale);
        renderText(text, centerX - width / 2, centerY - height / 2, scale, r, g, b, a);
    }

    public void renderCenteredText(String text, float centerX, float centerY, float scale) {
        renderCenteredText(text, centerX, centerY, scale, 1.0f, 1.0f, 1.0f, 1.0f);
    }

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

    public float getTextHeight(float scale) {
        return FONT_HEIGHT * scale;
    }

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

    // Utility to load a file into a ByteBuffer
    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        try (FileChannel fc = (FileChannel) Files.newByteChannel(Paths.get(resource), StandardOpenOption.READ)) {
            ByteBuffer buffer = BufferUtils.createByteBuffer((int) fc.size() + 1);
            while (fc.read(buffer) != -1) ;
            buffer.flip();
            return buffer;
        }
    }
}