package ru.klimovich.notificationservice.conf;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

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
}