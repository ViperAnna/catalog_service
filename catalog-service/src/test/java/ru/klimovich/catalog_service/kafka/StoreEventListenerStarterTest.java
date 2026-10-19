package ru.klimovich.catalog_service.kafka;

import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Requirement "Store events consumer error handling": старт catalog-service без Kafka
 * (change archive-products-of-deleted-store).
 */
@ExtendWith(MockitoExtension.class)
class StoreEventListenerStarterTest {

    @Mock
    private KafkaListenerEndpointRegistry registry;
    @Mock
    private MessageListenerContainer container;
    @Mock
    private ScheduledExecutorService scheduler;

    private StoreEventListenerStarter starter;

    @BeforeEach
    void setUp() {
        starter = new StoreEventListenerStarter(registry, scheduler);
        when(registry.getListenerContainer("storeEventsListener")).thenReturn(container);
    }

    @Test
    @DisplayName("Ошибка запуска (Kafka недоступна) -> повтор планируется, исключение не выходит наружу")
    void startFailure_schedulesRetry() {
        doThrow(new KafkaException("No resolvable bootstrap urls given in bootstrap.servers"))
                .when(container).start();

        starter.onApplicationReady();

        verify(container).stop();
        verify(scheduler).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Повтор после неудачи снова пытается запустить контейнер")
    void retryAfterFailure_triesAgain() {
        doThrow(new KafkaException("No resolvable bootstrap urls given in bootstrap.servers"))
                .doNothing()
                .when(container).start();

        starter.onApplicationReady();
        starter.tryStart();

        verify(container, org.mockito.Mockito.times(2)).start();
        verify(scheduler, org.mockito.Mockito.times(1)).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Успешный запуск -> повтор не планируется")
    void startSuccess_noRetry() {
        starter.onApplicationReady();

        verify(container).start();
        verify(scheduler, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Контейнер уже запущен -> повторный запуск не выполняется")
    void alreadyRunning_noStart() {
        when(container.isRunning()).thenReturn(true);

        starter.onApplicationReady();

        verify(container, never()).start();
    }
}
