package com.github.juglee0527.apsengine.factory.line;

import java.util.Locale;
import java.util.Objects;

import com.github.juglee0527.apsengine.factory.Factory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "production_line",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_production_line_factory_code",
                columnNames = {"factory_id", "line_code"}
        )
)
public class ProductionLine {

    static final int MAX_CODE_LENGTH = 50;
    static final int MAX_NAME_LENGTH = 100;

    private static final String CODE_PATTERN = "[A-Z0-9][A-Z0-9_-]*";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_line_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factory_id", nullable = false, updatable = false)
    private Factory factory;

    @Column(
            name = "line_code",
            nullable = false,
            updatable = false,
            length = MAX_CODE_LENGTH
    )
    private String code;

    @Column(name = "line_name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected ProductionLine() {
    }

    private ProductionLine(Factory factory, String code, String name) {
        this.factory = validateFactory(factory);
        this.code = normalizeCode(code);
        this.name = normalizeName(name);
        this.active = true;
    }

    public static ProductionLine create(
            Factory factory,
            String code,
            String name
    ) {
        return new ProductionLine(factory, code, name);
    }

    public Long id() {
        return id;
    }

    public Factory factory() {
        return factory;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    private static Factory validateFactory(Factory factory) {
        Objects.requireNonNull(factory, "factory must not be null");
        if (!factory.isActive()) {
            throw new IllegalStateException(
                    "비활성 공장에는 생산라인을 등록할 수 없습니다."
            );
        }
        return factory;
    }

    private static String normalizeCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("생산라인 코드는 필수입니다.");
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.isEmpty()) {
            throw new IllegalArgumentException("생산라인 코드는 필수입니다.");
        }
        if (normalizedCode.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "생산라인 코드는 50자를 초과할 수 없습니다."
            );
        }
        if (!normalizedCode.matches(CODE_PATTERN)) {
            throw new IllegalArgumentException(
                    "생산라인 코드는 영문, 숫자, 하이픈, 밑줄만 사용할 수 있습니다."
            );
        }
        return normalizedCode;
    }

    private static String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("생산라인 이름은 필수입니다.");
        }

        String normalizedName = name.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("생산라인 이름은 필수입니다.");
        }
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "생산라인 이름은 100자를 초과할 수 없습니다."
            );
        }
        return normalizedName;
    }
}
