# Java Spring Boot Scheduler

A Spring Boot project demonstrating scheduler integration using Quartz.

This project is derived from the [Java Spring Boot Core](https://github.com/laki5000/java-spring-boot-core) project.

## Scheduler Integration

The project provides a scheduler abstraction through `ISchedulerService` with a Quartz-based implementation.

It supports:

- One-time schedules
- Interval schedules
- Cron schedules
- Cancelling, pausing, and resuming schedules
- Manually triggering schedules
- Retrieving scheduled tasks

## Scheduler Tasks

Scheduled tasks implement the `ISchedulerTask` interface and receive their input through `SchedulerTaskInput`.

## Demo Endpoints

The project contains demo endpoints for creating and managing schedules.

Schedules can be created using one-time execution, fixed intervals, or cron expressions.