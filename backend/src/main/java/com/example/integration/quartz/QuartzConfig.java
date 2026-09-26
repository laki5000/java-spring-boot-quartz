package com.example.integration.quartz;

import com.example.core.scheduler.ISchedulerService;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
@ConditionalOnProperty(prefix = "scheduler", name = "provider", havingValue = "quartz")
public class QuartzConfig {

  @Bean
  public ISchedulerService scheduler(Scheduler quartzScheduler) {
    return new QuartzSchedulerService(quartzScheduler);
  }

  @Bean
  public SchedulerFactoryBeanCustomizer quartzJobFactory(ApplicationContext applicationContext) {

    return schedulerFactoryBean -> {
      SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();

      jobFactory.setApplicationContext(applicationContext);

      schedulerFactoryBean.setJobFactory(jobFactory);
    };
  }
}
