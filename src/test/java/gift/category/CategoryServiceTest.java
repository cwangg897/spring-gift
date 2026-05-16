package gift.category;

import gift.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryServiceTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Test
    void createPersistsCategory() {
        Category saved = categoryService.create(new CategoryRequest("cat-svc", "#aabbcc", "https://example.com/c.jpg", "desc"));

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("cat-svc");
    }

    @Test
    void updateReturnsNullForUnknownId() {
        Category updated = categoryService.update(999_999L,
            new CategoryRequest("nope", "#000000", "https://example.com/x.jpg", null));

        assertThat(updated).isNull();
    }
}
