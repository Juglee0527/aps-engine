package com.github.juglee0527.apsengine.product.routing;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutingRepository extends JpaRepository<Routing, Long> {

    boolean existsByProduct_IdAndCode(Long productId, String code);

    @EntityGraph(attributePaths = {
            "product",
            "operations",
            "operations.machine"
    })
    @Query("""
            select distinct routing
            from Routing routing
            where routing.product.id = :productId
            order by routing.id asc
            """)
    List<Routing> findAllDetailsByProductId(
            @Param("productId") Long productId
    );

    @EntityGraph(attributePaths = {
            "product",
            "operations",
            "operations.machine"
    })
    @Query("""
            select distinct routing
            from Routing routing
            where routing.id = :routingId
            """)
    Optional<Routing> findDetailById(@Param("routingId") Long routingId);
}
