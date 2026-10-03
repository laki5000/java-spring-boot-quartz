package com.example.proj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.core.scheduler.ISchedule;
import com.example.core.scheduler.ISchedulerService;
import com.example.core.scheduler.SchedulerTaskInput;
import com.example.proj.constant.SchedulerConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

@ExtendWith(MockitoExtension.class)
class SchedulerInitializerUnitTests {

  @Mock private ISchedulerService schedulerService;

  private SchedulerInitializer schedulerInitializer;

  @BeforeEach
  void setUp() {
    schedulerInitializer = new SchedulerInitializer(schedulerService);
  }

  @Test
  void testRun_shouldScheduleStartupTask_whenApplicationStarts() {
    // Given
    ApplicationArguments args = mock(ApplicationArguments.class);

    ArgumentCaptor<ISchedule> scheduleCaptor = ArgumentCaptor.forClass(ISchedule.class);
    ArgumentCaptor<SchedulerTaskInput> inputCaptor =
        ArgumentCaptor.forClass(SchedulerTaskInput.class);

    // When
    schedulerInitializer.run(args);

    // Then
    verify(schedulerService)
        .schedule(
            eq(SchedulerConstants.STARTUP_SCHEDULE_ID),
            eq(SchedulerConstants.STARTUP_TASK_ID),
            scheduleCaptor.capture(),
            inputCaptor.capture());

    ISchedule.Interval schedule =
        assertInstanceOf(ISchedule.Interval.class, scheduleCaptor.getValue());

    assertEquals(SchedulerConstants.STARTUP_INTERVAL_SECONDS, schedule.interval().toSeconds());

    assertEquals(SchedulerConstants.STARTUP_TASK_INPUT, inputCaptor.getValue().value());
  }
}
