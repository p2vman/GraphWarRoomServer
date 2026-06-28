package io.p2vman.graphwarserver.util;

import java.util.Objects;
import java.util.function.Function;

public final class Result<T> {

    private final T value;
    private final Throwable exception;

    private Result(T value, Throwable exception) {
        this.value = value;
        this.exception = exception;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> failure(Throwable exception) {
        return new Result<>(null, Objects.requireNonNull(exception));
    }

    public boolean isSuccess() {
        return exception == null;
    }

    public boolean isFailure() {
        return exception != null;
    }

    public T getOrThrow() {
        if (exception != null) {
            if (exception instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new RuntimeException(exception);
        }
        return value;
    }

    public T getOrNull() {
        return value;
    }

    public Throwable exceptionOrNull() {
        return exception;
    }

    public T getOrDefault(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }

    public T getOrElse(Function<Throwable, T> supplier) {
        return isSuccess() ? value : supplier.apply(exception);
    }

    public <R> Result<R> map(Function<T, R> mapper) {
        if (isFailure()) {
            return failure(exception);
        }

        try {
            return success(mapper.apply(value));
        } catch (Throwable t) {
            return failure(t);
        }
    }

    public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
        if (isFailure()) {
            return failure(exception);
        }

        try {
            return mapper.apply(value);
        } catch (Throwable t) {
            return failure(t);
        }
    }

    @Override
    public String toString() {
        return isSuccess()
                ? "Success(" + value + ')'
                : "Failure(" + exception + ')';
    }
}