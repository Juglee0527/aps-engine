package com.github.juglee0527.apsengine.order;

import java.time.OffsetDateTime;
import java.util.Objects;

import com.github.juglee0527.apsengine.common.domain.BusinessCodeNormalizer;
import com.github.juglee0527.apsengine.product.routing.Routing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "production_order",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_production_order_number",
                columnNames = "order_number"
        )
)
public class ProductionOrder {

    static final int MAX_ORDER_NUMBER_LENGTH = 50;
    static final long MAX_QUANTITY = 1_000_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routing_id", nullable = false, updatable = false)
    private Routing routing;

    @Column(
            name = "order_number",
            nullable = false,
            updatable = false,
            length = MAX_ORDER_NUMBER_LENGTH
    )
    private String orderNumber;

    @Column(name = "quantity", nullable = false, updatable = false)
    private long quantity;

    @Column(name = "release_at", nullable = false, updatable = false)
    private OffsetDateTime releaseAt;

    @Column(name = "due_at", nullable = false, updatable = false)
    private OffsetDateTime dueAt;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductionOrderStatus status;

    protected ProductionOrder() {
    }

    private ProductionOrder(
            Routing routing,
            String orderNumber,
            long quantity,
            OffsetDateTime releaseAt,
            OffsetDateTime dueAt,
            int priority
    ) {
        this.routing = validateRouting(routing);
        this.orderNumber = BusinessCodeNormalizer.normalize(
                orderNumber,
                "생산오더 번호",
                MAX_ORDER_NUMBER_LENGTH
        );
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException(
                    "생산수량은 1 이상 1000000 이하여야 합니다."
            );
        }
        this.quantity = quantity;
        this.releaseAt = Objects.requireNonNull(
                releaseAt,
                "releaseAt must not be null"
        );
        this.dueAt = Objects.requireNonNull(
                dueAt,
                "dueAt must not be null"
        );
        if (!dueAt.isAfter(releaseAt)) {
            throw new IllegalArgumentException(
                    "납기시각은 투입 가능 시각보다 이후여야 합니다."
            );
        }
        if (priority < 1 || priority > 100) {
            throw new IllegalArgumentException(
                    "우선순위는 1 이상 100 이하여야 합니다."
            );
        }
        this.priority = priority;
        this.status = ProductionOrderStatus.DRAFT;
    }

    public static ProductionOrder create(
            Routing routing,
            String orderNumber,
            long quantity,
            OffsetDateTime releaseAt,
            OffsetDateTime dueAt,
            int priority
    ) {
        return new ProductionOrder(
                routing,
                orderNumber,
                quantity,
                releaseAt,
                dueAt,
                priority
        );
    }

    public void confirm() {
        requireStatus(ProductionOrderStatus.DRAFT);
        status = ProductionOrderStatus.CONFIRMED;
    }

    public void markScheduled() {
        requireStatus(ProductionOrderStatus.CONFIRMED);
        status = ProductionOrderStatus.SCHEDULED;
    }

    public void cancel() {
        if (status == ProductionOrderStatus.CANCELLED) {
            return;
        }
        status = ProductionOrderStatus.CANCELLED;
    }

    public Long id() {
        return id;
    }

    public Routing routing() {
        return routing;
    }

    public String orderNumber() {
        return orderNumber;
    }

    public long quantity() {
        return quantity;
    }

    public OffsetDateTime releaseAt() {
        return releaseAt;
    }

    public OffsetDateTime dueAt() {
        return dueAt;
    }

    public int priority() {
        return priority;
    }

    public ProductionOrderStatus status() {
        return status;
    }

    private static Routing validateRouting(Routing routing) {
        Objects.requireNonNull(routing, "routing must not be null");
        if (!routing.isActive()) {
            throw new IllegalStateException(
                    "비활성 Routing으로 생산오더를 생성할 수 없습니다."
            );
        }
        if (routing.operations().isEmpty()) {
            throw new IllegalStateException(
                    "Operation이 없는 Routing으로 생산오더를 생성할 수 없습니다."
            );
        }
        return routing;
    }

    private void requireStatus(ProductionOrderStatus required) {
        if (status != required) {
            throw new IllegalStateException(
                    "생산오더 상태를 %s에서 변경할 수 없습니다."
                            .formatted(status)
            );
        }
    }
}
