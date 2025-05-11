package thegame.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

public class FontRenderer {
    private ByteBuffer fontBuffer;
    private ByteBuffer bitmap;
    private STBTTBakedChar.Buffer charData;
    private int texID;
    private int bitmapWidth = 1024;  // Increased for better quality
    private int bitmapHeight = 1024; // Increased for better quality
    private float fontSize = 48.0f;   // Increased for better visibility
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 96;

    public FontRenderer() {
        try {
            // Load the font file from the fonts directory
            InputStream is = FontRenderer.class.getResourceAsStream("/fonts/pf_tempesta_seven.ttf");
            if (is == null) {
                throw new RuntimeException("Font file not found! Please place pf_tempesta_seven.ttf in the resources/fonts directory");
            }

            fontBuffer = readToByteBuffer(is);
            charData = STBTTBakedChar.malloc(NUM_CHARS);
            bitmap = memAlloc(bitmapWidth * bitmapHeight);

            // Bake font bitmap
            stbtt_BakeFontBitmap(fontBuffer, fontSize, bitmap, bitmapWidth, bitmapHeight, FIRST_CHAR, charData);

            // Generate OpenGL texture
            texID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texID);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, bitmapWidth, bitmapHeight, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
            
            // Use nearest neighbor filtering for pixel-perfect rendering
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            // Free bitmap memory as it's now in GPU
            memFree(bitmap);

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize font", e);
        }
    }

    public void renderText(String text, float x, float y, float scale, float r, float g, float b) {
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBindTexture(GL_TEXTURE_2D, texID);
        glColor3f(r, g, b);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xpos = stack.floats(x);
            FloatBuffer ypos = stack.floats(y);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;

                stbtt_GetBakedQuad(charData, bitmapWidth, bitmapHeight, c - FIRST_CHAR, xpos, ypos, q, true);

                glBegin(GL_QUADS);
                glTexCoord2f(q.s0(), q.t0()); glVertex2f(q.x0() * scale, q.y0() * scale);
                glTexCoord2f(q.s1(), q.t0()); glVertex2f(q.x1() * scale, q.y0() * scale);
                glTexCoord2f(q.s1(), q.t1()); glVertex2f(q.x1() * scale, q.y1() * scale);
                glTexCoord2f(q.s0(), q.t1()); glVertex2f(q.x0() * scale, q.y1() * scale);
                glEnd();
            }
        }

        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
    }

    private ByteBuffer readToByteBuffer(InputStream is) throws IOException {
        ByteBuffer buffer;
        try (ReadableByteChannel rbc = Channels.newChannel(is)) {
            buffer = BufferUtils.createByteBuffer(1024 * 1024);
            while (true) {
                int bytes = rbc.read(buffer);
                if (bytes == -1) break;
                if (buffer.remaining() == 0) {
                    ByteBuffer newBuffer = BufferUtils.createByteBuffer(buffer.capacity() * 2);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }
        }
        buffer.flip();
        return buffer;
    }

    public void cleanup() {
        if (charData != null) {
            charData.free();
        }
        if (fontBuffer != null) {
            memFree(fontBuffer);
        }
        if (texID != 0) {
            glDeleteTextures(texID);
        }
    }

    public float getTextWidth(String text, float scale) {
        float width = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer x = stack.floats(0);
            FloatBuffer y = stack.floats(0);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);
            
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;
                
                stbtt_GetBakedQuad(charData, bitmapWidth, bitmapHeight, c - FIRST_CHAR, x, y, q, true);
                width = q.x1();
            }
        }
        return width * scale;
    }

    public float getTextHeight(float scale) {
        return fontSize * scale;
    }
}