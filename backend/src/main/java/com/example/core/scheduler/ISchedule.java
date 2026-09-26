package com.example.core.scheduler;

import com.example.core.exception.ValidationException;
import java.time.Duration;
import java.time.Instant;

public sealed interface ISchedule permits ISchedule.Once, ISchedule.Interval, ISchedule.Cron {

  record Once(Instant executionTime) implements ISchedule {

    public Once {
      if (executionTime == null) {
        throw new ValidationException("Execution time must not be null");
      }
    }
  }

  record Interval(Instant startTime, Duration interval) implements ISchedule {

    public Interval {
      if (startTime == null) {
        throw new ValidationException("Start time must not be null");
      }

      if (interval == null) {
        throw new ValidationException("Interval must not be null");
      }

      if (interval.isZero() || interval.isNegative()) {
        throw new ValidationException("Interval must be positive");
      }
    }
  }

  record Cron(String expression) implements ISchedule {

    public Cron {
      if (expression == null) {
        throw new ValidationException("Cron expression must not be null");
      }

      if (expression.isBlank()) {
        throw new ValidationException("Cron expression must not be blank");
      }
    }
  }

  static ISchedule onceAt(Instant executionTime) {
    return new Once(executionTime);
  }

  static ISchedule interval(Instant startTime, Duration interval) {
    return new Interval(startTime, interval);
  }

  static ISchedule cron(String expression) {
    return new Cron(expression);
  }
}
