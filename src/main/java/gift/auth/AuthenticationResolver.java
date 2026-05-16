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

    public Member extractMember(String authorization) {
        try {
            final String token = authorization.replace("Bearer ", "");
            final String email = jwtProvider.getEmail(token);
            return memberRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public Member extractMemberOrThrow(String authorization) {
        final Member member = extractMember(authorization);
        if (member == null) {
            throw new AuthenticationException("Invalid or missing authentication.");
        }
        return member;
    }
}
