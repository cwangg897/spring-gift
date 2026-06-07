package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import gift.support.exception.AuthenticationException;
import gift.support.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public TokenResponse register(MemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        final Member member = memberRepository.save(new Member(request.email(), request.password()));
        final String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    public TokenResponse authenticate(MemberRequest request) {
        final Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new AuthenticationException("Invalid email or password."));

        if (!member.matchesPassword(request.password())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        final String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Member not found. id=" + id));
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional
    public Member findOrCreateByKakao(String email, String accessToken) {
        final Member member = memberRepository.findByEmail(email)
            .orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(accessToken);
        return memberRepository.save(member);
    }

    @Transactional
    public Member createForAdmin(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        return memberRepository.save(new Member(email, password));
    }

    @Transactional
    public Member update(Long id, String email, String password) {
        final Member member = findById(id);
        member.update(email, password);
        return memberRepository.save(member);
    }

    @Transactional
    public Member chargePoint(Long id, int amount) {
        final Member member = findById(id);
        member.chargePoint(amount);
        return memberRepository.save(member);
    }

    @Transactional
    public void delete(Long id) {
        Member member = findById(id);
        memberRepository.delete(member);
    }
}
