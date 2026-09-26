package com.example.core.scheduler;

import java.util.List;

public interface ISchedulerService {

  void schedule(String scheduleId, String taskId, ISchedule schedule, SchedulerTaskInput input);

  void cancel(String scheduleId);

  void pause(String scheduleId);

  void resume(String scheduleId);

  void trigger(String scheduleId);

  List<ScheduledTask> getSchedules();
}
