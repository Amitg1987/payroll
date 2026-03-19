package com.embeddedpayroll.backend.config;

import com.embeddedpayroll.backend.temporal.TaxFilingActivities;
import com.embeddedpayroll.backend.temporal.TaxFilingWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CorsProperties.class, TemporalProperties.class})
public class TemporalConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.temporal", name = "enabled", havingValue = "true")
    WorkflowServiceStubs workflowServiceStubs(TemporalProperties temporalProperties) {
        return WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalProperties.target())
                .build()
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.temporal", name = "enabled", havingValue = "true")
    WorkflowClient workflowClient(
        WorkflowServiceStubs workflowServiceStubs,
        TemporalProperties temporalProperties
    ) {
        return WorkflowClient.newInstance(
            workflowServiceStubs,
            WorkflowClientOptions.newBuilder()
                .setNamespace(temporalProperties.namespace())
                .build()
        );
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "app.temporal", name = "enabled", havingValue = "true")
    WorkerFactory workerFactory(
        WorkflowClient workflowClient,
        TemporalProperties temporalProperties,
        TaxFilingActivities taxFilingActivities
    ) {
        WorkerFactory workerFactory = WorkerFactory.newInstance(workflowClient);
        Worker worker = workerFactory.newWorker(temporalProperties.taskQueue());
        worker.registerWorkflowImplementationTypes(TaxFilingWorkflowImpl.class);
        worker.registerActivitiesImplementations(taxFilingActivities);
        return workerFactory;
    }
}
