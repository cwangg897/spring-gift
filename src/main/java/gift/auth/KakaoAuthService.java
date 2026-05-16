package gift.auth;

import gift.member.Member;
import gift.member.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class KakaoAuthService {
    private static final String KAKAO_AUTHORIZE_URI = "https://kauth.kakao.com/oauth/authorize";
    private static final String KAKAO_LOGIN_SCOPE = "account_email,talk_message";

    private final KakaoLoginProperties properties;
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    public KakaoAuthService(
        KakaoLoginProperties properties,
        KakaoLoginClient kakaoLoginClient,
        MemberService memberService,
        JwtProvider jwtProvider
    ) {
        this.properties = properties;
        this.kakaoLoginClient = kakaoLoginClient;
        this.memberService = memberService;
        this.jwtProvider = jwtProvider;
    }

    public String buildLoginUrl() {
        return UriComponentsBuilder.fromUriString(KAKAO_AUTHORIZE_URI)
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("scope", KAKAO_LOGIN_SCOPE)
            .build()
            .toUriString();
    }

    public TokenResponse loginWithKakaoCode(String code) {
        final KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        final KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());

        final Member member = memberService.findOrCreateByKakao(kakaoUser.email(), kakaoToken.accessToken());

        final String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }
}
