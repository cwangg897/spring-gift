package gift.category;

import gift.support.exception.DomainException;
import org.springframework.http.HttpStatus;

public class CategoryInUseException extends DomainException {

    public CategoryInUseException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.CONFLICT;
    }
}
