# Создание image

docker build -t frontend-mock .

# Запуск без бэкенда с моковыми данными

docker run -p 3000:3000 --rm frontend-mock

# Запуск с бэкендом

docker run -p 3000:3000 --rm frontend-mock npm run dev:java

