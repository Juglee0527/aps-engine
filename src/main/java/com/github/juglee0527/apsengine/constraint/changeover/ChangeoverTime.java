package com.github.juglee0527.apsengine.constraint.changeover;

import java.util.Objects;

import com.github.juglee0527.apsengine.machine.Machine;
import com.github.juglee0527.apsengine.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "changeover_time",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_changeover_time_machine_products",
                columnNames = {
                        "machine_id",
                        "from_product_id",
                        "to_product_id"
                }
        ),
        indexes = {
                @Index(
                        name = "ix_changeover_time_machine_id",
                        columnList = "machine_id"
                ),
                @Index(
                        name = "ix_changeover_time_product_pair",
                        columnList = "from_product_id,to_product_id"
                )
        }
)
public class ChangeoverTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "changeover_time_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, updatable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_product_id", nullable = false, updatable = false)
    private Product fromProduct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_product_id", nullable = false, updatable = false)
    private Product toProduct;

    @Column(name = "changeover_minutes", nullable = false)
    private int changeoverMinutes;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected ChangeoverTime() {
    }

    private ChangeoverTime(
            Machine machine,
            Product fromProduct,
            Product toProduct,
            int changeoverMinutes
    ) {
        this.machine = Objects.requireNonNull(
                machine,
                "machine must not be null"
        );
        this.fromProduct = Objects.requireNonNull(
                fromProduct,
                "fromProduct must not be null"
        );
        this.toProduct = Objects.requireNonNull(
                toProduct,
                "toProduct must not be null"
        );
        if (sameProduct(fromProduct, toProduct)) {
            throw new IllegalArgumentException(
                    "동일 품목 사이에는 Changeover Time을 등록할 수 없습니다."
            );
        }
        if (changeoverMinutes < 0) {
            throw new IllegalArgumentException(
                    "Changeover Time은 0분 이상이어야 합니다."
            );
        }
        this.changeoverMinutes = changeoverMinutes;
        this.active = true;
    }

    public static ChangeoverTime create(
            Machine machine,
            Product fromProduct,
            Product toProduct,
            int changeoverMinutes
    ) {
        return new ChangeoverTime(
                machine,
                fromProduct,
                toProduct,
                changeoverMinutes
        );
    }

    public Long id() {
        return id;
    }

    public Machine machine() {
        return machine;
    }

    public Product fromProduct() {
        return fromProduct;
    }

    public Product toProduct() {
        return toProduct;
    }

    public int changeoverMinutes() {
        return changeoverMinutes;
    }

    public boolean isActive() {
        return active;
    }

    private static boolean sameProduct(Product left, Product right) {
        if (left == right) {
            return true;
        }
        return left.id() != null && left.id().equals(right.id());
    }
}
