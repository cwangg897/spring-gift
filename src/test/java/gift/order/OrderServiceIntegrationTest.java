package gift.order;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.support.AbstractIntegrationTest;
import gift.wish.Wish;
import gift.wish.WishRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private WishRepository wishRepository;

    @Test
    void placeOrderPersistsOrderAndSubtractsStockAndPoints() {
        Category category = categoryRepository.save(new Category("c-tx", "#ffffff", "https://example.com/i.jpg", null));
        Product product = productRepository.save(new Product("p-tx", 1000, "https://example.com/p.jpg", category));
        Option option = optionRepository.save(new Option(product, "opt-ok", 10));
        Member member = memberRepository.save(new Member("buyer-ok@example.com", "pw"));
        member.chargePoint(10_000);
        memberRepository.save(member);

        Order saved = orderService.placeOrder(member, new OrderRequest(option.getId(), 2, "thanks"));

        assertThat(saved.getId()).isNotNull();
        assertThat(optionRepository.findById(option.getId()).orElseThrow().getQuantity()).isEqualTo(8);
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getPoint()).isEqualTo(8_000);
    }

    @Test
    void placeOrderRollsBackStockAndPointsWhenPointDeductionFails() {
        Category category = categoryRepository.save(new Category("c-rb", "#000000", "https://example.com/i.jpg", null));
        Product product = productRepository.save(new Product("p-rb", 1_000_000, "https://example.com/p.jpg", category));
        Option option = optionRepository.save(new Option(product, "opt-rb", 5));
        Member member = memberRepository.save(new Member("buyer-rb@example.com", "pw"));
        member.chargePoint(100);
        memberRepository.save(member);

        assertThatThrownBy(() -> orderService.placeOrder(member, new OrderRequest(option.getId(), 2, "rollback")))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(optionRepository.findById(option.getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getPoint()).isEqualTo(100);
        assertThat(orderRepository.findAll())
            .noneMatch(o -> "rollback".equals(o.getMessage()));
    }

    @Test
    void placeOrderRaisesNotFoundExceptionWhenOptionMissing() {
        Member member = memberRepository.save(new Member("buyer-missing@example.com", "pw"));

        assertThatThrownBy(() -> orderService.placeOrder(member, new OrderRequest(999_999L, 1, "nope")))
            .isInstanceOf(OrderOptionNotFoundException.class);
    }

    @Test
    void placeOrderRemovesExistingWishForOrderedProduct() {
        Category category = categoryRepository.save(new Category("c-wishclean", "#abcabc", "https://example.com/i.jpg", null));
        Product product = productRepository.save(new Product("p-wishclean", 1000, "https://example.com/p.jpg", category));
        Option option = optionRepository.save(new Option(product, "opt-wishclean", 10));
        Member member = memberRepository.save(new Member("buyer-wishclean@example.com", "pw"));
        member.chargePoint(10_000);
        memberRepository.save(member);
        Wish wish = wishRepository.save(new Wish(member, product));

        orderService.placeOrder(member, new OrderRequest(option.getId(), 1, "clean"));

        assertThat(wishRepository.findById(wish.getId())).isEmpty();
    }
}
