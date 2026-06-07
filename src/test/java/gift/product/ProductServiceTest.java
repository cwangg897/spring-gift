package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.support.AbstractIntegrationTest;
import gift.support.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void createPersistsProductWithCategory() {
        Category category = categoryRepository.save(new Category("c-svc", "#ffffff", "https://example.com/i.jpg", null));

        Product saved = productService.create(new ProductRequest("svc-prod", 1000, "https://example.com/p.jpg", category.getId()));

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(productRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void createThrowsNotFoundForUnknownCategory() {
        assertThatThrownBy(() -> productService.create(
            new ProductRequest("orphan-prod", 1000, "https://example.com/p.jpg", 999_999L)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Category");
    }

    @Test
    void findByIdThrowsNotFoundForUnknownId() {
        assertThatThrownBy(() -> productService.findById(999_999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Product not found. id=999999");
    }

    @Test
    void deleteThrowsNotFoundForUnknownId() {
        assertThatThrownBy(() -> productService.delete(999_999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Product not found. id=999999");
    }

    @Test
    void createRejectsInvalidName() {
        Category category = categoryRepository.save(new Category("c-bad", "#000000", "https://example.com/i.jpg", null));

        assertThatThrownBy(() -> productService.create(new ProductRequest("카카오선물", 1000, "https://example.com/p.jpg", category.getId())))
            .isInstanceOf(ProductNameInvalidException.class)
            .hasMessageContaining("카카오");
    }
}
