package com.github.juglee0527.apsengine.product.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.github.juglee0527.apsengine.common.domain.BusinessCodeNormalizer;
import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.product.Product;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "routing",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_routing_product_code",
                columnNames = {"product_id", "routing_code"}
        )
)
public class Routing {

    static final int MAX_CODE_LENGTH = 50;
    static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routing_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(
            name = "routing_code",
            nullable = false,
            updatable = false,
            length = MAX_CODE_LENGTH
    )
    private String code;

    @Column(name = "routing_name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(
            mappedBy = "routing",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("sequence ASC")
    private List<Operation> operations = new ArrayList<>();

    protected Routing() {
    }

    private Routing(Product product, String code, String name) {
        this.product = Objects.requireNonNull(
                product,
                "product must not be null"
        );
        if (!product.isActive()) {
            throw new IllegalStateException(
                    "비활성 품목에는 Routing을 등록할 수 없습니다."
            );
        }
        this.code = BusinessCodeNormalizer.normalize(
                code,
                "Routing 코드",
                MAX_CODE_LENGTH
        );
        this.name = normalizeName(name);
        this.active = true;
    }

    public static Routing create(Product product, String code, String name) {
        return new Routing(product, code, name);
    }

    public void addOperation(
            int sequence,
            String code,
            String name,
            int processingTimeMinutes,
            Machine machine
    ) {
        validateOperationUniqueness(sequence, code);
        operations.add(Operation.create(
                this,
                machine,
                sequence,
                code,
                name,
                processingTimeMinutes
        ));
    }

    public Long id() {
        return id;
    }

    public Product product() {
        return product;
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

    public List<Operation> operations() {
        return Collections.unmodifiableList(operations);
    }

    private void validateOperationUniqueness(int sequence, String code) {
        String normalizedCode = BusinessCodeNormalizer.normalize(
                code,
                "Operation 코드",
                Operation.MAX_CODE_LENGTH
        );
        Set<Integer> sequences = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (Operation operation : operations) {
            sequences.add(operation.sequence());
            codes.add(operation.code());
        }
        if (sequences.contains(sequence)) {
            throw new IllegalArgumentException(
                    "Routing 안에서 Operation 순서는 중복될 수 없습니다."
            );
        }
        if (codes.contains(normalizedCode)) {
            throw new IllegalArgumentException(
                    "Routing 안에서 Operation 코드는 중복될 수 없습니다."
            );
        }
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Routing 이름은 필수입니다.");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Routing 이름은 100자를 초과할 수 없습니다."
            );
        }
        return normalizedName;
    }
}
