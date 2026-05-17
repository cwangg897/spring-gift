package gift.order;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPoller {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventProcessor processor;

    public OutboxPoller(
        OutboxEventRepository outboxEventRepository,
        OutboxEventProcessor processor
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.processor = processor;
    }

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        List<OutboxEvent> batch = outboxEventRepository
            .findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING);
        for (OutboxEvent event : batch) {
            processor.processOne(event.getId());
        }
    }
}
