package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class KakaoAuthServiceTest extends AbstractIntegrationTest {

    @Autowired
    private KakaoAuthService kakaoAuthService;

    @Autowired
    private MemberRepository memberRepository;

    @MockBean
    private KakaoLoginClient kakaoLoginClient;

    @Test
    void buildLoginUrlIncludesKakaoAuthorizeEndpointAndScope() {
        String url = kakaoAuthService.buildLoginUrl();

        assertThat(url).startsWith("https://kauth.kakao.com/oauth/authorize");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("scope=account_email,talk_message");
    }

    @Test
    void loginWithKakaoCodeCreatesMemberAndReturnsToken() {
        when(kakaoLoginClient.requestAccessToken(anyString()))
            .thenReturn(new KakaoLoginClient.KakaoTokenResponse("kakao-access-token"));
        when(kakaoLoginClient.requestUserInfo("kakao-access-token"))
            .thenReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("newkakao@example.com")));

        TokenResponse response = kakaoAuthService.loginWithKakaoCode("auth-code");

        assertThat(response.token()).isNotBlank();
        Member saved = memberRepository.findByEmail("newkakao@example.com").orElseThrow();
        assertThat(saved.getKakaoAccessToken()).isEqualTo("kakao-access-token");
    }

    @Test
    void loginWithKakaoCodeRefreshesTokenForExistingMember() {
        memberRepository.save(new Member("existing@example.com"));

        when(kakaoLoginClient.requestAccessToken(anyString()))
            .thenReturn(new KakaoLoginClient.KakaoTokenResponse("refreshed-token"));
        when(kakaoLoginClient.requestUserInfo(anyString()))
            .thenReturn(new KakaoLoginClient.KakaoUserResponse(
                new KakaoLoginClient.KakaoUserResponse.KakaoAccount("existing@example.com")));

        kakaoAuthService.loginWithKakaoCode("auth-code");

        Member updated = memberRepository.findByEmail("existing@example.com").orElseThrow();
        assertThat(updated.getKakaoAccessToken()).isEqualTo("refreshed-token");
    }

    @Test
    void loginWithKakaoCodePropagatesKakaoLoginExceptionWhenTokenExchangeFails() {
        when(kakaoLoginClient.requestAccessToken(anyString()))
            .thenThrow(new KakaoLoginException("simulated token failure",
                new RestClientException("downstream")));

        assertThatThrownBy(() -> kakaoAuthService.loginWithKakaoCode("bad-code"))
            .isInstanceOf(KakaoLoginException.class)
            .hasMessageContaining("simulated token failure");
    }

    @Test
    void loginWithKakaoCodeRollsBackMemberCreationWhenUserInfoFails() {
        when(kakaoLoginClient.requestAccessToken(anyString()))
            .thenReturn(new KakaoLoginClient.KakaoTokenResponse("token-then-fail"));
        when(kakaoLoginClient.requestUserInfo(anyString()))
            .thenThrow(new KakaoLoginException("simulated user info failure",
                new RestClientException("downstream")));

        assertThatThrownBy(() -> kakaoAuthService.loginWithKakaoCode("auth-code"))
            .isInstanceOf(KakaoLoginException.class);
    }
}
