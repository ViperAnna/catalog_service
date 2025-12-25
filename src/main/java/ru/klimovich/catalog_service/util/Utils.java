package ru.klimovich.catalog_service.util;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Utils {

    public static  String generateArticle(){
        return UUID.randomUUID().toString();
    }

}
