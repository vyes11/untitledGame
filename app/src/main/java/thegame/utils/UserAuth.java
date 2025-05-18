package thegame.utils;

import org.mindrot.jbcrypt.BCrypt;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

public class UserAuth {
    
    // Hash a password using BCrypt (strong one-way hashing)
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }
    
    // Verify a password against a hash
    public static boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
    
    // Register a new user with hashed password
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
            System.err.println("Error registering user: " + e.getMessage());
            return false;
        }
    }
    
    // Authenticate a user
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
            System.err.println("Error authenticating user: " + e.getMessage());
            return false;
        }
    }
}
