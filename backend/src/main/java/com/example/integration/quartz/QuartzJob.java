package com.example.integration.quartz;

import com.example.core.scheduler.ISchedulerTask;
import com.example.core.scheduler.SchedulerTaskInput;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.context.ApplicationContext;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class QuartzJob implements Job {

  private final ApplicationContext applicationContext;
  private final ObjectMapper objectMapper;

  @Override
  public void execute(JobExecutionContext context) {
    String taskId = context.getMergedJobDataMap().getString(QuartzConstants.TASK_ID_KEY);
    String inputJson = context.getMergedJobDataMap().getString(QuartzConstants.TASK_INPUT_KEY);

    ISchedulerTask task = applicationContext.getBean(taskId, ISchedulerTask.class);

    try {
      SchedulerTaskInput input = objectMapper.readValue(inputJson, SchedulerTaskInput.class);
      task.execute(input);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Failed to deserialize task input: " + taskId, exception);
    }
  }
}
