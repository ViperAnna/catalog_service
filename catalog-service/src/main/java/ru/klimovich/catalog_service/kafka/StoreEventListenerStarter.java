package ru.klimovich.catalog_service.kafka;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Запускает потребителя store-events-topic после старта приложения.
 * Автостарт контейнеров выключен (spring.kafka.listener.auto-startup=false),
 * чтобы недоступная Kafka не роняла контекст: при ошибке запуска попытка
 * повторяется, а REST API каталога работает независимо от Kafka.
 */
@Component
@Slf4j
public class StoreEventListenerStarter {

    static final String LISTENER_ID = "storeEventsListener";
    static final long RETRY_DELAY_SECONDS = 30;

    private final KafkaListenerEndpointRegistry registry;
    private final ScheduledExecutorService scheduler;

    @Autowired
    public StoreEventListenerStarter(KafkaListenerEndpointRegistry registry) {
        this(registry, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "store-events-listener-starter");
            thread.setDaemon(true);
            return thread;
        }));
    }

    StoreEventListenerStarter(KafkaListenerEndpointRegistry registry, ScheduledExecutorService scheduler) {
        this.registry = registry;
        this.scheduler = scheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        tryStart();
    }

    void tryStart() {
        MessageListenerContainer container = registry.getListenerContainer(LISTENER_ID);
        if (container == null || container.isRunning()) {
            return;
        }
        try {
            container.start();
            log.info("Store events listener started");
        } catch (RuntimeException e) {
            log.warn("Store events listener could not start ({}), retry in {} s",
                    e.getMessage(), RETRY_DELAY_SECONDS);
            // контейнер помечает себя running до создания consumer; сбрасываем состояние,
            // иначе следующая попытка решит, что он уже запущен
            stopQuietly(container);
            scheduler.schedule(this::tryStart, RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static void stopQuietly(MessageListenerContainer container) {
        try {
            container.stop();
        } catch (RuntimeException e) {
            log.debug("Store events listener stop after failed start: {}", e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
