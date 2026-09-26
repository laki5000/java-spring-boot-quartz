package com.example.proj.controller.v1;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.core.scheduler.ISchedulerService;
import com.example.generated.dto.CreateManualScheduleRequest;
import com.example.proj.constant.SchedulerConstants;
import com.example.proj.task.ManualTask;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class SchedulerDemoControllerIntegrationTests {

  private static final String SCHEDULER_ENDPOINT = "/api/v1/scheduler/demo";
  private static final String SCHEDULE_ENDPOINT = SCHEDULER_ENDPOINT + "/%s";
  private static final String PAUSE_ENDPOINT = SCHEDULE_ENDPOINT + "/pause";
  private static final String RESUME_ENDPOINT = SCHEDULE_ENDPOINT + "/resume";
  private static final String TRIGGER_ENDPOINT = SCHEDULE_ENDPOINT + "/trigger";

  private static final String INPUT_VALUE = "test-input";
  private static final String MANUAL_TASK_LOG = "Manual task executed with input: " + INPUT_VALUE;

  private static final long FUTURE_EXECUTION_DELAY_SECONDS = 3L;
  private static final long INTERVAL_SECONDS = 1L;
  private static final long LOG_WAIT_TIMEOUT_SECONDS = 5L;

  private static final String RESPONSE_DATA_PATH = "$.data";
  private static final String RESPONSE_IDS_PATH = "$.data[*].id";

  private static final String DATA_KEY = "data";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private ISchedulerService schedulerService;

  private final List<String> createdScheduleIds = new ArrayList<>();

  private ListAppender<ILoggingEvent> listAppender;
  private Logger logger;

  @BeforeEach
  void setUp() {
    logger = (Logger) LoggerFactory.getLogger(ManualTask.class);

    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(listAppender);

    createdScheduleIds.forEach(schedulerService::cancel);
    createdScheduleIds.clear();
  }

  @Test
  void testCreateManualSchedule_shouldReturnScheduleIdAndExecuteTask_whenExecutionTimeIsReached()
      throws Exception {
    // When
    String scheduleId = createOnceSchedule();

    // Then
    assertTrue(scheduleId.startsWith(SchedulerConstants.MANUAL_SCHEDULE_ID_PREFIX));

    waitForLog();

    assertTrue(
        listAppender.list.stream()
            .anyMatch(
                event ->
                    event.getLevel() == Level.INFO
                        && MANUAL_TASK_LOG.equals(event.getFormattedMessage())));
  }

  @Test
  void testCreateManualSchedule_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
    // Given
    CreateManualScheduleRequest request =
        new CreateManualScheduleRequest()
            .type(CreateManualScheduleRequest.TypeEnum.CRON)
            .input(INPUT_VALUE);

    // When / Then
    mockMvc
        .perform(
            post(SCHEDULER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testGetSchedules_shouldReturnCreatedSchedule_whenScheduleExists() throws Exception {
    // Given
    String scheduleId = createOnceSchedule();

    // When / Then
    mockMvc
        .perform(get(SCHEDULER_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath(RESPONSE_DATA_PATH).isArray())
        .andExpect(jsonPath(RESPONSE_IDS_PATH, hasItem(scheduleId)));
  }

  @Test
  void testCancelSchedule_shouldPreventTaskExecution_whenScheduleIsCancelled() throws Exception {
    // Given
    String scheduleId = createOnceSchedule();

    // When
    mockMvc
        .perform(delete(SCHEDULE_ENDPOINT.formatted(scheduleId)))
        .andExpect(status().isNoContent());

    // Then
    waitForExecutionWindow();

    assertTrue(
        listAppender.list.stream()
            .noneMatch(
                event ->
                    event.getLevel() == Level.INFO
                        && MANUAL_TASK_LOG.equals(event.getFormattedMessage())));
  }

  @Test
  void testPauseSchedule_shouldPreventTaskExecution_whenScheduleIsPaused() throws Exception {
    // Given
    String scheduleId = createIntervalSchedule();

    // When
    mockMvc.perform(post(PAUSE_ENDPOINT.formatted(scheduleId))).andExpect(status().isNoContent());

    // Then
    waitForExecutionWindow();

    assertTrue(
        listAppender.list.stream()
            .noneMatch(
                event ->
                    event.getLevel() == Level.INFO
                        && MANUAL_TASK_LOG.equals(event.getFormattedMessage())));
  }

  @Test
  void testResumeSchedule_shouldExecuteTask_whenPausedScheduleIsResumed() throws Exception {
    // Given
    String scheduleId = createIntervalSchedule();

    mockMvc.perform(post(PAUSE_ENDPOINT.formatted(scheduleId))).andExpect(status().isNoContent());

    waitForExecutionWindow();

    listAppender.list.clear();

    // When
    mockMvc.perform(post(RESUME_ENDPOINT.formatted(scheduleId))).andExpect(status().isNoContent());

    // Then
    waitForLog();

    assertTrue(
        listAppender.list.stream()
            .anyMatch(
                event ->
                    event.getLevel() == Level.INFO
                        && MANUAL_TASK_LOG.equals(event.getFormattedMessage())));
  }

  @Test
  void testTriggerSchedule_shouldExecuteTask_whenScheduleIsTriggered() throws Exception {
    // Given
    String scheduleId = createOnceSchedule();

    // When
    mockMvc.perform(post(TRIGGER_ENDPOINT.formatted(scheduleId))).andExpect(status().isNoContent());

    // Then
    waitForLog();

    assertTrue(
        listAppender.list.stream()
            .anyMatch(
                event ->
                    event.getLevel() == Level.INFO
                        && MANUAL_TASK_LOG.equals(event.getFormattedMessage())));
  }

  private String createOnceSchedule() throws Exception {
    CreateManualScheduleRequest request =
        new CreateManualScheduleRequest()
            .type(CreateManualScheduleRequest.TypeEnum.ONCE)
            .executionTime(OffsetDateTime.now().plusSeconds(FUTURE_EXECUTION_DELAY_SECONDS))
            .input(INPUT_VALUE);

    String response =
        mockMvc
            .perform(
                post(SCHEDULER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath(RESPONSE_DATA_PATH).isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String scheduleId = objectMapper.readTree(response).get(DATA_KEY).asString();
    createdScheduleIds.add(scheduleId);

    return scheduleId;
  }

  private String createIntervalSchedule() throws Exception {
    CreateManualScheduleRequest request =
        new CreateManualScheduleRequest()
            .type(CreateManualScheduleRequest.TypeEnum.INTERVAL)
            .startTime(OffsetDateTime.now().plusSeconds(FUTURE_EXECUTION_DELAY_SECONDS))
            .intervalSeconds(INTERVAL_SECONDS)
            .input(INPUT_VALUE);

    String response =
        mockMvc
            .perform(
                post(SCHEDULER_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath(RESPONSE_DATA_PATH).isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String scheduleId = objectMapper.readTree(response).get(DATA_KEY).asString();
    createdScheduleIds.add(scheduleId);

    return scheduleId;
  }

  private void waitForLog() throws InterruptedException {
    long timeout = System.nanoTime() + TimeUnit.SECONDS.toNanos(LOG_WAIT_TIMEOUT_SECONDS);

    while (System.nanoTime() < timeout) {
      boolean executed =
          listAppender.list.stream()
              .anyMatch(
                  event ->
                      event.getLevel() == Level.INFO
                          && MANUAL_TASK_LOG.equals(event.getFormattedMessage()));

      if (executed) {
        return;
      }

      Thread.sleep(100);
    }
  }

  private void waitForExecutionWindow() throws InterruptedException {
    Thread.sleep(TimeUnit.SECONDS.toMillis(FUTURE_EXECUTION_DELAY_SECONDS + 1));
  }
}
