package gift.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.member.Member;
import gift.member.MemberService;
import gift.option.Option;
import gift.option.OptionService;
import gift.product.Product;
import gift.wish.WishService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    static final String ORDER_COMPLETED_EVENT_TYPE = "OrderCompleted";

    private final OrderRepository orderRepository;
    private final OptionService optionService;
    private final MemberService memberService;
    private final WishService wishService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
        OrderRepository orderRepository,
        OptionService optionService,
        MemberService memberService,
        WishService wishService,
        OutboxEventRepository outboxEventRepository,
        ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.optionService = optionService;
        this.memberService = memberService;
        this.wishService = wishService;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order placeOrder(Member memberArg, OrderRequest request) {
        Option option = optionService.findByIdOrThrow(request.optionId());
        Member member = memberService.findById(memberArg.getId());

        option.subtractQuantity(request.quantity());

        Product product = option.getProduct();
        int price = product.getPrice() * request.quantity();
        member.deductPoint(price);

        Order saved = orderRepository.save(
            new Order(option, member, request.quantity(), request.message()));

        wishService.removeByMemberAndProduct(member, product.getId());

        enqueueKakaoNotification(saved, member, option, product, request);

        return saved;
    }

    public Page<Order> findByMember(Member member, Pageable pageable) {
        return orderRepository.findByMember_Id(member.getId(), pageable);
    }

    private void enqueueKakaoNotification(
        Order saved,
        Member member,
        Option option,
        Product product,
        OrderRequest request
    ) {
        OrderCompletedEvent payload = new OrderCompletedEvent(
            saved.getId(),
            member.getId(),
            member.getKakaoAccessToken(),
            option.getId(),
            option.getName(),
            request.quantity(),
            request.message(),
            product.getName(),
            product.getPrice()
        );
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEvent(ORDER_COMPLETED_EVENT_TYPE, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                "Failed to serialize OrderCompletedEvent payload", e);
        }
    }
}
