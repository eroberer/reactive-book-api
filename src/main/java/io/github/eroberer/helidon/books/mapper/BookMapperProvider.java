package io.github.eroberer.helidon.books.mapper;

import io.github.eroberer.helidon.books.model.Book;
import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.spi.DbMapperProvider;

import java.util.*;

public class BookMapperProvider implements DbMapperProvider {

    public static final BookMapper MAPPER = new BookMapper();

    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        if (type.equals(Book.class)) {
            return Optional.of((DbMapper<T>) MAPPER);
        }
        return Optional.empty();
    }

    public static class BookMapper implements DbMapper<Book> {

        @Override
        public Book read(DbRow dbRow) {
            DbColumn name = dbRow.column("name");
            DbColumn author = dbRow.column("author");
            DbColumn isbn = dbRow.column("isbn");
            DbColumn language = dbRow.column("language");

            return new Book(
                    name.as(String.class),
                    author.as(String.class),
                    isbn.as(String.class),
                    language.as(String.class)
            );
        }

        @Override
        public Map<String, Object> toNamedParameters(Book book) {
            Map<String, Object> map = new HashMap<>(5);
            map.put("name", book.name());
            map.put("author", book.author());
            map.put("isbn", book.isbn());
            map.put("language", book.language());
            return map;
        }

        @Override
        public List<?> toIndexedParameters(Book book) {
            List<Object> list = new ArrayList<>(5);
            list.add(book.name());
            list.add(book.author());
            list.add(book.isbn());
            list.add(book.language());
            return list;
        }
    }
}
