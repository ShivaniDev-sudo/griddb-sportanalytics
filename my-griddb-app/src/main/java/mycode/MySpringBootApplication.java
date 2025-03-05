package mycode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import mycode.service.MetricsCollectionService;

@SpringBootApplication(exclude = { R2dbcAutoConfiguration.class })
@EnableScheduling
public class MySpringBootApplication {
  public static void main(String[] args) {
    SpringApplication.run(MySpringBootApplication.class, args);
    MetricsCollectionService metricCollection = new MetricsCollectionService();
    metricCollection.collect();
  }

}
