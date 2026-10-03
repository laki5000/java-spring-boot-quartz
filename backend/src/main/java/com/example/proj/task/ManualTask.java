package com.example.proj.task;

import com.example.core.logging.LogExecution;
import com.example.core.scheduler.ISchedulerTask;
import com.example.core.scheduler.SchedulerTaskInput;
import com.example.proj.constant.SchedulerConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component(SchedulerConstants.MANUAL_TASK_ID)
@Slf4j
@RequiredArgsConstructor
public class ManualTask implements ISchedulerTask {

  private final ObjectMapper objectMapper;

  @LogExecution(level = Level.INFO, logArguments = true)
  @Override
  public void execute(SchedulerTaskInput input) {
    String value = objectMapper.convertValue(input.value(), String.class);
    log.info("Manual task executed with input: {}", value);
  }
}
