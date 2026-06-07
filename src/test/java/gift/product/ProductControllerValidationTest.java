package gift.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.category.Category;
import gift.category.CategoryRepository;
import gift.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ProductControllerValidationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createWithKakaoNameReturns400ViaGlobalAdvice() throws Exception {
        Category category = categoryRepository.save(
            new Category("c-rest", "#112233", "https://example.com/c.jpg", null));

        String body = objectMapper.writeValueAsString(
            new ProductRequest("카카오선물", 1000, "https://example.com/p.jpg", category.getId()));

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getUnknownProductReturns404ViaGlobalAdvice() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999_999L))
            .andExpect(status().isNotFound());
    }
}
