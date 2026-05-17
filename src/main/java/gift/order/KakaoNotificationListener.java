package gift.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class KakaoNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(KakaoNotificationListener.class);

    private final KakaoMessageClient kakaoMessageClient;

    public KakaoNotificationListener(KakaoMessageClient kakaoMessageClient) {
        this.kakaoMessageClient = kakaoMessageClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCompletedEvent event) {
        if (event.kakaoAccessToken() == null) {
            return;
        }
        try {
            kakaoMessageClient.sendToMe(event);
        } catch (Exception e) {
            log.warn("Kakao notification failed: orderId={}, memberId={}",
                event.orderId(), event.memberId(), e);
        }
    }
}
