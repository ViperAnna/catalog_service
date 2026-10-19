package ru.klimovich.user_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

//    @Bean("transactionManager")
//    JpaTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory){
//        return new JpaTransactionManager(entityManagerFactory);
//    }

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

    /**
     * Ошибка обработки product-events-topic повторяется 3 раза с паузой 1 с, затем сообщение
     * публикуется в product-events-topic.DLT (как в catalog-service и notification-service).
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3));
    }
}