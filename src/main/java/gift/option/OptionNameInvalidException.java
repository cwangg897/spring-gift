package gift.option;

import gift.support.exception.DomainException;
import org.springframework.http.HttpStatus;

public class OptionNameInvalidException extends DomainException {

    public OptionNameInvalidException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.BAD_REQUEST;
    }
}
