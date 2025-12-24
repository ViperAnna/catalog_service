package ru.klimovich.catalog_service.util;

import java.util.UUID;

public class ArticleUtils {
    public static  String generateArticle(){
        return UUID.randomUUID().toString();
    }
}
