package com.github.juglee0527.apsengine.constraint.changeover;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChangeoverTimeRepository
        extends JpaRepository<ChangeoverTime, Long> {

    boolean existsByMachine_IdAndFromProduct_IdAndToProduct_Id(
            Long machineId,
            Long fromProductId,
            Long toProductId
    );

    @EntityGraph(attributePaths = {
            "machine",
            "fromProduct",
            "toProduct"
    })
    List<ChangeoverTime>
    findAllByMachine_IdAndActiveTrueOrderByFromProduct_IdAscToProduct_IdAsc(
            Long machineId
    );

    Optional<ChangeoverTime>
    findByMachine_IdAndFromProduct_IdAndToProduct_IdAndActiveTrue(
            Long machineId,
            Long fromProductId,
            Long toProductId
    );

    @EntityGraph(attributePaths = {
            "machine",
            "fromProduct",
            "toProduct"
    })
    @Query("""
            select changeoverTime
            from ChangeoverTime changeoverTime
            where changeoverTime.id = :changeoverTimeId
              and changeoverTime.active = true
            """)
    Optional<ChangeoverTime> findActiveDetailById(
            @Param("changeoverTimeId") Long changeoverTimeId
    );
}
