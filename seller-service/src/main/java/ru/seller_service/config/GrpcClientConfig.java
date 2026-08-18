package ru.seller_service.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.klimovich.grpc.user.UserServiceGrpc;

@Configuration
public class GrpcClientConfig {

    @Bean
    public ManagedChannel managedChannel() {
        return ManagedChannelBuilder
                .forAddress("user-service", 9090)
                .usePlaintext()
                .build();
    }

    @Bean
    UserServiceGrpc.UserServiceBlockingStub userStub(
            ManagedChannel channel
    ) {

        return UserServiceGrpc.newBlockingStub(channel);

    }
}
