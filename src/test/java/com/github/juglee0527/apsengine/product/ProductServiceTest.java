package com.github.juglee0527.apsengine.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createsProduct() {
        when(productRepository.existsByCode("PRODUCT-01")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Product product = productService.create(
                "product-01",
                "완제품 A",
                ProductUnit.PIECE
        );

        assertThat(product.code()).isEqualTo("PRODUCT-01");
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void rejectsDuplicatedCode() {
        when(productRepository.existsByCode("PRODUCT-01")).thenReturn(true);

        assertErrorCode(
                () -> productService.create(
                        "product-01",
                        "완제품 A",
                        ProductUnit.PIECE
                ),
                ErrorCode.PRODUCT_CODE_DUPLICATED
        );

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void getsProductById() {
        Product product =
                Product.create("PRODUCT-01", "완제품 A", ProductUnit.PIECE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThat(productService.getById(1L)).isSameAs(product);
    }

    @Test
    void rejectsMissingProduct() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertErrorCode(
                () -> productService.getById(999L),
                ErrorCode.PRODUCT_NOT_FOUND
        );
    }

    private void assertErrorCode(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            ErrorCode expected
    ) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(expected)
                );
    }
}
