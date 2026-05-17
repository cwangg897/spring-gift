package gift.wish;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.support.AbstractIntegrationTest;
import gift.support.exception.AuthorizationException;
import gift.support.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void addThrowsNotFoundForUnknownProduct() {
        Member member = memberRepository.save(new Member("wish-none@example.com", "pw"));

        assertThatThrownBy(() -> wishService.add(member, 999_999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Product");
    }

    @Test
    void removeByOwnerDeletesWish() {
        Member owner = memberRepository.save(new Member("wish-owner@example.com", "pw"));
        Product product = persistProduct("c-wish-own", "p-wish-own");
        Wish wish = wishRepository.save(new Wish(owner, product));

        wishService.remove(owner, wish.getId());

        assertThat(wishRepository.findById(wish.getId())).isEmpty();
    }

    @Test
    void removeByNonOwnerThrowsForbidden() {
        Member owner = memberRepository.save(new Member("wish-other-owner@example.com", "pw"));
        Member intruder = memberRepository.save(new Member("wish-intruder@example.com", "pw"));
        Product product = persistProduct("c-wish-other", "p-wish-other");
        Wish wish = wishRepository.save(new Wish(owner, product));

        assertThatThrownBy(() -> wishService.remove(intruder, wish.getId()))
            .isInstanceOf(AuthorizationException.class);

        assertThat(wishRepository.findById(wish.getId())).isPresent();
    }

    @Test
    void removeUnknownWishThrowsNotFound() {
        Member member = memberRepository.save(new Member("wish-missing@example.com", "pw"));

        assertThatThrownBy(() -> wishService.remove(member, 999_999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Wish");
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
