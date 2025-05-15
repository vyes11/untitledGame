package thegame.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.util.ArrayList;
import com.google.gson.Gson;

public class MongoDBConnection implements AutoCloseable {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "theGame";
    private static final String COLLECTION_NAME = "data";
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> levelsCollection;

    public MongoDBConnection() {
        try {
            
            this.mongoClient = MongoClients.create(CONNECTION_STRING);
            this.database = mongoClient.getDatabase(DATABASE_NAME);
            
            // Check if collection exists, create if it doesn't
            boolean collectionExists = database.listCollectionNames()
                .into(new ArrayList<>())
                .contains(COLLECTION_NAME);
                
            if (!collectionExists) {
                
                database.createCollection(COLLECTION_NAME);
            }
            
            this.levelsCollection = database.getCollection(COLLECTION_NAME);
           
        } catch (Exception e) {
            System.err.println("MongoDB Connection Error: " + e.getMessage());
            throw e;
        }
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public MongoCollection<Document> getLevelsCollection() {
        return levelsCollection;
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}