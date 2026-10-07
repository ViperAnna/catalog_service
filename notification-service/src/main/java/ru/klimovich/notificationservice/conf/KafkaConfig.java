package ru.klimovich.notificationservice.conf;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import ru.klimovich.notificationservice.exception.UnknownApplicationTypeException;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic testTopic() {
        return TopicBuilder
                .name("user-created-events-topic")
                .partitions(3)
                .replicas(1)
//                .replicas(3)
//                .configs(Map.of("min.insync.replicas", "2"))
                .build();
    }
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        return errorHandler(new DeadLetterPublishingRecoverer(template));
    }

    /**
     * 3 повтора с паузой 1 с, затем DLT. Неизвестный тип заявки повторять бессмысленно: сразу в DLT.
     */
    static DefaultErrorHandler errorHandler(ConsumerRecordRecoverer recoverer) {
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(UnknownApplicationTypeException.class);
        return handler;
    }
}