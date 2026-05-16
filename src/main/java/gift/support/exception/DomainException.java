package gift.support.exception;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract HttpStatus status();
}
