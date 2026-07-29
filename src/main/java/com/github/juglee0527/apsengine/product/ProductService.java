package com.github.juglee0527.apsengine.product;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product create(String code, String name, ProductUnit unit) {
        Product product = Product.create(code, name, unit);
        if (productRepository.existsByCode(product.code())) {
            throw new ApplicationException(
                    ErrorCode.PRODUCT_CODE_DUPLICATED
            );
        }

        try {
            return productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            ErrorCode errorCode = ErrorCode.PRODUCT_CODE_DUPLICATED;
            throw new ApplicationException(
                    errorCode,
                    errorCode.defaultMessage(),
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public Product getById(long productId) {
        if (productId < 1) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "품목 ID는 1 이상이어야 합니다."
            );
        }
        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new ApplicationException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<Product> getPage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "페이지 조건이 올바르지 않습니다."
            );
        }
        return productRepository.findAll(PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        ));
    }
}
