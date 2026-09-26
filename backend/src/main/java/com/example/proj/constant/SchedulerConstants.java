package com.example.proj.constant;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class SchedulerConstants {

  public static final String STARTUP_SCHEDULE_ID = "startup-schedule";
  public static final String STARTUP_TASK_ID = "startup";
  public static final long STARTUP_INTERVAL_SECONDS = 10L;
  public static final String STARTUP_TASK_INPUT = "Startup task input";
  public static final String MANUAL_SCHEDULE_ID_PREFIX = "manual-schedule-";
  public static final String MANUAL_TASK_ID = "manual";
}
