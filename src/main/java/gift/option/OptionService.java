package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import gift.support.exception.DuplicateException;
import gift.support.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    public OptionService(OptionRepository optionRepository, ProductRepository productRepository) {
        this.optionRepository = optionRepository;
        this.productRepository = productRepository;
    }

    public List<Option> findByProductId(Long productId) {
        findProductOrThrow(productId);
        return optionRepository.findByProductId(productId);
    }

    public Option findByIdOrThrow(Long optionId) {
        return optionRepository.findById(optionId)
            .orElseThrow(() -> new NotFoundException("Option not found. id=" + optionId));
    }

    @Transactional
    public Option create(Long productId, OptionRequest request) {
        Product product = findProductOrThrow(productId);
        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new DuplicateException("이미 존재하는 옵션명입니다.");
        }
        return optionRepository.save(new Option(product, request.name(), request.quantity()));
    }

    @Transactional
    public void delete(Long productId, Long optionId) {
        findProductOrThrow(productId);
        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new NotFoundException("Option not found. id=" + optionId));
        if (!option.getProduct().getId().equals(productId)) {
            throw new NotFoundException("Option not found. id=" + optionId);
        }
        if (optionRepository.findByProductId(productId).size() <= 1) {
            throw new LastOptionDeletionException(
                "옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }
        optionRepository.delete(option);
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new NotFoundException("Product not found. id=" + productId));
    }
}
