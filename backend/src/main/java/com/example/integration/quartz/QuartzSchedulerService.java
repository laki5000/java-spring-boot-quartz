package com.example.integration.quartz;

import com.example.core.exception.SchedulerException;
import com.example.core.logging.LogExecution;
import com.example.core.scheduler.ISchedule;
import com.example.core.scheduler.ISchedulerService;
import com.example.core.scheduler.ScheduledTask;
import com.example.core.scheduler.SchedulerTaskInput;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
public class QuartzSchedulerService implements ISchedulerService {

  private final Scheduler quartzScheduler;
  private final ObjectMapper objectMapper;

  @LogExecution(logArguments = true)
  @Override
  public void schedule(
      String scheduleId, String taskId, ISchedule schedule, SchedulerTaskInput input) {
    try {
      String inputJson = objectMapper.writeValueAsString(input);

      JobDetail job =
          JobBuilder.newJob(QuartzJob.class)
              .withIdentity(scheduleId)
              .usingJobData(
                  new JobDataMap(
                      Map.of(
                          QuartzConstants.TASK_ID_KEY,
                          taskId,
                          QuartzConstants.TASK_INPUT_KEY,
                          inputJson)))
              .build();

      Trigger trigger = createTrigger(scheduleId, schedule);

      quartzScheduler.scheduleJob(job, trigger);
    } catch (org.quartz.SchedulerException exception) {
      throw new SchedulerException("Failed to schedule task: " + taskId, exception);
    }
  }

  @LogExecution(logArguments = true)
  @Override
  public void cancel(String scheduleId) {
    try {
      quartzScheduler.deleteJob(new JobKey(scheduleId));
    } catch (org.quartz.SchedulerException exception) {
      throw new SchedulerException("Failed to cancel schedule: " + scheduleId, exception);
    }
  }

  @LogExecution(logArguments = true)
  @Override
  public void pause(String scheduleId) {
    try {
      quartzScheduler.pauseJob(new JobKey(scheduleId));
    } catch (org.quartz.SchedulerException exception) {
      throw new SchedulerException("Failed to pause schedule: " + scheduleId, exception);
    }
  }

  @LogExecution(logResult = true)
  @Override
  public void resume(String scheduleId) {
    try {
      quartzScheduler.resumeJob(new JobKey(scheduleId));
    } catch (org.quartz.SchedulerException exception) {
      throw new SchedulerException("Failed to resume schedule: " + scheduleId, exception);
    }
  }

  @LogExecution(logResult = true)
  @Override
  public void trigger(String scheduleId) {
    try {
      quartzScheduler.triggerJob(new JobKey(scheduleId));
    } catch (org.quartz.SchedulerException exception) {
      throw new SchedulerException("Failed to trigger schedule: " + scheduleId, exception);
    }
  }

  @Override
  @LogExecution(logResult = true)
  public List<ScheduledTask> getSchedules() {
    try {
      return quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup()).stream()
          .map(this::getScheduledTask)
          .toList();
    } catch (org.quartz.SchedulerException exception) {
      throw new SchedulerException("Failed to get schedules", exception);
    }
  }

  private Trigger createTrigger(String scheduleId, ISchedule schedule) {
    TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(scheduleId);

    return switch (schedule) {
      case ISchedule.Once once -> triggerBuilder.startAt(Date.from(once.executionTime())).build();

      case ISchedule.Interval interval ->
          triggerBuilder
              .startAt(Date.from(interval.startTime()))
              .withSchedule(
                  SimpleScheduleBuilder.simpleSchedule()
                      .withIntervalInMilliseconds(interval.interval().toMillis())
                      .repeatForever())
              .build();

      case ISchedule.Cron cron ->
          triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cron.expression())).build();
    };
  }

  private ScheduledTask getScheduledTask(TriggerKey triggerKey) {
    try {
      Trigger trigger = quartzScheduler.getTrigger(triggerKey);

      return new ScheduledTask(
          triggerKey.getName(), getScheduleType(trigger), getScheduleStatus(trigger));
    } catch (org.quartz.SchedulerException exception) {
      throw new SchedulerException("Failed to get schedule: " + triggerKey.getName(), exception);
    }
  }

  private ScheduledTask.ScheduleType getScheduleType(Trigger trigger) {
    if (trigger instanceof CronTrigger) {
      return ScheduledTask.ScheduleType.CRON;
    }

    if (trigger instanceof SimpleTrigger simpleTrigger) {
      return simpleTrigger.getRepeatCount() == 0
          ? ScheduledTask.ScheduleType.ONCE
          : ScheduledTask.ScheduleType.INTERVAL;
    }

    throw new SchedulerException(
        "Unsupported Quartz trigger type: " + trigger.getClass().getSimpleName());
  }

  private ScheduledTask.ScheduleStatus getScheduleStatus(Trigger trigger) {
    try {
      return switch (quartzScheduler.getTriggerState(trigger.getKey())) {
        case NORMAL -> ScheduledTask.ScheduleStatus.SCHEDULED;
        case PAUSED -> ScheduledTask.ScheduleStatus.PAUSED;
        case BLOCKED -> ScheduledTask.ScheduleStatus.BLOCKED;
        case COMPLETE -> ScheduledTask.ScheduleStatus.COMPLETE;
        case ERROR -> ScheduledTask.ScheduleStatus.ERROR;
        case NONE ->
            throw new SchedulerException("Schedule not found: " + trigger.getKey().getName());
      };
    } catch (org.quartz.SchedulerException exception) {
      throw new SchedulerException(
          "Failed to get schedule status: " + trigger.getKey().getName(), exception);
    }
  }
}
