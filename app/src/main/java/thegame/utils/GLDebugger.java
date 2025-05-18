package thegame.utils;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Utility class for OpenGL error checking and debugging
 */
public class GLDebugger {
    private static boolean debugMode = false;
    
    /**
     * Enable or disable debug mode
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }
    
    /**
     * Clear any existing OpenGL errors 
     */
    public static void clearErrors() {
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {
            // Clear all errors
        }
    }
    
    /**
     * Check for OpenGL errors and print them if debug mode is enabled
     */
    public static boolean checkError(String location) {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            String errorString = getErrorString(error);
            System.err.println("OpenGL error at " + location + ": " + errorString + " (code " + error + ")");
            return false;
        }
        return true;
    }
    
    /**
     * Check shader compilation status
     */
    public static boolean checkShaderCompilation(int shader, String name) {
        int success = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
        if (success == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            System.err.println(name + " compilation failed: " + log);
            return false;
        }
        return true;
    }
    
    /**
     * Check program linking status
     */
    public static boolean checkProgramLinking(int program) {
        int success = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS);
        if (success == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            System.err.println("Program linking failed: " + log);
            return false;
        }
        return true;
    }
    
    /**
     * Convert OpenGL error code to string
     */
    private static String getErrorString(int error) {
        switch (error) {
            case GL11.GL_INVALID_ENUM: return "GL_INVALID_ENUM";
            case GL11.GL_INVALID_VALUE: return "GL_INVALID_VALUE";
            case GL11.GL_INVALID_OPERATION: return "GL_INVALID_OPERATION";
            case GL11.GL_STACK_OVERFLOW: return "GL_STACK_OVERFLOW";
            case GL11.GL_STACK_UNDERFLOW: return "GL_STACK_UNDERFLOW";
            case GL11.GL_OUT_OF_MEMORY: return "GL_OUT_OF_MEMORY";
            case GL30.GL_INVALID_FRAMEBUFFER_OPERATION: return "GL_INVALID_FRAMEBUFFER_OPERATION";
            default: return "UNKNOWN_ERROR";
        }
    }
    
    /**
     * Safely execute an OpenGL operation with error checking
     */
    public static void safeGLOperation(Runnable operation, String location) {
        try {
            clearErrors();
            operation.run();
            checkError(location);
        } catch (Exception e) {
            System.err.println("Exception in GL operation at " + location + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Validates that we have a valid OpenGL context
     */
    public static boolean validateContext(String location) {
        boolean valid = GL.getCapabilities() != null && 
                      GL.getCapabilities().OpenGL11;
        
        if (!valid && debugMode) {
            System.err.println("ERROR: No valid OpenGL context at " + location);
        }
        
        return valid;
    }

    /**
     * Safely run OpenGL code with context validation and error checking
     */
    public static void safeExecute(Runnable code, String location) {
        if (!validateContext(location)) {
            System.err.println("Skipping OpenGL calls due to missing context: " + location);
            return;
        }
        
        try {
            clearErrors();
            code.run();
            checkError("After " + location);
        } catch (Exception e) {
            System.err.println("Exception in OpenGL operation at " + location + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
