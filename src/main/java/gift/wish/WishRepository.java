package gift.wish;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishRepository extends JpaRepository<Wish, Long> {

    @EntityGraph(attributePaths = {"product"})
    Page<Wish> findByMember_Id(Long memberId, Pageable pageable);

    Optional<Wish> findByMember_IdAndProduct_Id(Long memberId, Long productId);
}
