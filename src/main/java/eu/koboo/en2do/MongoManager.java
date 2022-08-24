package eu.koboo.en2do;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import eu.koboo.en2do.config.MongoConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MongoManager {

    MongoClient client;
    MongoDatabase database;

    public MongoManager(MongoConfig config) {
        ConnectionString connectionString;
        if (!config.useAuthSource()) {
            connectionString = new ConnectionString(
                    "mongodb://" + config.username() + ":" + config.password() + "@"
                            + config.host()
                            + ":" + config.port() + "/" + config.database());
        } else {
            connectionString = new ConnectionString(
                    "mongodb://" + config.username() + ":" + config.password() + "@"
                            + config.host()
                            + ":" + config.port() + "/?authSource=admin");
        }

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .retryWrites(true)
                .build();

        client = MongoClients.create(clientSettings);
        database = client.getDatabase(config.database());
    }

    public MongoManager() {
        this(MongoConfig.readConfig());
    }

    protected MongoDatabase getDatabase() {
        return database;
    }

    public boolean close() {
        try {
            client.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public <T, ID> Repository<T, ID> createRepository(ExecutorService executorService) {
        return new Repository<>(this, executorService);
    }

    public <T, ID> Repository<T, ID> createRepository() {
        return createRepository(Executors.newSingleThreadExecutor());
    }
}