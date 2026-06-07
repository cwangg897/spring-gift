package gift.category;

import gift.product.ProductRepository;
import gift.support.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category create(CategoryRequest request) {
        return categoryRepository.save(request.toEntity());
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = findByIdOrThrow(id);
        category.update(request.name(), request.color(), request.imageUrl(), request.description());
        return category;
    }

    @Transactional
    public void delete(Long id) {
        Category category = findByIdOrThrow(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new CategoryInUseException(
                "카테고리에 등록된 상품이 있어 삭제할 수 없습니다. id=" + id);
        }
        categoryRepository.delete(category);
    }

    private Category findByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found. id=" + id));
    }
}
