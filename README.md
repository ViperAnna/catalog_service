# Создание image

docker build -t frontend-mock .

# Запуск без бэкенда с моковыми данными

docker run -p 3000:3000 --rm frontend-mock

# Запуск с бэкендом

docker run -p 3000:3000 --rm frontend-mock npm run dev:java

# Запуск сервиса с тестовыми данными
docker compose --profile generate up --build

# Повторный запуск сервиса с тестовыми данными
docker compose rm -f data-generator
docker compose --profile generate run --rm data-generator

# Запуск сервиса без тестовых данных
docker compose up --build

# API  Documentation (Swagger)
http://localhost:8080/swagger-ui/index.html

# OpenAPI JSON:
http://localhost:8080/v3/api-docs
