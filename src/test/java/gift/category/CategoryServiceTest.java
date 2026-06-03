package gift.category;

import gift.product.Product;
import gift.product.ProductRepository;
import gift.support.AbstractIntegrationTest;
import gift.support.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryServiceTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void createPersistsCategory() {
        Category saved = categoryService.create(new CategoryRequest("cat-svc", "#aabbcc", "https://example.com/c.jpg", "desc"));

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("cat-svc");
    }

    @Test
    void updateThrowsNotFoundForUnknownId() {
        assertThatThrownBy(() -> categoryService.update(999_999L,
            new CategoryRequest("nope", "#000000", "https://example.com/x.jpg", null)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRejectsCategoryReferencedByProduct() {
        Category category = categoryRepository.save(
            new Category("c-ref", "#abcdef", "https://example.com/c.jpg", null));
        productRepository.save(new Product("p-ref", 1000, "https://example.com/p.jpg", category));

        assertThatThrownBy(() -> categoryService.delete(category.getId()))
            .isInstanceOf(CategoryInUseException.class);

        assertThat(categoryRepository.findById(category.getId())).isPresent();
    }
}
