package com.github.juglee0527.apsengine.factory;

import com.github.juglee0527.apsengine.common.domain.BusinessCodeNormalizer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "factory")
public class Factory {

    static final int MAX_CODE_LENGTH = 50;
    static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "factory_id")
    private Long id;

    @Column(
            name = "factory_code",
            nullable = false,
            updatable = false,
            length = MAX_CODE_LENGTH,
            unique = true
    )
    private String code;

    @Column(
            name = "factory_name",
            nullable = false,
            length = MAX_NAME_LENGTH
    )
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Factory() {
    }

    private Factory(String code, String name) {
        this.code = BusinessCodeNormalizer.normalize(
                code,
                "공장 코드",
                MAX_CODE_LENGTH
        );
        this.name = normalizeName(name);
        this.active = true;
    }

    public static Factory create(String code, String name) {
        return new Factory(code, name);
    }

    public void rename(String name) {
        this.name = normalizeName(name);
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
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

    public boolean isActive() {
        return active;
    }

    private static String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("공장 이름은 필수입니다.");
        }

        String normalizedName = name.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("공장 이름은 필수입니다.");
        }
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "공장 이름은 100자를 초과할 수 없습니다."
            );
        }
        return normalizedName;
    }
}
