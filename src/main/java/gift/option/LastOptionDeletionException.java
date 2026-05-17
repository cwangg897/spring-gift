package gift.option;

import gift.support.exception.DomainException;
import org.springframework.http.HttpStatus;

public class LastOptionDeletionException extends DomainException {

    public LastOptionDeletionException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
