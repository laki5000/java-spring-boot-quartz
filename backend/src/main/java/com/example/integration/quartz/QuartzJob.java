package com.example.integration.quartz;

import com.example.core.scheduler.SchedulerTask;
import com.example.core.scheduler.SchedulerTaskInput;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.context.ApplicationContext;

@RequiredArgsConstructor
public class QuartzJob implements Job {

  private final ApplicationContext applicationContext;

  @Override
  public void execute(JobExecutionContext context) {
    String taskId = context.getMergedJobDataMap().getString(QuartzConstants.TASK_ID_KEY);

    SchedulerTaskInput input =
        (SchedulerTaskInput) context.getMergedJobDataMap().get(QuartzConstants.TASK_INPUT_KEY);

    SchedulerTask task = applicationContext.getBean(taskId, SchedulerTask.class);

    task.execute(input);
  }
}
