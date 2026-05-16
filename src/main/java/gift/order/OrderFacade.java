package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Temporary transaction-boundary facade for the order creation flow.
 *
 * <p>Promoted to {@code OrderService} in 06-order Phase A; this class will be
 * removed in that PR. Do not extend its responsibilities.
 *
 * @deprecated removed in 06-order Phase A after promotion to {@code OrderService}.
 */
@Deprecated(forRemoval = true, since = "01.5")
@Service
public class OrderFacade {
    private static final Logger log = LoggerFactory.getLogger(OrderFacade.class);

    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderFacade(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.kakaoMessageClient = kakaoMessageClient;
    }

    @Transactional
    public Order createOrder(Member member, OrderRequest request) {
        final Option option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new OrderOptionNotFoundException(
                "Option not found. id=" + request.optionId()));

        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        final int price = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(price);
        memberRepository.save(member);

        final Order saved = orderRepository.save(
            new Order(option, member.getId(), request.quantity(), request.message()));

        sendKakaoMessageIfPossible(member, saved, option);
        return saved;
    }

    private void sendKakaoMessageIfPossible(Member member, Order order, Option option) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            final Product product = option.getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception e) {
            log.warn("Kakao notification failed: orderId={}, memberId={}",
                order.getId(), member.getId(), e);
        }
    }
}
