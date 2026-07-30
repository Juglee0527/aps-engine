package com.github.juglee0527.apsengine.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductionOrderRepository
        extends JpaRepository<ProductionOrder, Long> {

    boolean existsByOrderNumber(String orderNumber);

    @Override
    @EntityGraph(attributePaths = {"routing", "routing.product"})
    Optional<ProductionOrder> findById(Long productionOrderId);

    @EntityGraph(attributePaths = {"routing", "routing.product"})
    Page<ProductionOrder> findAllBy(Pageable pageable);

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
}
