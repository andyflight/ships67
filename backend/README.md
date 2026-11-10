# Shipmaster backend

Сервер гри: гравці, авторизація, прогрес. Spring Boot 4.1, Java 25, Gradle, H2 → PostgreSQL.
Правила розробки — `CLAUDE.md`.

## Запуск

Потрібна лише JDK для запуску Gradle (17+); Java 25 для збірки Gradle завантажить сам (toolchain).

Потрібен ключ підпису токенів — лише зі змінної середовища, ніколи в git (без нього сервер не стартує):

```bash
cd backend
export SHIPMASTER_JWT_SECRET=$(openssl rand -base64 32)   # або збережіть один раз у ~/.zshrc
./gradlew bootRun
```

Новий ключ робить недійсними лише access-токени (15 хв); refresh-токени й guest credential від ключа не залежать.

- API: `http://localhost:8080/api/v1/...` — контракт: `openspec/specs/player-auth` і `openspec/specs/player-account`
  (до архівації зміни — `openspec/changes/add-player-identity/specs/`).
- H2-консоль: `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:file:./data/shipmaster`, user `sa`, без пароля.
- Дані локальної БД — `backend/data/` (в git не потрапляють). Скинути БД — видалити цю теку.

## Авторизація (коротко)

```
перший запуск   POST /api/v1/auth/guest          -> 201 сесія + guestCredential (зберегти назавжди!)
кожен запит     Authorization: Bearer <accessToken>   (живе 15 хв)
оновлення       POST /api/v1/auth/refresh         -> 200 нова пара токенів (старий refresh одноразовий)
втрата сесії    POST /api/v1/auth/guest/restore   -> 200 нова сесія того ж гравця
вихід           POST /api/v1/auth/logout          -> 204
профіль         GET/PATCH /api/v1/players/me
```

Повтор refresh зі старим токеном протягом 30 с (втрачена відповідь) — безпечний; пізніше — відкликає всю сесію,
але guest credential далі відновлює акаунт. Деталі: `openspec/changes/add-player-identity/design.md` (D4, D5).

## Тести

```bash
./gradlew test
```

Тести працюють на in-memory H2 (`src/test/resources/application-test.properties`, профіль `test`).

## База даних

- Схема — лише Liquibase: `src/main/resources/db/changelog/db.changelog-master.yaml`, зміни — `changes/NNN-<name>.yaml`.
- Перехід на PostgreSQL: змінити `spring.datasource.*` (URL, user, password — зі змінних середовища).
  Драйвер PostgreSQL уже підключений у `build.gradle`.
