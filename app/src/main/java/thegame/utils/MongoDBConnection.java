package thegame.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBConnection implements AutoCloseable {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "theGame";
    private static final String COLLECTION_NAME = "data";
    
    private final MongoClient mongoClient;
    
    public MongoDBConnection() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        System.out.println("MongoDB connection initialized");
    }
    
    public MongoDatabase getDatabase() {
        return mongoClient.getDatabase(DATABASE_NAME);
    }
    
    public MongoCollection<Document> getLevelsCollection() {
        // Since all levels are within user documents, there's no separate levels collection
        // This just gives access to the 'data' collection which contains users
        return getDatabase().getCollection(COLLECTION_NAME);
    }
    
    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed");
        }
    }
}