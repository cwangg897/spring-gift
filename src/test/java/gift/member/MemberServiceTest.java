package gift.member;

import gift.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberServiceTest extends AbstractIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void registerIssuesTokenAndPersistsMember() {
        MemberRequest request = new MemberRequest("new-user@example.com", "pw");

        var response = memberService.register(request);

        assertThat(response.token()).isNotBlank();
        assertThat(memberRepository.existsByEmail("new-user@example.com")).isTrue();
    }

    @Test
    void registerRejectsDuplicateEmail() {
        memberService.register(new MemberRequest("dup@example.com", "pw"));

        assertThatThrownBy(() -> memberService.register(new MemberRequest("dup@example.com", "pw")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already registered");
    }

    @Test
    void authenticateReturnsTokenForValidCredentials() {
        memberService.register(new MemberRequest("login@example.com", "pw"));

        var response = memberService.authenticate(new MemberRequest("login@example.com", "pw"));

        assertThat(response.token()).isNotBlank();
    }

    @Test
    void authenticateRejectsWrongPassword() {
        memberService.register(new MemberRequest("wrongpw@example.com", "pw"));

        assertThatThrownBy(() -> memberService.authenticate(new MemberRequest("wrongpw@example.com", "bad")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email or password");
    }
}
