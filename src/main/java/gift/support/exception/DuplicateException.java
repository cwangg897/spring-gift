package gift.support.exception;

import org.springframework.http.HttpStatus;

public class DuplicateException extends DomainException {

    public DuplicateException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.CONFLICT;
    }
}
