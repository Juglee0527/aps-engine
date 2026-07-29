package com.github.juglee0527.apsengine.capacity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

import com.github.juglee0527.apsengine.machine.Machine;

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
        name = "working_calendar",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_working_calendar_machine_window",
                columnNames = {
                        "machine_id",
                        "day_of_week",
                        "start_time",
                        "end_time"
                }
        )
)
public class WorkingCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "working_calendar_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false, updatable = false)
    private Machine machine;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected WorkingCalendar() {
    }

    private WorkingCalendar(
            Machine machine,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.machine = Objects.requireNonNull(
                machine,
                "machine must not be null"
        );
        WeeklyWorkingTime workingTime =
                new WeeklyWorkingTime(dayOfWeek, startTime, endTime);
        this.dayOfWeek = workingTime.dayOfWeek();
        this.startTime = workingTime.startTime();
        this.endTime = workingTime.endTime();
        this.active = true;
    }

    public static WorkingCalendar create(
            Machine machine,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return new WorkingCalendar(
                machine,
                dayOfWeek,
                startTime,
                endTime
        );
    }

    public Long id() {
        return id;
    }

    public Machine machine() {
        return machine;
    }

    public DayOfWeek dayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime startTime() {
        return startTime;
    }

    public LocalTime endTime() {
        return endTime;
    }

    public boolean isActive() {
        return active;
    }

    public WeeklyWorkingTime toWeeklyWorkingTime() {
        return new WeeklyWorkingTime(dayOfWeek, startTime, endTime);
    }
}
