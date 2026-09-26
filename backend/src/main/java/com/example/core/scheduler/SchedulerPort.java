package com.example.core.scheduler;

import java.util.List;

public interface SchedulerPort {

  void schedule(String scheduleId, String taskId, Schedule schedule, SchedulerTaskInput input);

  void cancel(String scheduleId);

  void pause(String scheduleId);

  void resume(String scheduleId);

  void trigger(String scheduleId);

  List<ScheduledTask> getSchedules();
}
