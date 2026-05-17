package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final WishService wishService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        WishService wishService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.wishService = wishService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Order placeOrder(Member member, OrderRequest request) {
        Option option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new OrderOptionNotFoundException(
                "Option not found. id=" + request.optionId()));

        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        Product product = option.getProduct();
        int price = product.getPrice() * request.quantity();
        member.deductPoint(price);
        memberRepository.save(member);

        Order saved = orderRepository.save(
            new Order(option, member, request.quantity(), request.message()));

        wishService.removeByMemberAndProduct(member, product.getId());

        eventPublisher.publishEvent(new OrderCompletedEvent(
            saved.getId(),
            member.getId(),
            member.getKakaoAccessToken(),
            option.getId(),
            option.getName(),
            request.quantity(),
            request.message(),
            product.getName(),
            product.getPrice()
        ));

        return saved;
    }

    public Page<Order> findByMember(Member member, Pageable pageable) {
        return orderRepository.findByMember_Id(member.getId(), pageable);
    }
}
