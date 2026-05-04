## 📝Описание

Проект представляет собой fullstack-приложение интернет-магазина различных товаров с backend (Spring Boot) и frontend (React).
Развёртывание полностью автоматизировано через CI/CD pipeline в Jenkins.

---

## 🧱Стек технологий

- Java: 17
- Backend: Spring Boot 3.3.4
- Frontend: React
- Database: MongoDB
- Storage: MinIO
- CI/CD: Jenkins
- Containerization: Docker, Docker Compose

---

## ⚙️Требования

На сервере должны быть установлены:

- docker
- docker compose
- git

---
## 🤵🏻Jenkins

### Установка Jenkins:

```
docker run -d \
  --name jenkins \
  -p 8081:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts
```

---

## 📋Конфигурация (ENV)
Все конфигурационные файлы передаются на сервер через Jenkins Credentials.

- .env.minio — настройки MinIO;
- .env.mongodb — параметры подключения к MongoDB;
- .env.spring — Spring Boot переменные.

---

## 🚀Возможности CI/CD:

Pipeline параметры

| Имя параметра | Значение               | Описание                                                                                     |
|---------------|------------------------|----------------------------------------------------------------------------------------------|
| BRANCH_NAME   | develop (default)      | Ветвь Git для сборки и развёртывания                                                         |
| DEPLOY_TARGET | all, backend, frontend | Что развернуть:<br>all — весь стек<br>backend — только backend<br>frontend — только frontend |

---

## ✅Доступ
- Jenkins: <http://SERVER_IP:8081>
- Frontend:	<http://SERVER_IP:3000>
- Backend:	<http://SERVER_IP:8080>

---

## 🐸API Документация
- Swagger UI: <http://SERVER_IP:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://SERVER_IP:8080/v3/api-docs>

---
