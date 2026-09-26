package com.example.core.exception;

public class SchedulerException extends RuntimeException {

  public SchedulerException(String message, Throwable cause) {
    super(message, cause);
  }

  public SchedulerException(String message) {
    super(message);
  }
}
