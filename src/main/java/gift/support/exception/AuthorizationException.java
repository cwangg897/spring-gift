package gift.support.exception;

import org.springframework.http.HttpStatus;

public class AuthorizationException extends DomainException {

    public AuthorizationException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.FORBIDDEN;
    }
}
