package base.expected;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed abstract class Expected<T, E extends Exception> {
    public abstract boolean hasValue();

    public abstract T getValue();

    public abstract E getError();

    private static boolean errorEquals(final Throwable e1, final Throwable e2) {
        if (e1 == null) {
            return e2 == null;
        }
        //noinspection RedundantIfStatement
        if (e1.getClass() == e2.getClass()
                // && e1.getMessage().equals(e2.getMessage())
                && errorEquals(e1.getCause(), e2.getCause())) {
            // TODO: add stacktrace comparing.
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof final Expected that
                && hasValue() == that.hasValue()
                && (hasValue() ? getValue().equals(that.getValue()) : errorEquals(getError(), that.getError()));
    }

    @Override
    public int hashCode() {
        return hasValue() ? getValue().hashCode() : getError().hashCode();
    }

    public static <T> Expected<T, RuntimeException> tryCall(final Supplier<T> operation) {
        try {
            return ofValue(operation.get());
        } catch (final RuntimeException e) {
            return Expected.ofError(e);
        }
    }

    public static Expected<Void, RuntimeException> tryCall(final Runnable operation) {
        return tryCall(() -> {
            operation.run();
            return null;
        });
    }

    public final <U> Expected<U, E> map(final Function<T, U> mapping) {
        if (hasValue()) {
            return Expected.ofValue(mapping.apply(getValue()));
        } else {
            return Expected.ofError(getError());
        }
    }

    public final T getValueOr(final T defaultValue) {
        return hasValue() ? getValue() : defaultValue;
    }

    public final <E1 extends Error> T getValueOrThrow(final E1 error) throws E1 {
        if (hasValue()) {
            return getValue();
        } else {
            throw error;
        }
    }

    public final <E1 extends Error> T getValueOrApplyThrow(final Function<E, E1> errorFunction) throws E1 {
        if (hasValue()) {
            return getValue();
        } else {
            throw errorFunction.apply(getError());
        }
    }

    private static final class ExpectedValue<T, E extends Exception> extends Expected<T, E> {
        private final T value;

        private ExpectedValue(final T value) {
            this.value = value;
        }

        @Override
        public boolean hasValue() {
            return true;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public E getError() {
            throw new BadExpectedAccessException("Trying to get error while holding value " + value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private static final class ExpectedError<T, E extends Exception> extends Expected<T, E> {
        private final E error;

        private ExpectedError(final E error) {
            this.error = error;
        }

        @Override
        public boolean hasValue() {
            return false;
        }

        @Override
        public T getValue() {
            throw new BadExpectedAccessException("Trying to get value while holding error", error);
        }

        @Override
        public E getError() {
            return error;
        }

        @Override
        public String toString() {
            return error.toString();
        }
    }

    public static <T, E extends Exception> Expected<T, E> ofValue(final T value) {
        return new ExpectedValue<>(value);
    }

    public static <T, E extends Exception> Expected<T, E> ofError(final E error) {
        return new ExpectedError<>(error);
    }

    public static <T> Expected<T, Exception> ofError(final Throwable throwable) {
        if (throwable instanceof final Exception exception) {
            return Expected.ofError(exception);
        } else {
            throw (Error) throwable;
        }
    }
}
