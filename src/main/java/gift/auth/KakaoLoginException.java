package gift.auth;

import gift.support.exception.DomainException;
import org.springframework.http.HttpStatus;

public class KakaoLoginException extends DomainException {

    public KakaoLoginException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
