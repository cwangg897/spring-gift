package gift.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MemberControllerLoginTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUpMember() {
        if (!memberService.existsByEmail("controller-login@example.com")) {
            memberService.register(new MemberRequest("controller-login@example.com", "pw"));
        }
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String body = objectMapper.writeValueAsString(new MemberRequest("controller-login@example.com", "bad"));

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }
}
