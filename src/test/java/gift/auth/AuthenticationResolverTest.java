package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.support.AbstractIntegrationTest;
import gift.support.exception.AuthenticationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationResolverTest extends AbstractIntegrationTest {

    @Autowired
    private AuthenticationResolver authenticationResolver;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void extractMemberOrThrowReturnsMemberForValidToken() {
        Member saved = memberRepository.save(new Member("resolver@example.com"));
        String token = "Bearer " + jwtProvider.createToken(saved.getEmail());

        Member resolved = authenticationResolver.extractMemberOrThrow(token);

        assertThat(resolved.getEmail()).isEqualTo("resolver@example.com");
    }

    @Test
    void extractMemberOrThrowRaisesAuthenticationExceptionWhenTokenIsInvalid() {
        assertThatThrownBy(() -> authenticationResolver.extractMemberOrThrow("Bearer not-a-real-token"))
            .isInstanceOf(AuthenticationException.class);
    }
}
