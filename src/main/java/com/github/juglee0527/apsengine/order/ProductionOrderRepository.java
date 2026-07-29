package com.github.juglee0527.apsengine.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
            "routing.operations.machine"
    })
    List<ProductionOrder> findAllByStatusOrderByPriorityDescDueAtAscIdAsc(
            ProductionOrderStatus status
    );
}
