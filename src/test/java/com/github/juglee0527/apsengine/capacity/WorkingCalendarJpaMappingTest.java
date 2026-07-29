package com.github.juglee0527.apsengine.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.github.juglee0527.apsengine.factory.Factory;
import com.github.juglee0527.apsengine.factory.line.ProductionLine;
import com.github.juglee0527.apsengine.machine.Machine;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("local")
@SpringBootTest
@Transactional
@Rollback
@EnabledIfEnvironmentVariable(
        named = "APS_POSTGRES_INTEGRATION_TEST",
        matches = "true"
)
class WorkingCalendarJpaMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private WorkingCalendarRepository workingCalendarRepository;

    @Test
    void persistsAndLoadsMachineWorkingTime() {
        Factory factory = Factory.create("FACTORY-WC-MAP", "매핑 공장");
        entityManager.persist(factory);
        ProductionLine line =
                ProductionLine.create(factory, "LINE-WC", "매핑 라인");
        entityManager.persist(line);
        Machine machine = Machine.create(line, "MACHINE-WC", "매핑 설비");
        entityManager.persist(machine);
        WorkingCalendar calendar = WorkingCalendar.create(
                machine,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0)
        );
        entityManager.persist(calendar);
        entityManager.flush();
        entityManager.clear();

        List<WorkingCalendar> calendars = workingCalendarRepository
                .findAllByMachine_IdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                        machine.id()
                );
        entityManager.clear();
        WorkingCalendarResponse response =
                WorkingCalendarResponse.from(calendars.getFirst());

        assertThat(response.machineId()).isEqualTo(machine.id());
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }
}
