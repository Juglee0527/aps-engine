package com.github.juglee0527.apsengine.product;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createsProduct() throws Exception {
        Product product = persistedProduct();
        when(productService.create(
                "product-01",
                "완제품 A",
                ProductUnit.PIECE
        )).thenReturn(product);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "product-01",
                                  "name": "완제품 A",
                                  "unit": "PIECE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/products/1"
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("PRODUCT-01"))
                .andExpect(jsonPath("$.unit").value("PIECE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void rejectsInvalidProductRequest() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "product 01",
                                  "name": "",
                                  "unit": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));

        verify(productService, never()).create(
                "product 01",
                "",
                null
        );
    }

    @Test
    void getsProductPage() throws Exception {
        Product product = persistedProduct();
        when(productService.getPage(0, 20)).thenReturn(new PageImpl<>(
                List.of(product),
                PageRequest.of(0, 20),
                1
        ));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("완제품 A"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private Product persistedProduct() {
        Product product =
                Product.create("PRODUCT-01", "완제품 A", ProductUnit.PIECE);
        ReflectionTestUtils.setField(product, "id", 1L);
        return product;
    }
}
