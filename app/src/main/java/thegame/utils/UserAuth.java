package thegame.utils;

import org.mindrot.jbcrypt.BCrypt;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

/**
 * Provides authentication utilities for user management.
 * Handles password hashing, verification, and user registration.
 */
public class UserAuth {
    
    /**
     * Hashes a password using BCrypt with strong security.
     *
     * @param plainTextPassword The password to hash
     * @return A secure hash of the password
     */
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }
    
    /**
     * Verifies a plain text password against a stored hash.
     *
     * @param plainTextPassword The plain text password to verify
     * @param hashedPassword The hashed password to check against
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
    
    /**
     * Registers a new user with a hashed password.
     *
     * @param username The username for the new user
     * @param password The password for the new user (will be hashed)
     * @return true if registration was successful, false if the username already exists or an error occurred
     */
    public static boolean registerUser(String username, String password) {
        try (MongoDBConnection mongodb = new MongoDBConnection()) {
            MongoCollection<Document> usersCollection = mongodb.getDatabase().getCollection("users");
            
            // Check if username already exists
            if (usersCollection.countDocuments(Filters.eq("username", username)) > 0) {
                return false;
            }
            
            // Hash the password before storing
            String hashedPassword = hashPassword(password);
            
            // Create user document with hashed password
            Document userDoc = new Document()
                .append("username", username)
                .append("password", hashedPassword)
                .append("createdAt", System.currentTimeMillis());
                
            usersCollection.insertOne(userDoc);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Authenticates a user by checking username and password.
     *
     * @param username The username to check
     * @param password The plain text password to verify
     * @return true if authentication was successful, false otherwise
     */
    public static boolean authenticateUser(String username, String password) {
        try (MongoDBConnection mongodb = new MongoDBConnection()) {
            MongoCollection<Document> usersCollection = mongodb.getDatabase().getCollection("users");
            
            // Find user by username
            Document userDoc = usersCollection.find(Filters.eq("username", username)).first();
            if (userDoc == null) {
                return false;
            }
            
            // Verify password against stored hash
            String hashedPassword = userDoc.getString("password");
            return verifyPassword(password, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
