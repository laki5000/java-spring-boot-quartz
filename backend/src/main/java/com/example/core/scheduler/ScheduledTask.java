package com.example.core.scheduler;

public record ScheduledTask(String id, ScheduleType type, ScheduleStatus status) {

  public enum ScheduleType {
    ONCE,
    INTERVAL,
    CRON
  }

  public enum ScheduleStatus {
    SCHEDULED,
    PAUSED,
    BLOCKED,
    COMPLETE,
    ERROR
  }
}
