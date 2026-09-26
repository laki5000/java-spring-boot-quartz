package com.example.proj;

import com.example.core.scheduler.ISchedule;
import com.example.core.scheduler.ISchedulerService;
import com.example.core.scheduler.SchedulerTaskInput;
import com.example.proj.constant.SchedulerConstants;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchedulerInitializer implements ApplicationRunner {

  private final ISchedulerService schedulerService;

  @Override
  public void run(@NonNull ApplicationArguments args) {
    SchedulerTaskInput input = new SchedulerTaskInput(SchedulerConstants.STARTUP_TASK_INPUT);

    schedulerService.schedule(
        SchedulerConstants.STARTUP_SCHEDULE_ID,
        SchedulerConstants.STARTUP_TASK_ID,
        ISchedule.interval(
            Instant.now(), Duration.ofSeconds(SchedulerConstants.STARTUP_INTERVAL_SECONDS)),
        input);
  }
}
