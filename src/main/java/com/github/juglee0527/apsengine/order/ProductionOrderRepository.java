package com.github.juglee0527.apsengine.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionOrderRepository
        extends JpaRepository<ProductionOrder, Long> {

    boolean existsByOrderNumber(String orderNumber);

    @Override
    @EntityGraph(attributePaths = {"routing", "routing.product"})
    Optional<ProductionOrder> findById(Long productionOrderId);

    @EntityGraph(attributePaths = {"routing", "routing.product"})
    Page<ProductionOrder> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"routing", "routing.product"})
    @Query("""
            SELECT productionOrder
            FROM ProductionOrder productionOrder
            JOIN productionOrder.routing routing
            JOIN routing.product product
            WHERE (:query IS NULL
                   OR LOWER(productionOrder.orderNumber) LIKE CONCAT('%', :query, '%')
                   OR LOWER(product.code) LIKE CONCAT('%', :query, '%')
                   OR LOWER(product.name) LIKE CONCAT('%', :query, '%'))
              AND (:status IS NULL OR productionOrder.status = :status)
            """)
    Page<ProductionOrder> search(
            @Param("query") String query,
            @Param("status") ProductionOrderStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "routing",
            "routing.product",
            "routing.operations",
            "routing.operations.machine",
            "routing.operations.machineCandidates",
            "routing.operations.machineCandidates.machine"
    })
    @Query("""
            SELECT DISTINCT productionOrder
            FROM ProductionOrder productionOrder
            WHERE productionOrder.status = :status
            ORDER BY productionOrder.priority DESC,
                     productionOrder.dueAt ASC,
                     productionOrder.id ASC
            """)
    List<ProductionOrder> findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
            ProductionOrderStatus status
    );

    @EntityGraph(attributePaths = {
            "routing",
            "routing.product",
            "routing.operations",
            "routing.operations.machine",
            "routing.operations.machineCandidates",
            "routing.operations.machineCandidates.machine"
    })
    @Query("""
            SELECT DISTINCT productionOrder
            FROM ProductionOrder productionOrder
            WHERE productionOrder.id IN :ids
              AND productionOrder.status = :status
            ORDER BY productionOrder.priority DESC,
                     productionOrder.dueAt ASC,
                     productionOrder.id ASC
            """)
    List<ProductionOrder> findAllInScope(
            @Param("ids") List<Long> ids,
            @Param("status") ProductionOrderStatus status
    );
}
