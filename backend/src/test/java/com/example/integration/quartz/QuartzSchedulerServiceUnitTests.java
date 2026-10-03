package com.example.integration.quartz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.core.exception.SchedulerException;
import com.example.core.scheduler.ISchedule;
import com.example.core.scheduler.ScheduledTask;
import com.example.core.scheduler.SchedulerTaskInput;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class QuartzSchedulerServiceUnitTests {

  private static final String SCHEDULE_ID = "test-schedule";
  private static final String TASK_ID = "test-task";
  private static final String INPUT_VALUE = "test-input";
  private static final String INPUT_JSON = "{\"value\":\"test-input\"}";
  private static final Instant EXECUTION_TIME = Instant.parse("2030-09-26T14:00:00Z");
  private static final Instant START_TIME = Instant.parse("2030-09-26T14:00:00Z");
  private static final Duration INTERVAL = Duration.ofSeconds(10);
  private static final String CRON_EXPRESSION = "0/10 * * * * ?";

  @Mock private Scheduler quartzScheduler;
  @Mock private ObjectMapper objectMapper;

  private QuartzSchedulerService quartzSchedulerService;

  @BeforeEach
  void setUp() {
    quartzSchedulerService = new QuartzSchedulerService(quartzScheduler, objectMapper);
  }

  @Test
  void testSchedule_shouldCreateOnceTrigger_whenScheduleIsOnce() throws Exception {
    // Given
    ISchedule schedule = ISchedule.onceAt(EXECUTION_TIME);
    SchedulerTaskInput input = new SchedulerTaskInput(INPUT_VALUE);

    when(objectMapper.writeValueAsString(input)).thenReturn(INPUT_JSON);

    ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

    // When
    quartzSchedulerService.schedule(SCHEDULE_ID, TASK_ID, schedule, input);

    // Then
    verify(objectMapper).writeValueAsString(input);
    verify(quartzScheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());

    JobDetail job = jobCaptor.getValue();
    Trigger trigger = triggerCaptor.getValue();

    assertEquals(SCHEDULE_ID, job.getKey().getName());
    assertEquals(TASK_ID, job.getJobDataMap().getString(QuartzConstants.TASK_ID_KEY));
    assertEquals(INPUT_JSON, job.getJobDataMap().getString(QuartzConstants.TASK_INPUT_KEY));

    SimpleTrigger simpleTrigger = assertInstanceOf(SimpleTrigger.class, trigger);

    assertEquals(SCHEDULE_ID, simpleTrigger.getKey().getName());
    assertEquals(Date.from(EXECUTION_TIME), simpleTrigger.getStartTime());
    assertEquals(0, simpleTrigger.getRepeatCount());
  }

  @Test
  void testSchedule_shouldCreateIntervalTrigger_whenScheduleIsInterval() throws Exception {
    // Given
    ISchedule schedule = ISchedule.interval(START_TIME, INTERVAL);
    SchedulerTaskInput input = new SchedulerTaskInput(INPUT_VALUE);

    when(objectMapper.writeValueAsString(input)).thenReturn(INPUT_JSON);

    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

    // When
    quartzSchedulerService.schedule(SCHEDULE_ID, TASK_ID, schedule, input);

    // Then
    verify(objectMapper).writeValueAsString(input);
    verify(quartzScheduler).scheduleJob(any(JobDetail.class), triggerCaptor.capture());

    SimpleTrigger trigger = assertInstanceOf(SimpleTrigger.class, triggerCaptor.getValue());

    assertEquals(SCHEDULE_ID, trigger.getKey().getName());
    assertEquals(Date.from(START_TIME), trigger.getStartTime());
    assertEquals(INTERVAL.toMillis(), trigger.getRepeatInterval());
    assertEquals(SimpleTrigger.REPEAT_INDEFINITELY, trigger.getRepeatCount());
  }

  @Test
  void testSchedule_shouldCreateCronTrigger_whenScheduleIsCron() throws Exception {
    // Given
    ISchedule schedule = ISchedule.cron(CRON_EXPRESSION);
    SchedulerTaskInput input = new SchedulerTaskInput(INPUT_VALUE);

    when(objectMapper.writeValueAsString(input)).thenReturn(INPUT_JSON);

    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

    // When
    quartzSchedulerService.schedule(SCHEDULE_ID, TASK_ID, schedule, input);

    // Then
    verify(objectMapper).writeValueAsString(input);
    verify(quartzScheduler).scheduleJob(any(JobDetail.class), triggerCaptor.capture());

    CronTrigger trigger = assertInstanceOf(CronTrigger.class, triggerCaptor.getValue());

    assertEquals(SCHEDULE_ID, trigger.getKey().getName());
    assertEquals(CRON_EXPRESSION, trigger.getCronExpression());
  }

  @Test
  void testSchedule_shouldThrowSchedulerException_whenQuartzThrowsException() throws Exception {
    // Given
    ISchedule schedule = ISchedule.onceAt(EXECUTION_TIME);
    SchedulerTaskInput input = new SchedulerTaskInput(INPUT_VALUE);
    org.quartz.SchedulerException quartzException = new org.quartz.SchedulerException();

    when(objectMapper.writeValueAsString(input)).thenReturn(INPUT_JSON);

    when(quartzScheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
        .thenThrow(quartzException);

    // When / Then
    SchedulerException exception =
        assertThrows(
            SchedulerException.class,
            () -> quartzSchedulerService.schedule(SCHEDULE_ID, TASK_ID, schedule, input));

    assertEquals(quartzException, exception.getCause());
  }

  @Test
  void testCancel_shouldDeleteJob_whenScheduleExists() throws Exception {
    // When
    quartzSchedulerService.cancel(SCHEDULE_ID);

    // Then
    verify(quartzScheduler).deleteJob(new JobKey(SCHEDULE_ID));
  }

  @Test
  void testCancel_shouldThrowSchedulerException_whenQuartzThrowsException() throws Exception {
    // Given
    org.quartz.SchedulerException quartzException = new org.quartz.SchedulerException();

    when(quartzScheduler.deleteJob(new JobKey(SCHEDULE_ID))).thenThrow(quartzException);

    // When / Then
    SchedulerException exception =
        assertThrows(SchedulerException.class, () -> quartzSchedulerService.cancel(SCHEDULE_ID));

    assertEquals(quartzException, exception.getCause());
  }

  @Test
  void testPause_shouldPauseJob_whenScheduleExists() throws Exception {
    // When
    quartzSchedulerService.pause(SCHEDULE_ID);

    // Then
    verify(quartzScheduler).pauseJob(new JobKey(SCHEDULE_ID));
  }

  @Test
  void testPause_shouldThrowSchedulerException_whenQuartzThrowsException() throws Exception {
    // Given
    org.quartz.SchedulerException quartzException = new org.quartz.SchedulerException();

    doThrow(quartzException).when(quartzScheduler).pauseJob(new JobKey(SCHEDULE_ID));

    // When / Then
    SchedulerException exception =
        assertThrows(SchedulerException.class, () -> quartzSchedulerService.pause(SCHEDULE_ID));

    assertEquals(quartzException, exception.getCause());
  }

  @Test
  void testResume_shouldResumeJob_whenScheduleExists() throws Exception {
    // When
    quartzSchedulerService.resume(SCHEDULE_ID);

    // Then
    verify(quartzScheduler).resumeJob(new JobKey(SCHEDULE_ID));
  }

  @Test
  void testResume_shouldThrowSchedulerException_whenQuartzThrowsException() throws Exception {
    // Given
    org.quartz.SchedulerException quartzException = new org.quartz.SchedulerException();

    doThrow(quartzException).when(quartzScheduler).resumeJob(new JobKey(SCHEDULE_ID));

    // When / Then
    SchedulerException exception =
        assertThrows(SchedulerException.class, () -> quartzSchedulerService.resume(SCHEDULE_ID));

    assertEquals(quartzException, exception.getCause());
  }

  @Test
  void testTrigger_shouldTriggerJob_whenScheduleExists() throws Exception {
    // When
    quartzSchedulerService.trigger(SCHEDULE_ID);

    // Then
    verify(quartzScheduler).triggerJob(new JobKey(SCHEDULE_ID));
  }

  @Test
  void testTrigger_shouldThrowSchedulerException_whenQuartzThrowsException() throws Exception {
    // Given
    org.quartz.SchedulerException quartzException = new org.quartz.SchedulerException();

    doThrow(quartzException).when(quartzScheduler).triggerJob(new JobKey(SCHEDULE_ID));

    // When / Then
    SchedulerException exception =
        assertThrows(SchedulerException.class, () -> quartzSchedulerService.trigger(SCHEDULE_ID));

    assertEquals(quartzException, exception.getCause());
  }

  @Test
  void testGetSchedules_shouldReturnScheduledTasks_whenSchedulesExist() throws Exception {
    // Given
    TriggerKey triggerKey = new TriggerKey(SCHEDULE_ID);
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).build();

    when(quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup())).thenReturn(Set.of(triggerKey));
    when(quartzScheduler.getTrigger(triggerKey)).thenReturn(trigger);
    when(quartzScheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.NORMAL);

    // When
    List<ScheduledTask> result = quartzSchedulerService.getSchedules();

    // Then
    assertEquals(1, result.size());
    assertEquals(SCHEDULE_ID, result.getFirst().id());
    assertEquals(ScheduledTask.ScheduleType.ONCE, result.getFirst().type());
    assertEquals(ScheduledTask.ScheduleStatus.SCHEDULED, result.getFirst().status());
  }

  @Test
  void testGetSchedules_shouldReturnCronType_whenTriggerIsCronTrigger() throws Exception {
    // Given
    TriggerKey triggerKey = new TriggerKey(SCHEDULE_ID);
    CronTrigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .withSchedule(CronScheduleBuilder.cronSchedule(CRON_EXPRESSION))
            .build();

    when(quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup())).thenReturn(Set.of(triggerKey));
    when(quartzScheduler.getTrigger(triggerKey)).thenReturn(trigger);
    when(quartzScheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.NORMAL);

    // When
    List<ScheduledTask> result = quartzSchedulerService.getSchedules();

    // Then
    assertEquals(ScheduledTask.ScheduleType.CRON, result.getFirst().type());
  }

  @Test
  void testGetSchedules_shouldReturnIntervalType_whenTriggerRepeats() throws Exception {
    // Given
    TriggerKey triggerKey = new TriggerKey(SCHEDULE_ID);
    SimpleTrigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(10).withRepeatCount(5))
            .build();

    when(quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup())).thenReturn(Set.of(triggerKey));
    when(quartzScheduler.getTrigger(triggerKey)).thenReturn(trigger);
    when(quartzScheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.NORMAL);

    // When
    List<ScheduledTask> result = quartzSchedulerService.getSchedules();

    // Then
    assertEquals(ScheduledTask.ScheduleType.INTERVAL, result.getFirst().type());
  }

  @Test
  void testGetSchedules_shouldReturnPausedStatus_whenTriggerIsPaused() throws Exception {
    // Given
    TriggerKey triggerKey = new TriggerKey(SCHEDULE_ID);
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).build();

    when(quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup())).thenReturn(Set.of(triggerKey));
    when(quartzScheduler.getTrigger(triggerKey)).thenReturn(trigger);
    when(quartzScheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.PAUSED);

    // When
    List<ScheduledTask> result = quartzSchedulerService.getSchedules();

    // Then
    assertEquals(ScheduledTask.ScheduleStatus.PAUSED, result.getFirst().status());
  }

  @Test
  void testGetSchedules_shouldReturnBlockedStatus_whenTriggerIsBlocked() throws Exception {
    // Given
    TriggerKey triggerKey = new TriggerKey(SCHEDULE_ID);
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).build();

    when(quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup())).thenReturn(Set.of(triggerKey));
    when(quartzScheduler.getTrigger(triggerKey)).thenReturn(trigger);
    when(quartzScheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.BLOCKED);

    // When
    List<ScheduledTask> result = quartzSchedulerService.getSchedules();

    // Then
    assertEquals(ScheduledTask.ScheduleStatus.BLOCKED, result.getFirst().status());
  }

  @Test
  void testGetSchedules_shouldReturnCompleteStatus_whenTriggerIsComplete() throws Exception {
    // Given
    TriggerKey triggerKey = new TriggerKey(SCHEDULE_ID);
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).build();

    when(quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup())).thenReturn(Set.of(triggerKey));
    when(quartzScheduler.getTrigger(triggerKey)).thenReturn(trigger);
    when(quartzScheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.COMPLETE);

    // When
    List<ScheduledTask> result = quartzSchedulerService.getSchedules();

    // Then
    assertEquals(ScheduledTask.ScheduleStatus.COMPLETE, result.getFirst().status());
  }

  @Test
  void testGetSchedules_shouldReturnErrorStatus_whenTriggerIsError() throws Exception {
    // Given
    TriggerKey triggerKey = new TriggerKey(SCHEDULE_ID);
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).build();

    when(quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup())).thenReturn(Set.of(triggerKey));
    when(quartzScheduler.getTrigger(triggerKey)).thenReturn(trigger);
    when(quartzScheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.ERROR);

    // When
    List<ScheduledTask> result = quartzSchedulerService.getSchedules();

    // Then
    assertEquals(ScheduledTask.ScheduleStatus.ERROR, result.getFirst().status());
  }

  @Test
  void testGetSchedules_shouldThrowSchedulerException_whenTriggerStateIsNone() throws Exception {
    // Given
    TriggerKey triggerKey = new TriggerKey(SCHEDULE_ID);
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).build();

    when(quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup())).thenReturn(Set.of(triggerKey));
    when(quartzScheduler.getTrigger(triggerKey)).thenReturn(trigger);
    when(quartzScheduler.getTriggerState(triggerKey)).thenReturn(Trigger.TriggerState.NONE);

    // When / Then
    assertThrows(SchedulerException.class, () -> quartzSchedulerService.getSchedules());
  }

  @Test
  void testGetSchedules_shouldThrowSchedulerException_whenGetTriggerThrowsException()
      throws Exception {
    // Given
    TriggerKey triggerKey = new TriggerKey(SCHEDULE_ID);
    org.quartz.SchedulerException quartzException = new org.quartz.SchedulerException();

    when(quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup())).thenReturn(Set.of(triggerKey));
    when(quartzScheduler.getTrigger(triggerKey)).thenThrow(quartzException);

    // When / Then
    SchedulerException exception =
        assertThrows(SchedulerException.class, () -> quartzSchedulerService.getSchedules());

    assertEquals(quartzException, exception.getCause());
  }

  @Test
  void testGetSchedules_shouldThrowSchedulerException_whenGetTriggerKeysThrowsException()
      throws Exception {
    // Given
    org.quartz.SchedulerException quartzException = new org.quartz.SchedulerException();

    when(quartzScheduler.getTriggerKeys(GroupMatcher.anyGroup())).thenThrow(quartzException);

    // When / Then
    SchedulerException exception =
        assertThrows(SchedulerException.class, () -> quartzSchedulerService.getSchedules());

    assertEquals(quartzException, exception.getCause());
  }
}
