package gift.product;

import gift.support.exception.DomainException;
import org.springframework.http.HttpStatus;

public class ProductNameInvalidException extends DomainException {

    public ProductNameInvalidException(String message) {
        super(message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.BAD_REQUEST;
    }
}
