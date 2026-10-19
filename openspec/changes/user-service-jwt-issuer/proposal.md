# Proposal

## Why

user-service проверяет подпись JWT, но не claim `iss`, в отличие от
catalog-service, seller-service и order-service (`access-control`
`JWT validation per service`, OBS-01; `user-profile` OBS-06; `wishlists`
ASM-01). Токен, подписанный ключом realm, но выданный для другого издателя
(например, через внутренний адрес `http://keycloak:8181`), user-service
принимает, а catalog-service отклоняет — вишлисты отвечают 404 «товар не
найден». Решение пользователя (2026-10-15): отдельный change для user-service —
проверять issuer и отклонять токены неподходящего издателя.

## What Changes

- user-service проверяет `iss` по `JWT_ISSUER_URI`, как catalog-service и
  seller-service; несовпадение — 401.
- Новая переменная окружения `JWT_ISSUER_URI` для user-service: dev —
  `http://localhost:8181/realms/user-service-realm` (как у catalog/seller);
  prod — значение пользователь добавляет в Jenkins credential `env-user`.
- Не входят: проверка audience, изменения других сервисов и Keycloak,
  OAuth2-клиент user-service (`PROVIDER_KEYCLOAK_ISSUER_URI` не меняется).

## Capabilities

### New Capabilities
<!-- нет -->

### Modified Capabilities
- `access-control`: user-service входит в число сервисов с проверкой `iss`.

## Impact

- user-service: `application.yml` (`issuer-uri`), тест безопасности. Код
  конфигурации безопасности не меняется: Spring Boot добавляет проверку
  issuer при заданных `jwk-set-uri` и `issuer-uri` без OIDC discovery.
- Окружение: `.env.user` (локально, не в git) и prod credential `env-user`
  (пользователь). Без переменной user-service не стартует (fail-fast) —
  это обязательный шаг деплоя.
- После archive: `access-control` OBS-01, `user-profile` OBS-06, `wishlists`
  ASM-01 — устранены.
