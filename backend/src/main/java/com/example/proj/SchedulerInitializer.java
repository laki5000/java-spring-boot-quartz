package com.example.proj;

import com.example.core.scheduler.Schedule;
import com.example.core.scheduler.SchedulerPort;
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

  private final SchedulerPort schedulerPort;

  @Override
  public void run(@NonNull ApplicationArguments args) {
    SchedulerTaskInput input = new SchedulerTaskInput(SchedulerConstants.STARTUP_TASK_INPUT);

    schedulerPort.schedule(
        SchedulerConstants.STARTUP_SCHEDULE_ID,
        SchedulerConstants.STARTUP_TASK_ID,
        Schedule.interval(
            Instant.now(), Duration.ofSeconds(SchedulerConstants.STARTUP_INTERVAL_SECONDS)),
        input);
  }
}
