package gift.option;

import gift.product.Product;
import gift.product.ProductRepository;
import gift.support.exception.DuplicateException;
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
        if (!productRepository.existsById(productId)) {
            return null;
        }
        return optionRepository.findByProductId(productId);
    }

    @Transactional
    public Option create(Long productId, OptionRequest request) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return null;
        }
        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new DuplicateException("이미 존재하는 옵션명입니다.");
        }
        return optionRepository.save(new Option(product, request.name(), request.quantity()));
    }

    @Transactional
    public boolean delete(Long productId, Long optionId) {
        if (!productRepository.existsById(productId)) {
            return false;
        }
        Option option = optionRepository.findById(optionId).orElse(null);
        if (option == null || !option.getProduct().getId().equals(productId)) {
            return false;
        }
        if (optionRepository.findByProductId(productId).size() <= 1) {
            throw new LastOptionDeletionException(
                "옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        }
        optionRepository.delete(option);
        return true;
    }
}
