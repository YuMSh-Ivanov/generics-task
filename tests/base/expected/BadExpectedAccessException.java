package base.expected;

public class BadExpectedAccessException extends RuntimeException {
    BadExpectedAccessException(final String message) {
        super(message);
    }

    BadExpectedAccessException(final String message, final Throwable cause) {
        super(message, cause);
    }

    BadExpectedAccessException(final Throwable cause) {
        super(cause);
    }
}
