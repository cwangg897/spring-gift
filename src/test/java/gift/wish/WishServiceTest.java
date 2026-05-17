package gift.wish;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class WishServiceTest extends AbstractIntegrationTest {

    @Autowired
    private WishService wishService;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void addNewWishMarksNewlyCreated() {
        Member member = memberRepository.save(new Member("wish-new@example.com", "pw"));
        Product product = persistProduct("c-wish-new", "p-wish-new");

        WishService.AddOutcome outcome = wishService.add(member, product.getId());

        assertThat(outcome.wish()).isNotNull();
        assertThat(outcome.newlyCreated()).isTrue();
        assertThat(wishRepository.findById(outcome.wish().getId())).isPresent();
    }

    @Test
    void addDuplicateWishReturnsExistingWithoutNewlyCreated() {
        Member member = memberRepository.save(new Member("wish-dup@example.com", "pw"));
        Product product = persistProduct("c-wish-dup", "p-wish-dup");
        Wish first = wishRepository.save(new Wish(member, product));

        WishService.AddOutcome outcome = wishService.add(member, product.getId());

        assertThat(outcome.newlyCreated()).isFalse();
        assertThat(outcome.wish().getId()).isEqualTo(first.getId());
    }

    @Test
    void addReturnsNullWishForUnknownProduct() {
        Member member = memberRepository.save(new Member("wish-none@example.com", "pw"));

        WishService.AddOutcome outcome = wishService.add(member, 999_999L);

        assertThat(outcome.wish()).isNull();
        assertThat(outcome.newlyCreated()).isFalse();
    }

    @Test
    void removeByOwnerReturnsDeleted() {
        Member owner = memberRepository.save(new Member("wish-owner@example.com", "pw"));
        Product product = persistProduct("c-wish-own", "p-wish-own");
        Wish wish = wishRepository.save(new Wish(owner, product));

        WishService.RemoveOutcome outcome = wishService.remove(owner, wish.getId());

        assertThat(outcome).isEqualTo(WishService.RemoveOutcome.DELETED);
        assertThat(wishRepository.findById(wish.getId())).isEmpty();
    }

    @Test
    void removeByNonOwnerReturnsForbidden() {
        Member owner = memberRepository.save(new Member("wish-other-owner@example.com", "pw"));
        Member intruder = memberRepository.save(new Member("wish-intruder@example.com", "pw"));
        Product product = persistProduct("c-wish-other", "p-wish-other");
        Wish wish = wishRepository.save(new Wish(owner, product));

        WishService.RemoveOutcome outcome = wishService.remove(intruder, wish.getId());

        assertThat(outcome).isEqualTo(WishService.RemoveOutcome.FORBIDDEN);
        assertThat(wishRepository.findById(wish.getId())).isPresent();
    }

    @Test
    void listReturnsMemberWishesWithPageable() {
        Member member = memberRepository.save(new Member("wish-list@example.com", "pw"));
        Product product = persistProduct("c-wish-list", "p-wish-list");
        wishRepository.save(new Wish(member, product));

        var page = wishService.list(member, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getProduct().getId()).isEqualTo(product.getId());
    }

    private Product persistProduct(String categoryName, String productName) {
        Category category = categoryRepository.save(
            new Category(categoryName, "#ffffff", "https://example.com/c.jpg", null));
        return productRepository.save(
            new Product(productName, 1000, "https://example.com/p.jpg", category));
    }
}
