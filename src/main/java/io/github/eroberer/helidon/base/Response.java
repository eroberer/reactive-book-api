package io.github.eroberer.helidon.base;

public record Response<T>(ResponseStatus status, T body, String error) {

    public static <T> Response success(T body) {
        return new Response(ResponseStatus.SUCCESS, body, null);
    }

    public static <T> Response error(String error) {
        return new Response(ResponseStatus.FAIL, null, error);
    }
}
