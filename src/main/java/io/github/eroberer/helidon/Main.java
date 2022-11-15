package io.github.eroberer.helidon;


import io.github.eroberer.helidon.books.BookService;
import io.github.eroberer.helidon.books.config.InitializeDb;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.metrics.MetricsSupport;
import io.helidon.health.HealthSupport;
import io.helidon.health.checks.HealthChecks;
import io.helidon.dbclient.DbClient;
import io.helidon.media.jsonb.JsonbSupport;
import io.helidon.dbclient.health.DbClientHealthCheck;
import io.helidon.common.LogConfig;
import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

public final class Main {

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        startServer();
    }

    /**
     * Start the server.
     *
     * @return the created {@link WebServer} instance
     */
    static Single<WebServer> startServer() {

        // load logging configuration
        LogConfig.configureRuntime();

        // By default this will pick up application.yaml from the classpath
        Config config = Config.create();

        var server = WebServer.builder(createRouting(config))
                .config(config.get("server"))
                .addMediaSupport(JsonpSupport.create())
                .addMediaSupport(JsonbSupport.create())
                .addMediaSupport(JsonpSupport.create())
                .build();

        var webserver = server.start();

        // Try to start the server. If successful, print some info and arrange to
        // print a message at shutdown. If unsuccessful, print the exception.
        webserver.thenAccept(ws -> {
                    ws.whenShutdown().thenRun(() -> System.out.println("WEB server is DOWN. Good bye!"));
                })
                .exceptionallyAccept(t -> {
                    System.err.println("Startup failed: " + t.getMessage());
                    t.printStackTrace(System.err);
                });

        return webserver;
    }

    /**
     * Creates new {@link Routing}.
     *
     * @param config configuration of this server
     * @return routing configured with JSON support, a health check, and a service
     */
    private static Routing createRouting(Config config) {

        var dbConfig = config.get("db");

        // Client services are added through a service loader
        var dbClient = DbClient.builder(dbConfig).build();

        // Initialize database schema
        InitializeDb.init(dbClient);

        var health = HealthSupport.builder()
                .addLiveness(HealthChecks.healthChecks()) // Adds a convenient set of checks
                .addLiveness(DbClientHealthCheck.create(dbClient, dbConfig.get("health-check")))
                .build();

        var builder = Routing.builder()
                .register(MetricsSupport.create()) // Metrics at "/metrics"
                .register(health) // Health at "/health"
                .register("/books", new BookService(dbClient));

        return builder.build();
    }
}
