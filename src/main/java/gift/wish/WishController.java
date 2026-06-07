package gift.wish;

import gift.auth.LoginMember;
import gift.member.Member;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final WishService wishService;

    public WishController(WishService wishService) {
        this.wishService = wishService;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @LoginMember Member member,
        Pageable pageable
    ) {
        return ResponseEntity.ok(wishService.list(member, pageable).map(WishResponse::from));
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @LoginMember Member member,
        @Valid @RequestBody WishRequest request
    ) {
        WishService.AddOutcome outcome = wishService.add(member, request.productId());
        if (!outcome.newlyCreated()) {
            return ResponseEntity.ok(WishResponse.from(outcome.wish()));
        }
        return ResponseEntity.created(URI.create("/api/wishes/" + outcome.wish().getId()))
            .body(WishResponse.from(outcome.wish()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @LoginMember Member member,
        @PathVariable Long id
    ) {
        wishService.remove(member, id);
        return ResponseEntity.noContent().build();
    }
}
