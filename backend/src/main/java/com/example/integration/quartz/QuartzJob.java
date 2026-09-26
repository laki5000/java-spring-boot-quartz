package com.example.integration.quartz;

import com.example.core.scheduler.ISchedulerTask;
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

    ISchedulerTask task = applicationContext.getBean(taskId, ISchedulerTask.class);

    task.execute(input);
  }
}
