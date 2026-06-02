package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.support.exception.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationResolver {
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    public AuthenticationResolver(JwtProvider jwtProvider, MemberRepository memberRepository) {
        this.jwtProvider = jwtProvider;
        this.memberRepository = memberRepository;
    }

    public Member extractMemberOrThrow(String authorization) {
        try {
            final String token = authorization.replace("Bearer ", "");
            final String email = jwtProvider.getEmail(token);
            return memberRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid or missing authentication."));
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("Invalid or missing authentication.");
        }
    }
}
