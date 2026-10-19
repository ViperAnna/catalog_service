# Tasks

`skipTests=true` в user-service не меняется. Тесты без Docker:
`mvn -f user-service/pom.xml test -DskipTests=false -Dtest='JwtIssuerValidationTest,UserSecurityWebMvcTest'`.

## 1. Проверка issuer

- [ ] 1.1 Тест (до кода) `JwtIssuerValidationTest`: валидатор, собранный так же, как Spring Boot строит его из `issuer-uri`, отклоняет JWT с `iss` = `http://keycloak:8181/realms/user-service-realm` и принимает с `JWT_ISSUER_URI`; контекст user-service с заданным `issuer-uri` поднимает `JwtDecoder` с проверкой issuer (D3). Проверка: тест на наличие проверки в контексте падает на текущем коде
- [ ] 1.2 `issuer-uri: ${JWT_ISSUER_URI}` в `application.yml` (D1, D2); `JWT_ISSUER_URI` в локальном `.env.user` (dev-значение). Проверка: тесты 1.1 и `UserSecurityWebMvcTest` проходят; `mvn -q -f user-service/pom.xml package`

## 2. Проверка change

- [ ] 2.1 Границы: изменены только `application.yml` и тест user-service (плюс локальный `.env.user`, не в git). Проверка: `git diff --stat`
- [ ] 2.2 `openspec validate user-service-jwt-issuer --strict` проходит; после archive — `access-control` OBS-01, `user-profile` OBS-06, `wishlists` ASM-01 — устранены. Проверка: strict validate specs

## 3. Ручные операции (только по запросу пользователя)

- [ ] 3.1 Стенд: пересобрать user-service; токен через `localhost:8181` → `GET /users/me` проходит; токен с `iss` `http://keycloak:8181/...` → 401
- [ ] 3.2 Prod: пользователь добавляет `JWT_ISSUER_URI` в credential `env-user` до деплоя
