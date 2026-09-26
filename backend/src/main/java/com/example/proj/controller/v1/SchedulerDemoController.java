package com.example.proj.controller.v1;

import com.example.generated.api.SchedulerDemoApi;
import com.example.generated.dto.ApiResponseGetSchedulesDtoList;
import com.example.generated.dto.ApiResponseString;
import com.example.generated.dto.CreateManualScheduleRequest;
import com.example.proj.service.SchedulerDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SchedulerDemoController implements SchedulerDemoApi {

  private final SchedulerDemoService schedulerDemoService;

  @Override
  public ResponseEntity<ApiResponseString> createManualSchedule(
          CreateManualScheduleRequest request) {

    return ResponseEntity.ok(
            new ApiResponseString().data(schedulerDemoService.createManualSchedule(request)));
  }

  @Override
  public ResponseEntity<ApiResponseGetSchedulesDtoList> getSchedules() {
    return ResponseEntity.ok(
            new ApiResponseGetSchedulesDtoList().data(schedulerDemoService.getSchedules()));
  }

  @Override
  public ResponseEntity<Void> cancelSchedule(String scheduleId) {
    schedulerDemoService.cancelSchedule(scheduleId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> pauseSchedule(String scheduleId) {
    schedulerDemoService.pauseSchedule(scheduleId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> resumeSchedule(String scheduleId) {
    schedulerDemoService.resumeSchedule(scheduleId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> triggerSchedule(String scheduleId) {
    schedulerDemoService.triggerSchedule(scheduleId);
    return ResponseEntity.noContent().build();
  }
}