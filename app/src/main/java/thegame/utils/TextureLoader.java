package thegame.utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class TextureLoader {
    /**
     * Load a texture from a resource path
     * @param resourcePath Path to the texture file in resources
     * @return OpenGL texture ID
     */
    public static int loadTexture(String resourcePath) {
        ByteBuffer imageBuffer;
        int width, height;
        ByteBuffer image;
        
        try {
            // Print debugging info
            System.out.println("Attempting to load texture: " + resourcePath);
            
            // Load image file from resources
            imageBuffer = ioResourceToByteBuffer(resourcePath, 8 * 1024 * 1024);
            
            // Use STB to decode the image
            IntBuffer w = BufferUtils.createIntBuffer(1);
            IntBuffer h = BufferUtils.createIntBuffer(1);
            IntBuffer comp = BufferUtils.createIntBuffer(1);
            
            // Flip the image vertically to match OpenGL's coordinate system
            STBImage.stbi_set_flip_vertically_on_load(true);
            
            image = STBImage.stbi_load_from_memory(imageBuffer, w, h, comp, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load texture: " + resourcePath + ", reason: " + STBImage.stbi_failure_reason());
            }
            
            width = w.get(0);
            height = h.get(0);
            
            // Success message
            System.out.println("Successfully loaded texture: " + resourcePath + " (" + width + "x" + height + ")");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture: " + resourcePath, e);
        }
        
        // Create OpenGL texture
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        
        // Setup texture parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        // Upload texture data
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        
        // Free the image memory
        STBImage.stbi_image_free(image);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        return textureID;
    }
    
    // Resource loading method (same as in FontRenderer)
    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        
        // Check if resource path starts with "/" for class resource loading
        if (resource.startsWith("/")) {
            // Load from classpath resource
            try (java.io.InputStream is = TextureLoader.class.getResourceAsStream(resource)) {
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
            // File loading code for non-resource paths
            try (java.nio.channels.FileChannel fc = (java.nio.channels.FileChannel)
                 java.nio.file.Files.newByteChannel(java.nio.file.Paths.get(resource), java.nio.file.StandardOpenOption.READ)) {
                buffer = BufferUtils.createByteBuffer((int) fc.size() + 1);
                while (fc.read(buffer) != -1);
                buffer.flip();
            }
        }
        
        return buffer;
    }
}
