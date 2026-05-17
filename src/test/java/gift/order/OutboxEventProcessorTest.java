package gift.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OutboxEventProcessorTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxEventProcessor processor;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KakaoMessageClient kakaoMessageClient;

    @Test
    void processSkipsKakaoCallWhenAccessTokenIsNullAndMarksSent() throws Exception {
        OutboxEvent saved = persistPendingEvent(payloadWithToken(null));

        processor.processOne(saved.getId());

        OutboxEvent after = outboxEventRepository.findById(saved.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.SENT);
        assertThat(after.getProcessedAt()).isNotNull();
        verify(kakaoMessageClient, never()).sendToMe(Mockito.any());
    }

    @Test
    void processCallsKakaoAndMarksSentOnSuccess() throws Exception {
        OutboxEvent saved = persistPendingEvent(payloadWithToken("kakao-token-abc"));

        processor.processOne(saved.getId());

        OutboxEvent after = outboxEventRepository.findById(saved.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.SENT);
        verify(kakaoMessageClient, times(1)).sendToMe(Mockito.any());
    }

    @Test
    void processIncrementsAttemptsAndKeepsPendingOnFailure() throws Exception {
        Mockito.doThrow(new RuntimeException("kakao timeout"))
            .when(kakaoMessageClient).sendToMe(Mockito.any());
        OutboxEvent saved = persistPendingEvent(payloadWithToken("kakao-token-xyz"));

        processor.processOne(saved.getId());

        OutboxEvent after = outboxEventRepository.findById(saved.getId()).orElseThrow();
        assertThat(after.getAttempts()).isEqualTo(1);
        assertThat(after.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(after.getLastError()).contains("kakao timeout");
    }

    @Test
    void processMarksDeadAfterMaxAttempts() throws Exception {
        Mockito.doThrow(new RuntimeException("kakao 503"))
            .when(kakaoMessageClient).sendToMe(Mockito.any());
        OutboxEvent saved = persistPendingEvent(payloadWithToken("kakao-token-die"));
        // Drive attempts to 4 via repeated processing, then 5th attempt → DEAD
        for (int i = 0; i < OutboxEventProcessor.MAX_ATTEMPTS - 1; i++) {
            processor.processOne(saved.getId());
        }
        OutboxEvent beforeFinal = outboxEventRepository.findById(saved.getId()).orElseThrow();
        assertThat(beforeFinal.getAttempts()).isEqualTo(OutboxEventProcessor.MAX_ATTEMPTS - 1);
        assertThat(beforeFinal.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);

        processor.processOne(saved.getId());

        OutboxEvent after = outboxEventRepository.findById(saved.getId()).orElseThrow();
        assertThat(after.getAttempts()).isEqualTo(OutboxEventProcessor.MAX_ATTEMPTS);
        assertThat(after.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.DEAD);
        assertThat(after.getProcessedAt()).isNotNull();
    }

    private OutboxEvent persistPendingEvent(OrderCompletedEvent payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        return outboxEventRepository.save(
            new OutboxEvent(OrderService.ORDER_COMPLETED_EVENT_TYPE, json));
    }

    private OrderCompletedEvent payloadWithToken(String token) {
        return new OrderCompletedEvent(
            1L, 1L, token, 1L, "opt-test", 1, "msg", "product-test", 1000);
    }
}
