package gift.order;

import gift.support.exception.NotFoundException;

public class OrderOptionNotFoundException extends NotFoundException {

    public OrderOptionNotFoundException(String message) {
        super(message);
    }
}
