package io.github.eroberer.helidon.books;

import io.github.eroberer.helidon.base.Response;
import io.github.eroberer.helidon.books.mapper.BookMapperProvider;
import io.github.eroberer.helidon.books.model.Book;
import io.helidon.common.http.Http;
import io.helidon.dbclient.DbClient;
import io.helidon.webserver.*;
import jakarta.json.JsonObject;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BookService implements Service {

    public static final Logger LOGGER = Logger.getLogger(BookService.class.getName());

    private final DbClient dbClient;

    public BookService(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::listBooks)
                .get("/{isbn}", this::getBookByIsbn)
                .post("/", Handler.create(Book.class, this::insertBook))
                .put("/{isbn}", Handler.create(Book.class, this::updateBook))
                .delete("/{isbn}", this::deleteBook);
    }

    private void updateBook(ServerRequest serverRequest, ServerResponse serverResponse, Book book) {
        var oldIsbn = serverRequest.path().param("isbn");

        var parameters = BookMapperProvider.MAPPER.toNamedParameters(book);
        parameters.put("old_isbn", oldIsbn);

        dbClient.execute(exec -> exec.createNamedUpdate("update-book-by-isbn")
                        .params(parameters)
                        .execute()
                ).thenAccept(count -> serverResponse.send(Response.success("Updated: " + count + " values")))
                .exceptionally(throwable -> error(throwable, serverResponse));
    }

    private void insertBook(ServerRequest serverRequest, ServerResponse serverResponse, Book book) {
        dbClient.execute(exec -> exec
                        .createNamedInsert("insert-book")
                        .indexedParam(book)
                        .execute())
                .thenAccept(count -> serverResponse.send(Response.success("Inserted: " + count + " values")))
                .exceptionally(throwable -> error(throwable, serverResponse));
    }

    private void listBooks(ServerRequest serverRequest, ServerResponse serverResponse) {
        try {
            var books = dbClient
                    .execute(exec -> exec.namedQuery("select-all-book"))
                    .map(it -> it.as(Book.class)).collectList().get();

            serverResponse.send(Response.success(books));
        } catch (InterruptedException | ExecutionException e) {
            serverResponse.send(Response.error(e.getMessage()));
        }
    }

    private void getBookByIsbn(ServerRequest serverRequest, ServerResponse serverResponse) {
        var isbn = serverRequest.path().param("isbn");

        dbClient.execute(exec -> exec
                        .createNamedGet("select-book-by-isbn")
                        .addParam("isbn", isbn)
                        .execute())
                .thenAccept(maybeRow -> maybeRow
                        .ifPresentOrElse(
                                row -> serverResponse.send(Response.success(row.as(JsonObject.class))),
                                () -> notFound(serverResponse, isbn)))
                .exceptionally(throwable -> error(throwable, serverResponse));
    }

    private void deleteBook(ServerRequest serverRequest, ServerResponse serverResponse) {
        var isbn = serverRequest.path().param("isbn");

        dbClient.execute(exec -> exec
                        .createNamedDelete("delete-book-by-isbn")
                        .addParam("isbn", isbn)
                        .execute())
                .thenAccept(count -> serverResponse.send(Response.success("Deleted: " + count + " values")))
                .exceptionally(throwable -> error(throwable, serverResponse));
    }

    private void notFound(ServerResponse response, String isbn) {
        response.status(Http.Status.NOT_FOUND_404);
        response.send(Response.error("Book " + isbn + " not found"));
    }

    private <T> T error(Throwable throwable, ServerResponse response) {
        response.status(Http.Status.INTERNAL_SERVER_ERROR_500);
        response.send(Response.error(throwable.getMessage()));
        LOGGER.log(Level.WARNING, "Yahşı günde yar yahşıdır yaman günde yetiş gardaş", throwable);
        return null;
    }
}
