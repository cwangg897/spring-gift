package gift.wish;

import gift.member.Member;
import gift.product.Product;
import gift.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
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

    public AddOutcome add(Member member, Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return new AddOutcome(null, false);
        }
        Wish existing = wishRepository.findByMember_IdAndProduct_Id(member.getId(), productId).orElse(null);
        if (existing != null) {
            return new AddOutcome(existing, false);
        }
        Wish saved = wishRepository.save(new Wish(member, product));
        return new AddOutcome(saved, true);
    }

    public RemoveOutcome remove(Member member, Long wishId) {
        Wish wish = wishRepository.findById(wishId).orElse(null);
        if (wish == null) {
            return RemoveOutcome.NOT_FOUND;
        }
        if (!wish.getMember().getId().equals(member.getId())) {
            return RemoveOutcome.FORBIDDEN;
        }
        wishRepository.delete(wish);
        return RemoveOutcome.DELETED;
    }

    public boolean removeByMemberAndProduct(Member member, Long productId) {
        Wish wish = wishRepository.findByMember_IdAndProduct_Id(member.getId(), productId).orElse(null);
        if (wish == null) {
            return false;
        }
        wishRepository.delete(wish);
        return true;
    }

    public record AddOutcome(Wish wish, boolean newlyCreated) {
    }

    public enum RemoveOutcome {
        DELETED,
        NOT_FOUND,
        FORBIDDEN
    }
}
