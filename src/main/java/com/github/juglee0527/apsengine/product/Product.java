package com.github.juglee0527.apsengine.product;

import java.util.Objects;

import com.github.juglee0527.apsengine.common.domain.BusinessCodeNormalizer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "product",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_code",
                columnNames = "product_code"
        )
)
public class Product {

    static final int MAX_CODE_LENGTH = 50;
    static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(
            name = "product_code",
            nullable = false,
            updatable = false,
            length = MAX_CODE_LENGTH
    )
    private String code;

    @Column(name = "product_name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    private ProductUnit unit;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Product() {
    }

    private Product(String code, String name, ProductUnit unit) {
        this.code = BusinessCodeNormalizer.normalize(
                code,
                "품목 코드",
                MAX_CODE_LENGTH
        );
        this.name = normalizeName(name);
        this.unit = Objects.requireNonNull(unit, "unit must not be null");
        this.active = true;
    }

    public static Product create(
            String code,
            String name,
            ProductUnit unit
    ) {
        return new Product(code, name, unit);
    }

    public Long id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public ProductUnit unit() {
        return unit;
    }

    public boolean isActive() {
        return active;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("품목 이름은 필수입니다.");
        }

        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "품목 이름은 100자를 초과할 수 없습니다."
            );
        }
        return normalizedName;
    }
}
