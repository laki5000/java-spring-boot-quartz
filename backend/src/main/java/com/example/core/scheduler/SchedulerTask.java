package com.example.core.scheduler;

@FunctionalInterface
public interface SchedulerTask {

  void execute(SchedulerTaskInput input);
}
