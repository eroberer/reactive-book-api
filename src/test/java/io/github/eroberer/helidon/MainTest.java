
package io.github.eroberer.helidon;

import io.github.eroberer.helidon.base.ResponseStatus;
import io.helidon.common.http.Http;
import io.helidon.media.jsonb.JsonbSupport;
import io.helidon.media.jsonp.JsonpSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webserver.WebServer;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MainTest {

    private static final JsonBuilderFactory JSON_BUILDER = Json.createBuilderFactory(Collections.emptyMap());

    private static WebServer webServer;
    private static WebClient webClient;

    @BeforeAll
    static void startTheServer() {
        webServer = Main.startServer().await();

        webClient = WebClient.builder()
                .baseUri("http://localhost:" + webServer.port())
                .addMediaSupport(JsonpSupport.create())
                .addMediaSupport(JsonpSupport.create())
                .addMediaSupport(JsonbSupport.create())
                .build();
    }

    @AfterAll
    static void stopServer() throws ExecutionException, InterruptedException, TimeoutException {
        if (webServer != null) {
            webServer.shutdown()
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void testMetrics() {
        WebClientResponse response = webClient.get()
                .path("/metrics")
                .request()
                .await();
        assertThat(response.status().code(), is(200));
    }

    @Test
    void testHealth() {
        WebClientResponse response = webClient.get()
                .path("health")
                .request()
                .await();
        assertThat(response.status().code(), is(200));
    }

    @Test
    void testBookList() throws ExecutionException, InterruptedException {
        webClient.get()
                .path("/books")
                .request(JsonObject.class)
                .thenApply(json -> {
                    assertThat(json.getString("status"), is(ResponseStatus.SUCCESS.name()));
                    return json.get("body");
                })
                .thenAccept(array -> {
                    var body = array.asJsonArray();
                    assertThat(body.size(), is(3));
                })
                .toCompletableFuture()
                .get();
    }

    @Test
    void testGetBookByIsbn() throws ExecutionException, InterruptedException {
        webClient.get()
                .path("/books/9759952378")
                .request(JsonObject.class)
                .thenAccept(book -> {
                    assertThat(book.get("body").asJsonObject().getString("isbn"), is("9759952378"));
                    assertThat(book.get("body").asJsonObject().getString("name"), is("Saatleri Ayarlama Enstitüsü"));
                })
                .toCompletableFuture()
                .get();
    }

    @Test
    void testInsertBook() throws ExecutionException, InterruptedException {
        JsonObject insertRequest = JSON_BUILDER.createObjectBuilder()
                .add("isbn", "6056669580")
                .add("name", "Beyaz Zambaklar Ülkesinde")
                .add("author", "Grigory Petrov")
                .add("language", "Türkçe")
                .build();

        webClient.post()
                .path("/books")
                .submit(insertRequest)
                .thenAccept(r -> assertThat(r.status(), is(Http.Status.OK_200)))
                .toCompletableFuture()
                .get();
        assertThat(getBookCount(), is(4));
    }

    @Test
    void testUpdateBook() throws ExecutionException, InterruptedException {

        JsonObject updateRequest = JSON_BUILDER.createObjectBuilder()
                .add("isbn", "111")
                .add("name", "Update Test Name")
                .add("author", "Update Test Author")
                .add("language", "JP")
                .build();

        webClient.put()
                .path("/books/9754587175")
                .submit(updateRequest)
                .thenAccept(r -> assertThat(r.status(), is(Http.Status.OK_200)))
                .toCompletableFuture()
                .get();

        webClient.get()
                .path("/books/111")
                .request(JsonObject.class)
                .thenAccept(book -> {
                    assertThat(book.get("body").asJsonObject().getString("isbn"), is("111"));
                    assertThat(book.get("body").asJsonObject().getString("name"), is("Update Test Name"));
                    assertThat(book.get("body").asJsonObject().getString("author"), is("Update Test Author"));
                    assertThat(book.get("body").asJsonObject().getString("language"), is("JP"));
                })
                .toCompletableFuture()
                .get();
    }

    @Test
    void testDeleteBook() throws ExecutionException, InterruptedException {
        var bookCount = getBookCount();
        webClient.delete()
                .path("/books/6053603538")
                .request()
                .thenAccept(r -> assertThat(r.status(), is(Http.Status.OK_200)))
                .toCompletableFuture()
                .get();
        assertThat(getBookCount(), is(bookCount - 1));
    }

    private int getBookCount() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> result = new CompletableFuture<>();
        webClient.get()
                .path("/books")
                .request(JsonObject.class)
                .thenAccept(json -> result.complete(json.get("body").asJsonArray().size()));
        return result.get();
    }
}
