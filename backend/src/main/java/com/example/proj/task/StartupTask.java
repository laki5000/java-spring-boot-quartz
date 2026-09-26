package com.example.proj.task;

import com.example.core.logging.LogExecution;
import com.example.core.scheduler.SchedulerTask;
import com.example.core.scheduler.SchedulerTaskInput;
import com.example.proj.constant.SchedulerConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

@Component(SchedulerConstants.STARTUP_TASK_ID)
@Slf4j
public class StartupTask implements SchedulerTask {

  @LogExecution(level = Level.INFO, logArguments = true)
  @Override
  public void execute(SchedulerTaskInput input) {
    log.info("Startup task executed with input: {}", input.as(String.class));
  }
}
