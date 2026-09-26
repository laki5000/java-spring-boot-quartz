package com.example.core.scheduler;

import com.example.core.exception.ValidationException;
import java.time.Duration;
import java.time.Instant;

public sealed interface Schedule permits Schedule.Once, Schedule.Interval, Schedule.Cron {

  record Once(Instant executionTime) implements Schedule {

    public Once {
      if (executionTime == null) {
        throw new ValidationException("Execution time must not be null");
      }
    }
  }

  record Interval(Instant startTime, Duration interval) implements Schedule {

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

  record Cron(String expression) implements Schedule {

    public Cron {
      if (expression == null) {
        throw new ValidationException("Cron expression must not be null");
      }

      if (expression.isBlank()) {
        throw new ValidationException("Cron expression must not be blank");
      }
    }
  }

  static Schedule onceAt(Instant executionTime) {
    return new Once(executionTime);
  }

  static Schedule interval(Instant startTime, Duration interval) {
    return new Interval(startTime, interval);
  }

  static Schedule cron(String expression) {
    return new Cron(expression);
  }
}
