# Design

## Context

- user-service: `spring.security.oauth2.resourceserver.jwt.jwk-set-uri:
  ${JWK_SET_URI}` без `issuer-uri`. catalog-service и seller-service задают
  оба (`JWT_JWK_SET_URI`, `JWT_ISSUER_URI`).
- `.env.user` содержит `PROVIDER_KEYCLOAK_ISSUER_URI=http://keycloak:8181/...`
  — это адрес OAuth2-клиента user-service, он не равен `iss` токенов frontend
  (`http://localhost:8181/...` в dev), поэтому для resource server нужна
  отдельная переменная.
- `UserSecurityWebMvcTest` подменяет `JwtDecoder`, поэтому проверку issuer
  не видит.

## Goals / Non-Goals

**Goals:** отклонение токенов чужого издателя в user-service так же, как в
catalog-service.

**Non-Goals:** audience, внутренние вызовы с токенами внутреннего адреса
Keycloak (их нет: сервисы передают токен пользователя).

## Decisions

- **D1. Конфигурация, а не код.** `issuer-uri: ${JWT_ISSUER_URI}` рядом с
  `jwk-set-uri`. При обоих свойствах Spring Boot строит `NimbusJwtDecoder` по
  JWK set и добавляет `JwtIssuerValidator` — сетевого обращения к discovery
  при старте нет. *Альтернатива:* собственный `JwtDecoder` с валидатором —
  больше кода, тот же результат; отклонено.
- **D2. Переменная без значения по умолчанию.** Отсутствие `JWT_ISSUER_URI`
  дает ошибку старта, а не молчаливое отключение проверки.
- **D3. Тест.** Unit-тест декодера, собранного из конфигурации Spring Boot
  (`OAuth2ResourceServerProperties` → `JwtDecoder`), невозможен без сети к JWK;
  поэтому проверяется валидатор: `JwtValidators.createDefaultWithIssuer`
  отклоняет токен с другим `iss` и принимает с ожидаемым, а в контексте
  user-service бин `JwtDecoder` получает issuer из свойства (тест контекста
  с подмененным JWK URI не обращается к сети до первого запроса).

## Risks / Trade-offs

- [Prod без `JWT_ISSUER_URI` или с неверным значением → user-service не
  стартует или отклоняет все токены] → значение такое же, как у
  catalog-service в prod (`env-catalog`); пользователь добавляет его в
  `env-user` до деплоя.
- [Токены, выданные через внутренний адрес Keycloak] → в проекте такие токены
  не используются клиентами user-service; catalog-service их уже отклоняет.

## Migration Plan

1. Пользователь добавляет `JWT_ISSUER_URI` в Jenkins credential `env-user`
   (значение — как `JWT_ISSUER_URI` в `env-catalog`).
2. Деплой user-service.
3. Откат: прежний образ; переменная не мешает.
