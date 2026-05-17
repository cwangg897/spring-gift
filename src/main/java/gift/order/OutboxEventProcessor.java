package gift.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventProcessor {
    static final int MAX_ATTEMPTS = 5;

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KakaoMessageClient kakaoMessageClient;
    private final ObjectMapper objectMapper;

    public OutboxEventProcessor(
        OutboxEventRepository outboxEventRepository,
        KakaoMessageClient kakaoMessageClient,
        ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kakaoMessageClient = kakaoMessageClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processOne(Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
        if (event == null || event.getStatus() != OutboxEvent.OutboxStatus.PENDING) {
            return;
        }
        try {
            OrderCompletedEvent payload = objectMapper.readValue(
                event.getPayload(), OrderCompletedEvent.class);
            if (payload.kakaoAccessToken() != null) {
                kakaoMessageClient.sendToMe(payload);
            }
            event.markSent();
        } catch (Exception e) {
            log.warn("Outbox delivery failed: eventId={}, attempts={}",
                event.getId(), event.getAttempts() + 1, e);
            event.markFailure(e.getMessage(), MAX_ATTEMPTS);
        }
    }
}
