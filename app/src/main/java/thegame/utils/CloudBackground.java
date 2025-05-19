package thegame.utils;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import thegame.App;

/**
 * Provides animated cloud backgrounds for various screens.
 * Can render detailed clouds with textures or simple white blots.
 */
public class CloudBackground {
    private List<Cloud> clouds = new ArrayList<>();
    private int[] cloudTextures;
    private Random random = new Random();
    private long lastUpdateTime;
    
    /** Defines the rendering style for the clouds */
    public enum RenderStyle {
        /** Textured clouds with full detail */
        TEXTURED,
        /** Simple white blots for subtle background effects */
        SIMPLE_BLOTS
    }
    
    private RenderStyle currentStyle = RenderStyle.TEXTURED;
    
    /**
     * Stores properties for a single cloud instance.
     */
    private static class Cloud {
        float x, y;
        float width, height;
        float speed;
        int textureIndex;
        float alpha;
        
        Cloud(float x, float y, float width, float height, float speed, int textureIndex, float alpha) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = speed;
            this.textureIndex = textureIndex;
            this.alpha = alpha;
        }
    }
    
    /**
     * Creates a new cloud background with default settings.
     */
    public CloudBackground() {
        // Load cloud textures
        loadCloudTextures();
        
        // Create initial clouds with random properties
        initializeClouds(15); // Create 15 clouds
        
        lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Creates a new cloud background with the specified style.
     * 
     * @param style The cloud rendering style to use
     */
    public CloudBackground(RenderStyle style) {
        this();
        this.currentStyle = style;
    }
    
    /**
     * Sets the rendering style for the cloud background.
     * 
     * @param style The desired rendering style
     */
    public void setRenderStyle(RenderStyle style) {
        this.currentStyle = style;
    }
    
    /**
     * Loads cloud textures from resources.
     */
    private void loadCloudTextures() {
        cloudTextures = new int[3];
        boolean loadedAnyTexture = false;
        
        String[][] pathsToTry = {
            {"/images/cloud1.png", "/images/cloud2.png", "/images/cloud3.png"},
            {"/cloud1.png", "/cloud2.png", "/cloud3.png"},
            {"/textures/cloud1.png", "/textures/cloud2.png", "/textures/cloud3.png"},
            {"cloud1.png", "cloud2.png", "cloud3.png"},
            {"resources/cloud1.png", "resources/cloud2.png", "resources/cloud3.png"},
            {"assets/cloud1.png", "assets/cloud2.png", "assets/cloud3.png"}
        };
        
        // Try each set of paths
        for (String[] paths : pathsToTry) {
            try {
                boolean allLoaded = true;
                for (int i = 0; i < 3; i++) {
                    try {
                        cloudTextures[i] = loadTexture(paths[i]);
                    } catch (Exception e) {
                        allLoaded = false;
                        break;
                    }
                }
                
                if (allLoaded) {
                    loadedAnyTexture = true;
                    break;
                }
            } catch (Exception e) {
                // Continue to next path set
            }
        }
        
        // If no textures could be loaded, create placeholder textures
        if (!loadedAnyTexture) {
            for (int i = 0; i < 3; i++) {
                cloudTextures[i] = createPlaceholderTexture(128, 64, (i+1) * 0.2f);
            }
        }
    }
    
    /**
     * Loads a texture from a resource path.
     * 
     * @param resourcePath Path to the texture file
     * @return The OpenGL texture ID
     * @throws IOException If the texture cannot be loaded
     */
    private int loadTexture(String resourcePath) throws IOException {
        ByteBuffer imageBuffer = ioResourceToByteBuffer(resourcePath, 8 * 1024 * 1024);
        
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer comp = BufferUtils.createIntBuffer(1);
        
        // Don't flip the image vertically - clouds should be upright
        STBImage.stbi_set_flip_vertically_on_load(false);
        
        ByteBuffer image = STBImage.stbi_load_from_memory(imageBuffer, w, h, comp, 4);
        if (image == null) {
            throw new RuntimeException("Failed to decode image: " + STBImage.stbi_failure_reason());
        }
        
        int width = w.get(0);
        int height = h.get(0);
        
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
        
        // Free the image memory
        STBImage.stbi_image_free(image);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        return textureID;
    }
    
    /**
     * Loads a resource into a ByteBuffer using the same approach as FontRenderer.
     * 
     * @param resource The resource path to load
     * @param bufferSize The buffer size to allocate
     * @return A ByteBuffer containing the resource data
     * @throws IOException If the resource cannot be loaded
     */
    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        
        // Check if resource path starts with "/" for class resource loading
        if (resource.startsWith("/")) {
            // Load from classpath resource
            try (java.io.InputStream is = CloudBackground.class.getResourceAsStream(resource)) {
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
    
    /**
     * Creates a simple placeholder texture when actual cloud images can't be loaded.
     * 
     * @param width The width of the texture
     * @param height The height of the texture
     * @param alpha The base alpha value for the texture
     * @return The OpenGL texture ID
     */
    private int createPlaceholderTexture(int width, int height, float alpha) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Calculate distance from center
                float dx = (x - width/2f) / (width/2f);
                float dy = (y - height/2f) / (height/2f);
                float distance = (float)Math.sqrt(dx*dx + dy*dy);
                
                // Create a soft circular gradient
                float intensity = Math.max(0, 1 - distance * 1.5f);
                
                // Make the edges softer
                intensity = intensity * intensity * (3 - 2 * intensity);
                
                // Add some noise
                intensity *= 0.7f + 0.3f * random.nextFloat();
                
                // Set white color with calculated alpha
                buffer.put((byte)255);  // R
                buffer.put((byte)255);  // G
                buffer.put((byte)255);  // B
                buffer.put((byte)(intensity * 255 * alpha)); // A
            }
        }
        
        buffer.flip();
        
        // Create OpenGL texture
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        
        // Setup texture parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        // Upload texture data
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return textureID;
    }
    
    /**
     * Creates the initial set of clouds.
     * 
     * @param count The number of clouds to create
     */
    private void initializeClouds(int count) {
        for (int i = 0; i < count; i++) {
            createRandomCloud(true); // true means cloud can be anywhere initially
        }
    }
    
    /**
     * Creates a random cloud with randomized properties.
     * 
     * @param initialPlacement Whether this is for initial placement (can be anywhere on screen)
     *                       or should start from the right edge
     */
    private void createRandomCloud(boolean initialPlacement) {
        // Position cloud either randomly across screen or just off right edge
        float x = initialPlacement ? 
                  random.nextFloat() * App.WINDOW_WIDTH : 
                  App.WINDOW_WIDTH + random.nextFloat() * 200;
                  
        float y = random.nextFloat() * App.WINDOW_HEIGHT * 0.9f;
        
        // Randomize cloud size
        float scale = 0.5f + random.nextFloat() * 1.0f;
        float width = 200 * scale;
        float height = 100 * scale;
        
        // Randomize speed based on size (smaller clouds move faster)
        float speed = 10f + (1.0f - scale) * 30f + random.nextFloat() * 5f;
        
        // Random texture
        int textureIndex = random.nextInt(cloudTextures.length);
        
        // Random transparency
        float alpha = 0.4f + random.nextFloat() * 0.4f;
        
        clouds.add(new Cloud(x, y, width, height, speed, textureIndex, alpha));
    }
    
    /**
     * Updates the cloud positions based on elapsed time.
     * Should be called once per frame before rendering.
     */
    public void update() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        
        // Limit delta time to prevent large jumps if app is paused
        if (deltaTime > 0.1f) deltaTime = 0.1f;
        
        // Update cloud positions
        for (int i = 0; i < clouds.size(); i++) {
            Cloud cloud = clouds.get(i);
            
            // Move clouds from right to left
            cloud.x -= cloud.speed * deltaTime;
            
            // If cloud moves off screen, reset it to the right
            if (cloud.x + cloud.width < 0) {
                clouds.remove(i);
                createRandomCloud(false);
                i--; // Adjust index since we removed an element
            }
        }
    }
    
    /**
     * Renders the clouds using the current render style.
     */
    public void render() {
        if (currentStyle == RenderStyle.SIMPLE_BLOTS) {
            renderSimple();
        } else {
            renderTextured();
        }
    }
    
    /**
     * Renders clouds as detailed textured quads.
     * This is the standard rendering style for menu screens.
     */
    private void renderTextured() {
        // Enable texturing and blending
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Render each cloud
        for (Cloud cloud : clouds) {
            // Bind the cloud texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, cloudTextures[cloud.textureIndex]);
            
            // Draw the cloud as a textured quad
            glColor4f(1.0f, 1.0f, 1.0f, cloud.alpha);
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0); glVertex2f(cloud.x, cloud.y);
            glTexCoord2f(1, 0); glVertex2f(cloud.x + cloud.width, cloud.y);
            glTexCoord2f(1, 1); glVertex2f(cloud.x + cloud.width, cloud.y + cloud.height);
            glTexCoord2f(0, 1); glVertex2f(cloud.x, cloud.y + cloud.height);
            glEnd();
        }
        
        // Reset state
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);
    }
    
    /**
     * Renders clouds as simple white blots.
     * This is a more subtle rendering style for gameplay screens.
     */
    private void renderSimple() {
        // Enable blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Render each cloud as a simple white shape with gradient
        for (Cloud cloud : clouds) {
            // Use a much lower alpha for simple blots
            float alpha = cloud.alpha * 0.3f;
            
            // Draw a simple soft oval
            for (int i = 0; i < 5; i++) {
                float scaleFactor = 1.0f - (i * 0.15f);
                float alphaFactor = 1.0f - (i * 0.25f);
                
                float width = cloud.width * scaleFactor;
                float height = cloud.height * 0.7f * scaleFactor;
                
                glColor4f(1.0f, 1.0f, 1.0f, alpha * alphaFactor);
                glBegin(GL_TRIANGLE_FAN);
                
                // Center point
                glVertex2f(cloud.x + width/2, cloud.y + height/2);
                
                // Edge points
                int segments = 16;
                for (int j = 0; j <= segments; j++) {
                    double angle = j * 2.0 * Math.PI / segments;
                    float x = (float)(Math.cos(angle) * width/2);
                    float y = (float)(Math.sin(angle) * height/2);
                    glVertex2f(cloud.x + width/2 + x, cloud.y + height/2 + y);
                }
                
                glEnd();
            }
        }
        
        // Reset state
        glDisable(GL_BLEND);
    }
    
    /**
     * Releases all OpenGL resources used by the cloud background.
     * Should be called when the cloud background is no longer needed.
     */
    public void cleanup() {
        // Delete cloud textures
        for (int textureId : cloudTextures) {
            GL11.glDeleteTextures(textureId);
        }
    }
}
