package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.support.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return findByIdOrThrow(id);
    }

    @Transactional
    public Product create(ProductRequest request) {
        validateName(request.name(), false);
        Category category = findCategoryOrThrow(request.categoryId());
        return productRepository.save(request.toEntity(category));
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        validateName(request.name(), false);
        Product product = findByIdOrThrow(id);
        Category category = findCategoryOrThrow(request.categoryId());
        product.update(request.name(), request.price(), request.imageUrl(), category);
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findByIdOrThrow(id);
        productRepository.delete(product);
    }

    @Transactional
    public Product createForAdmin(String name, int price, String imageUrl, Long categoryId) {
        validateName(name, true);
        Category category = findCategoryOrThrow(categoryId);
        return productRepository.save(new Product(name, price, imageUrl, category));
    }

    @Transactional
    public Product updateForAdmin(Long id, String name, int price, String imageUrl, Long categoryId) {
        Product product = findByIdOrThrow(id);
        validateName(name, true);
        Category category = findCategoryOrThrow(categoryId);
        product.update(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    public Product findByIdOrThrow(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found. id=" + id));
    }

    public List<String> validateNameOnly(String name, boolean allowKakao) {
        return ProductNameValidator.validate(name, allowKakao);
    }

    private Category findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NotFoundException("Category not found. id=" + categoryId));
    }

    private void validateName(String name, boolean allowKakao) {
        List<String> errors = ProductNameValidator.validate(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new ProductNameInvalidException(String.join(", ", errors));
        }
    }
}
