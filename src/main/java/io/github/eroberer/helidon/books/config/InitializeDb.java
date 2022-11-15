package io.github.eroberer.helidon.books.config;

import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbExecute;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static jakarta.json.Json.createReader;

public class InitializeDb {

    private static final Logger LOGGER = Logger.getLogger(InitializeDb.class.getName());

    /**
     * Books source file.
     * */
    private static final String BOOKS = "/books.json";

    private InitializeDb() {
        throw new UnsupportedOperationException("Instances of InitializeDb utility class are not allowed");
    }

    /**
     * Initialize JDBC database schema and populate it with sample data.
     *
     * @param dbClient database client
     */
    public static void init(DbClient dbClient) {
        try {
            initSchema(dbClient);
            initData(dbClient);
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.warning(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    /**
     * Initializes database schema.
     *
     * @param dbClient database client
     */
    private static void initSchema(DbClient dbClient) {
        try {
            dbClient.execute(exec -> exec.namedDml("create-book-table")).await();
        } catch (Exception e) {
            LOGGER.warning(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    /**
     * Initialize database content.
     *
     * @param dbClient database client
     * @throws ExecutionException when database query failed
     * @throws InterruptedException if the current thread was interrupted
     */
    private static void initData(DbClient dbClient) throws InterruptedException, ExecutionException {
        dbClient.execute(exec -> initBooks(exec))
                .toCompletableFuture()
                .get();
    }

    /**
     * Initialize Books.
     *
     * @param exec database client executor
     * @return executed statements future
     */
    private static Single<Long> initBooks(DbExecute exec) {
        Single<Long> stage = Single.just(0L);
        try (JsonReader reader = createReader(InitializeDb.class.getResourceAsStream(BOOKS))) {
            JsonArray books = reader.readArray();
            for (JsonValue bookValue : books) {
                JsonObject book = bookValue.asJsonObject();
                stage = stage.flatMapSingle(result -> exec.namedInsert("insert-book",
                        book.getString("name"),
                        book.getString("author"),
                        book.getString("isbn"),
                        book.getString("language")));
            }
        }
        return stage;
    }
}
