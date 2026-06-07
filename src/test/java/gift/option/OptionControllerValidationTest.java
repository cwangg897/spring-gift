package gift.option;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.category.Category;
import gift.category.CategoryRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OptionControllerValidationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createWithIllegalNameCharactersReturns400ViaGlobalAdvice() throws Exception {
        Product product = persistProductWithOption("opt-ctrl-bad", "p-ctrl-bad", "seed-ok");

        String body = objectMapper.writeValueAsString(
            new OptionRequest("@illegal!", 5));

        mockMvc.perform(post("/api/products/{productId}/options", product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getOptionsWithUnknownProductReturns404ViaGlobalAdvice() throws Exception {
        mockMvc.perform(get("/api/products/{productId}/options", 999_999L))
            .andExpect(status().isNotFound());
    }

    @Test
    void createWithUnknownProductReturns404ViaGlobalAdvice() throws Exception {
        String body = objectMapper.writeValueAsString(
            new OptionRequest("missing-product-option", 5));

        mockMvc.perform(post("/api/products/{productId}/options", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteWithUnknownOptionReturns404ViaGlobalAdvice() throws Exception {
        Product product = persistProductWithOption("opt-ctrl-missing", "p-ctrl-missing", "seed");

        mockMvc.perform(delete("/api/products/{productId}/options/{optionId}",
                product.getId(), 999_999L))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteLastOptionReturns422ViaGlobalAdvice() throws Exception {
        Product product = persistProductWithOption("opt-ctrl-last", "p-ctrl-last", "only-opt");
        Long onlyOptionId = optionRepository.findByProductId(product.getId()).get(0).getId();

        mockMvc.perform(delete("/api/products/{productId}/options/{optionId}",
                product.getId(), onlyOptionId))
            .andExpect(status().isUnprocessableEntity());
    }

    private Product persistProductWithOption(String categoryName, String productName, String seedOptionName) {
        Category category = categoryRepository.save(
            new Category(categoryName, "#ffffff", "https://example.com/c.jpg", null));
        Product product = productRepository.save(
            new Product(productName, 1000, "https://example.com/p.jpg", category));
        optionRepository.save(new Option(product, seedOptionName, 5));
        return product;
    }
}
