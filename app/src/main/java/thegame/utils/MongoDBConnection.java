package thegame.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Manages connections to the MongoDB database.
 * Implements AutoCloseable to allow use with try-with-resources.
 */
public class MongoDBConnection implements AutoCloseable {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "theGame";
    private static final String COLLECTION_NAME = "data";
    
    private final MongoClient mongoClient;
    
    /**
     * Creates a new MongoDB connection.
     */
    public MongoDBConnection() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
    }
    
    /**
     * Gets the MongoDB database.
     *
     * @return The MongoDB database
     */
    public MongoDatabase getDatabase() {
        return mongoClient.getDatabase(DATABASE_NAME);
    }
    
    /**
     * Gets the levels collection.
     * Note: Levels are stored within user documents, so this returns the main data collection.
     *
     * @return The MongoDB collection containing user data and levels
     */
    public MongoCollection<Document> getLevelsCollection() {
        // Since all levels are within user documents, there's no separate levels collection
        // This just gives access to the 'data' collection which contains users
        return getDatabase().getCollection(COLLECTION_NAME);
    }
    
    /**
     * Closes the MongoDB connection.
     * Automatically called when used with try-with-resources.
     */
    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}