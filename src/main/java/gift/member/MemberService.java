package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

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
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (member.getPassword() == null || !member.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        final String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found. id=" + id));
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member createForAdmin(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        return memberRepository.save(new Member(email, password));
    }

    public Member update(Long id, String email, String password) {
        final Member member = findById(id);
        member.update(email, password);
        return memberRepository.save(member);
    }

    public Member chargePoint(Long id, int amount) {
        final Member member = findById(id);
        member.chargePoint(amount);
        return memberRepository.save(member);
    }

    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
