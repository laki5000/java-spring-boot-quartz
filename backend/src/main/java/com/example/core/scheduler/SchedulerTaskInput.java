package com.example.core.scheduler;

public record SchedulerTaskInput(Object value) {

  public <T> T as(Class<T> type) {
    return type.cast(value);
  }
}
