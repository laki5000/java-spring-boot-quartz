package com.example.core.scheduler;

@FunctionalInterface
public interface ISchedulerTask {

  void execute(SchedulerTaskInput input);
}
