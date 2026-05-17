package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

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
        return productRepository.findById(id).orElse(null);
    }

    @Transactional
    public Product create(ProductRequest request) {
        validateName(request.name(), false);
        Category category = categoryRepository.findById(request.categoryId()).orElse(null);
        if (category == null) {
            return null;
        }
        return productRepository.save(request.toEntity(category));
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        validateName(request.name(), false);
        Category category = categoryRepository.findById(request.categoryId()).orElse(null);
        if (category == null) {
            return null;
        }
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return null;
        }
        product.update(request.name(), request.price(), request.imageUrl(), category);
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    @Transactional
    public Product createForAdmin(String name, int price, String imageUrl, Long categoryId) {
        validateName(name, true);
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));
        return productRepository.save(new Product(name, price, imageUrl, category));
    }

    @Transactional
    public Product updateForAdmin(Long id, String name, int price, String imageUrl, Long categoryId) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
        validateName(name, true);
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));
        product.update(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    public Product findByIdOrThrow(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
    }

    public List<String> validateNameOnly(String name, boolean allowKakao) {
        return ProductNameValidator.validate(name, allowKakao);
    }

    private void validateName(String name, boolean allowKakao) {
        List<String> errors = ProductNameValidator.validate(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new ProductNameInvalidException(String.join(", ", errors));
        }
    }
}
