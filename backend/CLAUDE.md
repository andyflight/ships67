# Shipmaster — backend

Сервер мобільної гри: акаунти гравців і їхній прогрес (кораблі, встановлені деталі, фарби, валюта).
Про арт/ассети бекенд не знає — лише ідентифікатори контенту (`ship_sloop_t1`, `part_cannon_bronze`, `paint_red`)
та ігрові числа (ціни, слоти, умови еволюції).

Spring Boot 4.1, Java 25 (Gradle toolchain), Gradle wrapper, Spring MVC, Spring Data JPA, Liquibase, Lombok.
База: **H2** (файл `backend/data/`, режим PostgreSQL) → пізніше **PostgreSQL** лише зміною конфігурації.

## Структура

Clean architecture всередині кожного доменного пакета (верхній рівень — домен, не шар):

```
src/main/java/com/games/shipmaster/
├── BackendApplication.java
├── common/                 # спільне
│   ├── domain/             # DomainException: NotFound/Conflict/InvalidInput/Unauthorized
│   ├── api/                # ProblemDetailsAdvice, SecurityConfig, @CurrentPlayer
│   └── infrastructure/     # Clock bean
└── <domain>/               # player/, auth/, progress/, economy/ …
    ├── domain/             # @Entity з поведінкою, value objects, порти (інтерфейси репозиторіїв і сервісів)
    ├── application/        # use case = один клас (SignInAsGuest, RenamePlayer), @Transactional, повертає record-и
    ├── infrastructure/     # реалізації портів: Spring Data (extends JpaRepository + порт), JWT, крипто, конфіг
    └── api/                # @RestController + dto/ (record-и запитів/відповідей, Bean Validation)
src/main/resources/
├── application.properties
└── db/changelog/db.changelog-master.yaml + changes/NNN-<name>.yaml
src/test/resources/application-test.properties   # in-memory H2 для тестів
```

## Правила

1. **Сервер — джерело правди.** Клієнт надсилає дії (встановити деталь, еволюціонувати, купити), сервер
   перевіряє умови й валюту і застосовує. Клієнт ніколи не надсилає готовий стан прогресу.
2. **Схема БД — лише через Liquibase.** `ddl-auto=validate`. Новий changeset = новий файл у `db/changelog/changes/`
   + include у master. Злитий changeset ніколи не редагуємо.
3. **SQL сумісний з PostgreSQL.** Без H2-специфічних типів і функцій; нативний SQL — лише за потреби.
4. **API:** REST + JSON під `/api/v1/...`; помилки — RFC 9457 `ProblemDetail`; сутності JPA назовні не віддаємо — лише DTO (`record`).
   Будь-яка зміна API = зміна контракту з Unity-клієнтом → описується в OpenSpec.
5. **Ідентифікатори й час:** `UUID` для гравців і записів, `Instant` (UTC) для часу.
6. **Покупки й нагороди ідемпотентні:** повторний запит з тим самим id не дає валюту чи предмет двічі.
7. Lombok: `@Getter`, `@RequiredArgsConstructor`, `@Slf4j`; без `@Data` на сутностях. Залежності — через конструктор.
8. Секрети (ключі підпису токенів, паролі БД) — лише зі змінних середовища, ніколи в git.
9. Нова логіка → тест: unit (domain, use case з in-memory фейками портів), `@WebMvcTest` (контролер),
   `@DataJpaTest` (репозиторій), `@SpringBootTest` (інтеграція).
10. **Залежності шарів — лише всередину:** `api → application → domain`, `infrastructure → application/domain`.
    - `domain`: лише `java.*`, Lombok і `jakarta.persistence` (свідомий виняток: сутності — це `@Entity`). Без Spring.
    - `application`: domain + `@Service`/`@Component`/`@Transactional`. Назовні — record-и, не сутності.
    - `api`: лише application (і `common.domain`); сутності, репозиторії, infrastructure — заборонені.
    - Інший домен викликаємо через його `application` (напр. `auth` → `CreateGuestPlayer`); зворотну залежність
      розриваємо інтерфейсом в application (напр. `PlayerProvidersLookup`, реалізує auth).
    - Перевіряє `ArchitectureTest` (ArchUnit) — порушення ламає `./gradlew test`.
11. Use case-и, сутності й Spring Data-інтерфейси не `final` (проксі `@Transactional`, JPA). Порт репозиторію
    оголошує `<S extends X> S save(S)` — так Spring Data реалізує його без адаптера.
12. Час — лише через бін `Clock` (у тестах `MutableClock`), не `Instant.now()`.

## Платформні флоу

Нагадування (не специфікація) про auth, покупки, рекламу, рефанди й прогрес: @.claude/platform-flows.md

## Команди (з `backend/`)

```bash
./gradlew bootRun      # http://localhost:8080, H2-консоль: /h2-console
./gradlew test         # тести на in-memory H2
./gradlew build        # компіляція + тести + jar
```

Після змін у коді — `./gradlew test` і виправити помилки.

## Код-стиль

- Пакет = домен, не шар (`player/`, а не `controllers/`).
- Класи `final` там, де не потрібне наслідування (крім JPA-сутностей), константи замість магічних чисел.
- Коментарі англійською, коротко і лише «чому».
