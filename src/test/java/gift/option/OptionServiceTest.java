package gift.option;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.support.AbstractIntegrationTest;
import gift.support.exception.DuplicateException;
import gift.support.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionServiceTest extends AbstractIntegrationTest {

    @Autowired
    private OptionService optionService;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void createPersistsOption() {
        Product product = persistProductWithOption("opt-svc-create", "p-svc", "seed");

        Option saved = optionService.create(product.getId(), new OptionRequest("svc-opt", 10));

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(optionRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void createThrowsNotFoundForUnknownProduct() {
        assertThatThrownBy(() -> optionService.create(999_999L, new OptionRequest("orphan", 1)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Product not found. id=999999");
    }

    @Test
    void createRejectsDuplicateName() {
        Product product = persistProductWithOption("opt-svc-dup", "p-dup", "dup-name");

        assertThatThrownBy(() -> optionService.create(product.getId(), new OptionRequest("dup-name", 1)))
            .isInstanceOf(DuplicateException.class)
            .hasMessageContaining("이미 존재");
    }

    @Test
    void findByProductIdThrowsNotFoundForUnknownProduct() {
        assertThatThrownBy(() -> optionService.findByProductId(999_999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Product not found. id=999999");
    }

    @Test
    void deleteThrowsNotFoundForUnknownProduct() {
        assertThatThrownBy(() -> optionService.delete(999_999L, 1L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Product not found. id=999999");
    }

    @Test
    void deleteThrowsNotFoundForUnknownOption() {
        Product product = persistProductWithOption("opt-svc-missing", "p-missing", "seed");

        assertThatThrownBy(() -> optionService.delete(product.getId(), 999_999L))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Option not found. id=999999");
    }

    @Test
    void deleteThrowsNotFoundForOptionOfDifferentProduct() {
        Product product = persistProductWithOption("opt-svc-owner", "p-owner", "owned");
        Product otherProduct = persistProductWithOption("opt-svc-other", "p-other", "other");
        Long otherOptionId = optionRepository.findByProductId(otherProduct.getId()).get(0).getId();

        assertThatThrownBy(() -> optionService.delete(product.getId(), otherOptionId))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Option not found. id=" + otherOptionId);
    }

    @Test
    void deleteRejectsWhenOnlyOneOptionRemains() {
        Product product = persistProductWithOption("opt-svc-last", "p-last", "last");
        Long onlyOptionId = optionRepository.findByProductId(product.getId()).get(0).getId();

        assertThatThrownBy(() -> optionService.delete(product.getId(), onlyOptionId))
            .isInstanceOf(LastOptionDeletionException.class)
            .hasMessageContaining("1개");
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
