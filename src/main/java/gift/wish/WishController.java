package gift.wish;

import gift.auth.AuthenticationResolver;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final WishService wishService;
    private final AuthenticationResolver authenticationResolver;

    public WishController(WishService wishService, AuthenticationResolver authenticationResolver) {
        this.wishService = wishService;
        this.authenticationResolver = authenticationResolver;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        Member member = authenticationResolver.extractMemberOrThrow(authorization);
        return ResponseEntity.ok(wishService.list(member, pageable).map(WishResponse::from));
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody WishRequest request
    ) {
        Member member = authenticationResolver.extractMemberOrThrow(authorization);
        WishService.AddOutcome outcome = wishService.add(member, request.productId());
        if (!outcome.newlyCreated()) {
            return ResponseEntity.ok(WishResponse.from(outcome.wish()));
        }
        return ResponseEntity.created(URI.create("/api/wishes/" + outcome.wish().getId()))
            .body(WishResponse.from(outcome.wish()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id
    ) {
        Member member = authenticationResolver.extractMemberOrThrow(authorization);
        wishService.remove(member, id);
        return ResponseEntity.noContent().build();
    }
}
