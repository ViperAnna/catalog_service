package ru.klimovich.notificationservice.conf;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import ru.klimovich.notificationservice.exception.UnknownApplicationTypeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Requirement "Consumer error handling and DLT": неизвестный тип заявки уходит в DLT без повторов
 * (change seller-approval-notification-types).
 */
class KafkaConfigTest {

    private final ConsumerRecord<String, String> record =
            new ConsumerRecord<>("seller-events-topic", 0, 0L, "7", "{}");
    private final Consumer<?, ?> kafkaConsumer = mock(Consumer.class);
    private final MessageListenerContainer container = mock(MessageListenerContainer.class);

    @Test
    void unknownApplicationType_recoveredOnFirstAttempt() {
        ConsumerRecordRecoverer recoverer = mock(ConsumerRecordRecoverer.class);
        DefaultErrorHandler handler = KafkaConfig.errorHandler(recoverer);
        ListenerExecutionFailedException failure = new ListenerExecutionFailedException(
                "Listener failed", new UnknownApplicationTypeException(7L, "FOO"));

        boolean recovered = handler.handleOne(failure, record, kafkaConsumer, container);

        assertThat(recovered).isTrue();
        verify(recoverer).accept(eq(record), any());
    }

    @Test
    void otherException_isRetriedFirst() {
        ConsumerRecordRecoverer recoverer = mock(ConsumerRecordRecoverer.class);
        DefaultErrorHandler handler = KafkaConfig.errorHandler(recoverer);
        ListenerExecutionFailedException failure = new ListenerExecutionFailedException(
                "Listener failed", new RuntimeException("Failed to deserialize event"));

        boolean recovered = handler.handleOne(failure, record, kafkaConsumer, container);

        assertThat(recovered).isFalse();
        verify(recoverer, never()).accept(any(), any());
    }
}
