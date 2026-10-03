package com.example.integration.quartz;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.core.scheduler.ISchedulerTask;
import com.example.core.scheduler.SchedulerTaskInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class QuartzJobUnitTests {

  private static final String TASK_ID = "test-task";
  private static final String INPUT_VALUE = "test-input";
  private static final String INPUT_JSON = "{\"value\":\"test-input\"}";

  @Mock private ApplicationContext applicationContext;
  @Mock private JobExecutionContext jobExecutionContext;
  @Mock private ISchedulerTask task;
  @Mock private ObjectMapper objectMapper;

  private QuartzJob quartzJob;

  @BeforeEach
  void setUp() {
    quartzJob = new QuartzJob(applicationContext, objectMapper);
  }

  @Test
  void testExecute_shouldExecuteTask_whenTaskExists() throws Exception {
    // Given
    SchedulerTaskInput input = new SchedulerTaskInput(INPUT_VALUE);

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put(QuartzConstants.TASK_ID_KEY, TASK_ID);
    jobDataMap.put(QuartzConstants.TASK_INPUT_KEY, INPUT_JSON);

    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(applicationContext.getBean(TASK_ID, ISchedulerTask.class)).thenReturn(task);
    when(objectMapper.readValue(INPUT_JSON, SchedulerTaskInput.class)).thenReturn(input);

    // When
    quartzJob.execute(jobExecutionContext);

    // Then
    verify(applicationContext).getBean(TASK_ID, ISchedulerTask.class);
    verify(objectMapper).readValue(INPUT_JSON, SchedulerTaskInput.class);
    verify(task).execute(input);
  }
}
