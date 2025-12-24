package ru.klimovich.catalog_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.Random;
import java.util.UUID;

@Configuration
@EnableMongoAuditing
public class AppConfig {

}
