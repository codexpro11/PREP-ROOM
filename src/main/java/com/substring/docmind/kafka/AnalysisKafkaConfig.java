package com.substring.docmind.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.server.ResponseStatusException;

@Configuration @EnableScheduling
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class AnalysisKafkaConfig {
    @Bean
    NewTopic analysisTopic(@Value("${app.kafka.analysis-topic}") String topic,
                           @Value("${app.kafka.partitions:3}") int partitions,
                           @Value("${app.kafka.replicas:1}") int replicas) {
        return TopicBuilder.name(topic).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    NewTopic analysisDeadLetterTopic(@Value("${app.kafka.analysis-topic}") String topic,
                                     @Value("${app.kafka.partitions:3}") int partitions,
                                     @Value("${app.kafka.replicas:1}") int replicas) {
        return TopicBuilder.name(topic + ".DLT").partitions(partitions).replicas(replicas).build();
    }

    @Bean
    DefaultErrorHandler analysisErrorHandler(KafkaTemplate<String, String> template, AnalysisKafkaConsumer consumer,
                                             @Value("${app.kafka.retry-delay-ms:2000}") long delay,
                                             @Value("${app.kafka.retry-attempts:2}") long retries) {
        var deadLetter = new DeadLetterPublishingRecoverer(template,
                (record, error) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        deadLetter.setFailIfSendResultIsError(true);
        return new DefaultErrorHandler((record, error) -> {
            // Confirm DLT delivery before marking failed, so a DLT outage cannot
            // turn the next redelivery into a successful no-op and lose the record.
            deadLetter.accept(record, error);
            // Malformed messages and deleted interviews still go to DLT for inspection.
            try { consumer.failed((String) record.value()); }
            catch (IllegalArgumentException ignored) { }
            catch (tools.jackson.core.JacksonException ignored) { }
            catch (ResponseStatusException e) {
                if (e.getStatusCode().value() != 404) throw e;
            }
        }, new FixedBackOff(delay, retries));
    }
}
