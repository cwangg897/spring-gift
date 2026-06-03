package gift.wish;

import gift.member.Member;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.support.exception.AuthorizationException;
import gift.support.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    public Page<Wish> list(Member member, Pageable pageable) {
        return wishRepository.findByMember_Id(member.getId(), pageable);
    }

    @Transactional
    public AddOutcome add(Member member, Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NotFoundException("Product not found. id=" + productId));
        Wish existing = wishRepository.findByMember_IdAndProduct_Id(member.getId(), productId).orElse(null);
        if (existing != null) {
            return new AddOutcome(existing, false);
        }
        Wish saved = wishRepository.save(new Wish(member, product));
        return new AddOutcome(saved, true);
    }

    @Transactional
    public void remove(Member member, Long wishId) {
        Wish wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new NotFoundException("Wish not found. id=" + wishId));
        if (!wish.getMember().getId().equals(member.getId())) {
            throw new AuthorizationException("You can only delete your own wish.");
        }
        wishRepository.delete(wish);
    }

    @Transactional
    public void removeByMemberAndProduct(Member member, Long productId) {
        Wish wish = wishRepository.findByMember_IdAndProduct_Id(member.getId(), productId).orElse(null);
        if (wish == null) {
            return;
        }
        wishRepository.delete(wish);
    }

    public record AddOutcome(Wish wish, boolean newlyCreated) {
    }
}
