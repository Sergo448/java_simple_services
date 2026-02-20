# Сервис 3: User Service (Spring Boot)

## Твой первый проект на Spring Boot!

В сервисах 1 и 2 ты писал всё вручную: HTTP-сервер, роутинг, парсинг JSON, хранение данных
в памяти. Это было полезно, чтобы понять, как всё работает «под капотом». Теперь пришло время
познакомиться с **Spring Boot** — фреймворком, который берёт на себя тонны рутинной работы.

**Структура файлов, которые тебе нужно реализовать:**

```
service-3-userservice/
├── pom.xml                          ← Maven-конфигурация с Spring Boot
├── Dockerfile                       ← Многоступенчатая сборка
├── src/main/resources/
│   └── application.yml              ← Конфигурация приложения
└── src/main/java/com/tutorial/userservice/
    ├── UserServiceApplication.java  ← Точка входа
    ├── model/
    │   └── User.java                ← JPA-сущность
    ├── repository/
    │   └── UserRepository.java      ← Spring Data JPA репозиторий
    ├── service/
    │   └── UserService.java         ← Бизнес-логика
    ├── controller/
    │   └── UserController.java      ← REST API
    └── exception/
        ├── UserNotFoundException.java    ← Кастомное исключение
        └── GlobalExceptionHandler.java   ← Глобальный обработчик ошибок
```

---

## Шаг 1: Цель и что ты узнаешь

### Что мы строим

REST API для управления пользователями (CRUD — Create, Read, Update, Delete).
Это классическая задача, но теперь — на Spring Boot вместо ручного кода.

### Что ты узнаешь в этом сервисе

1. **Spring Boot авто-конфигурация** — фреймворк сам настраивает сервер, подключение к БД,
   сериализацию JSON и многое другое. Тебе не нужно писать `HttpServer.create(...)` как
   в сервисе 1.

2. **Слоистая архитектура (Layered Architecture):**
   ```
   HTTP-запрос
       ↓
   Controller  — принимает запросы, возвращает ответы
       ↓
   Service     — бизнес-логика (валидация, правила, преобразования)
       ↓
   Repository  — работа с базой данных
       ↓
   Database    — хранение данных (H2 in-memory)
   ```
   Каждый слой отвечает за своё. Это стандартный паттерн в enterprise-разработке.

3. **JPA (Java Persistence API)** — стандарт для работы с базами данных через Java-объекты.
   Вместо написания SQL-запросов ты описываешь сущность (Entity) как обычный Java-класс
   с аннотациями, а Spring/Hibernate сам генерирует SQL.

4. **H2 Database** — встроенная in-memory база данных. Идеальна для разработки и обучения:
   не нужно ничего устанавливать, данные живут в памяти пока работает приложение.

5. **Bean Validation** — декларативная валидация через аннотации (`@NotBlank`, `@Email`, `@Min`).
   Вместо ручных `if`-проверок Spring сам валидирует входящие данные.

6. **Глобальная обработка ошибок** — единое место для обработки исключений вместо
   try-catch в каждом методе.

### Сравни с сервисами 1-2

| Что делали вручную (сервисы 1-2) | Что делает Spring Boot (сервис 3) |
|---|---|
| `HttpServer.create(new InetSocketAddress(port), 0)` | `server.port: 8083` в YAML |
| Ручной роутинг по путям и методам | `@GetMapping`, `@PostMapping` и т.д. |
| `Gson` для JSON вручную | Jackson автоматически |
| `ConcurrentHashMap` для хранения | JPA + H2 (настоящая база данных) |
| Ручные проверки `if (name == null)` | `@NotBlank`, `@Email` — аннотации валидации |
| try-catch в каждом хэндлере | `@ControllerAdvice` — единый обработчик |

Ты увидишь, насколько меньше кода нужно написать — Spring убирает огромное количество
шаблонного (boilerplate) кода.

---

## Шаг 2: Настройка pom.xml

### Концепция: Spring Boot Parent POM

В сервисах 1-2 ты сам указывал версии всех зависимостей. В Spring Boot есть
**parent POM** — родительский pom.xml, который уже содержит проверенные совместимые
версии сотен библиотек. Тебе нужно только указать parent, а версии подтянутся автоматически.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.1</version>
    <relativePath/>
</parent>
```

`<relativePath/>` означает «не ищи parent в файловой системе, скачай из Maven-репозитория».

### Концепция: Starter-зависимости

Spring Boot предоставляет **стартеры** — готовые наборы зависимостей для типичных задач.
Один стартер тянет за собой всё необходимое:

| Стартер | Что включает |
|---|---|
| `spring-boot-starter-web` | Встроенный Tomcat, Spring MVC, Jackson (JSON), всё для REST API |
| `spring-boot-starter-data-jpa` | Hibernate (ORM), Spring Data JPA, подключение к БД |
| `spring-boot-starter-validation` | Hibernate Validator — аннотации `@NotBlank`, `@Email` и т.д. |

Без стартеров тебе пришлось бы вручную добавлять 10-15 отдельных зависимостей
и следить за совместимостью версий.

### Что нужно написать

Открой файл `pom.xml`. Там сейчас комментарии-подсказки. Тебе нужно создать полноценный
Maven-проект со следующей структурой:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="...">
    <modelVersion>4.0.0</modelVersion>

    <!-- 1. Подключи Spring Boot Parent -->
    <parent>
        ...spring-boot-starter-parent 3.2.1...
    </parent>

    <!-- 2. Координаты проекта -->
    <groupId>com.tutorial</groupId>
    <artifactId>user-service</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <!-- 3. Свойства (версия Java) -->
    <properties>
        <java.version>22</java.version>
    </properties>

    <!-- 4. Зависимости -->
    <dependencies>
        <!-- Веб: Tomcat + Spring MVC + Jackson -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA: Hibernate + Spring Data -->
        <!-- artifactId: spring-boot-starter-data-jpa -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Валидация: @NotBlank, @Email и т.д. -->
        <!-- artifactId: spring-boot-starter-validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- H2 база данных (только для runtime, не для компиляции) -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>

    <!-- 5. Плагин для сборки исполняемого JAR -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Обрати внимание:**
- У `spring-boot-starter-*` зависимостей **нет тега `<version>`** — версия берётся из parent.
- У H2 стоит `<scope>runtime</scope>` — эта библиотека нужна только при запуске,
  а не при компиляции. Твой код не импортирует классы H2 напрямую — Spring сам найдёт
  и подключит H2 через auto-configuration.
- `spring-boot-maven-plugin` создаёт **исполняемый JAR** (fat JAR) — один файл,
  который содержит и твой код, и все зависимости, и встроенный Tomcat. Запуск одной
  командой: `java -jar target/user-service-1.0.0.jar`.

**Задание:** допиши недостающие зависимости (data-jpa и validation) по аналогии с web-стартером.

---

## Шаг 3: application.yml — конфигурация приложения

### Концепция: YAML-конфигурация

Spring Boot поддерживает два формата конфигурации: `application.properties` (key=value)
и `application.yml` (YAML). Мы используем YAML — он нагляднее для вложенных свойств.

**Важно:** в YAML отступы имеют значение! Используй **пробелы, НЕ табы**. Стандартный
отступ — 2 пробела.

### Что нужно написать

Открой `src/main/resources/application.yml` и напиши конфигурацию:

```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:h2:mem:userdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  h2:
    console:
      enabled: true
```

### Объяснение каждого свойства

**`server.port: 8083`**
Порт, на котором будет слушать встроенный Tomcat. По умолчанию Spring Boot использует 8080,
но мы меняем, чтобы не конфликтовать с другими сервисами.

**`spring.datasource.url: jdbc:h2:mem:userdb`**
JDBC URL для подключения к базе данных. Разбираем по частям:
- `jdbc:h2` — используем драйвер H2
- `mem:` — база данных в оперативной памяти (in-memory), данные исчезнут при остановке
- `userdb` — имя базы данных (можно назвать как угодно)

**`spring.datasource.driver-class-name: org.h2.Driver`**
Класс JDBC-драйвера. Spring мог бы определить его автоматически из URL, но лучше
указать явно для ясности.

**`spring.jpa.hibernate.ddl-auto: update`**
Это **очень важная** настройка! Она определяет, что Hibernate делает со схемой БД при запуске:
- `none` — ничего не делать (для production)
- `validate` — проверить, что схема в БД совпадает с Entity-классами
- `update` — **автоматически создать/обновить таблицы** по Entity-классам (идеально для разработки)
- `create` — пересоздавать таблицы при каждом запуске (данные теряются)
- `create-drop` — создать при запуске, удалить при остановке

Мы используем `update`: Hibernate посмотрит на твой класс `User.java`, увидит аннотацию
`@Entity` и **автоматически создаст таблицу** `users` с нужными колонками. Никакого SQL
писать не надо!

**`spring.jpa.show-sql: true`**
Показывает в консоли SQL-запросы, которые генерирует Hibernate. Очень полезно для обучения —
ты увидишь, что происходит «под капотом»:
```
Hibernate: insert into users (age,email,name) values (?,?,?)
Hibernate: select u1_0.id,u1_0.age,u1_0.email,u1_0.name from users u1_0
```

**`spring.h2.console.enabled: true`**
Включает веб-консоль H2 по адресу `http://localhost:8083/h2-console`.
Это графический интерфейс, где можно вручную выполнять SQL-запросы и смотреть данные
в таблицах. Для подключения используй тот же URL: `jdbc:h2:mem:userdb`.

---

## Шаг 4: UserServiceApplication.java — точка входа

### Концепция: @SpringBootApplication

Это самый короткий шаг, но за одной аннотацией скрывается мощный механизм.

`@SpringBootApplication` — это **комбинированная аннотация**, которая объединяет сразу три:

| Аннотация | Что делает |
|---|---|
| `@Configuration` | Этот класс — источник конфигурации (Spring Beans) |
| `@EnableAutoConfiguration` | Включает магию авто-конфигурации: Spring смотрит, какие библиотеки есть в classpath (Tomcat, Hibernate, H2 и т.д.) и автоматически настраивает их |
| `@ComponentScan` | Сканирует текущий пакет и все вложенные на наличие компонентов (`@Controller`, `@Service`, `@Repository`) |

### Что нужно написать

```java
// Добавь нужный import
// Добавь аннотацию @SpringBootApplication

public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

Всего одна аннотация и одна строка в `main` — и у тебя запускается полноценный веб-сервер
с подключением к базе данных! Сравни это с `App.java` из сервиса 1, где ты вручную создавал
`HttpServer`, регистрировал хэндлеры, настраивал executor...

**Важно:** класс `UserServiceApplication` должен находиться в **корневом пакете**
(`com.tutorial.userservice`). `@ComponentScan` сканирует этот пакет и все вложенные
(`model`, `repository`, `service`, `controller`, `exception`). Если положить класс
в другой пакет — Spring не найдёт твои компоненты.

---

## Шаг 5: User.java — JPA-сущность

### Концепция: ORM (Object-Relational Mapping)

ORM — это технология, которая связывает Java-объекты с таблицами в базе данных.
Вместо того чтобы писать SQL:

```sql
CREATE TABLE users (id BIGINT AUTO_INCREMENT, name VARCHAR(255), ...);
INSERT INTO users (name, email, age) VALUES ('Alice', 'alice@mail.com', 25);
```

Ты описываешь обычный Java-класс с аннотациями, а Hibernate (реализация JPA) сам:
- Создаёт таблицу (благодаря `ddl-auto: update`)
- Генерирует SQL для всех операций (INSERT, SELECT, UPDATE, DELETE)
- Маппит результаты SQL-запросов обратно в Java-объекты

### JPA-аннотации, которые тебе понадобятся

**Аннотации для класса и таблицы:**

```java
@Entity                        // Помечает класс как JPA-сущность
@Table(name = "users")         // Имя таблицы в БД (без @Table будет имя класса)
public class User {
    ...
}
```

Почему `@Table(name = "users")`, а не просто `User`? Потому что `user` — зарезервированное
слово в некоторых SQL-диалектах (включая H2). Безопаснее назвать таблицу `users`.

**Аннотации для первичного ключа:**

```java
@Id                                              // Это поле — первичный ключ
@GeneratedValue(strategy = GenerationType.IDENTITY) // Автоинкремент (БД сама назначает ID)
private Long id;
```

`GenerationType.IDENTITY` означает, что база данных сама генерирует ID
(автоинкремент: 1, 2, 3, ...). Тебе не надо его задавать при создании пользователя.

**Аннотации валидации (Bean Validation):**

```java
@NotBlank(message = "Имя не может быть пустым")
private String name;

@Email(message = "Некорректный формат email")
@NotBlank(message = "Email не может быть пустым")
private String email;

@Min(value = 0, message = "Возраст не может быть отрицательным")
@Max(value = 150, message = "Возраст не может превышать 150")
private Integer age;
```

Эти аннотации сами по себе ничего не делают — они начнут работать, когда в контроллере
ты используешь `@Valid` перед параметром.

### Что нужно написать

Тебе нужно создать полноценный Java-класс с:

1. **Аннотациями `@Entity` и `@Table`** на классе
2. **Четырьмя полями:** `id` (Long), `name` (String), `email` (String), `age` (Integer)
3. **Аннотациями для id:** `@Id` и `@GeneratedValue`
4. **Аннотациями валидации** на полях `name`, `email`, `age`
5. **Конструктором без аргументов** (обязательно для JPA! Hibernate создаёт объекты
   через рефлексию и вызывает конструктор без аргументов)
6. **Геттерами и сеттерами** для всех полей (Spring/Jackson использует их для
   сериализации/десериализации JSON)

**Подсказка по импортам:**
```java
import jakarta.persistence.*;       // @Entity, @Table, @Id, @GeneratedValue
import jakarta.validation.constraints.*;  // @NotBlank, @Email, @Min, @Max
```

**Обрати внимание:** в Spring Boot 3.x используется пакет `jakarta.*`, а не `javax.*`
(как было в старых версиях). Это связано с переходом Java EE в Jakarta EE.

---

## Шаг 6: UserRepository.java — репозиторий

### Концепция: Spring Data JPA — магия интерфейсов

Это, пожалуй, самая впечатляющая часть Spring Data. Вместо того чтобы писать DAO-класс
с кучей SQL-запросов (как ты делал `TaskStorage` в сервисе 2, только ещё хуже — с JDBC),
ты просто создаёшь **интерфейс** и наследуешь `JpaRepository`.

Spring **автоматически** создаёт реализацию этого интерфейса во время запуска приложения.
Тебе не нужно писать ни единого метода!

### Что ты получаешь бесплатно

`JpaRepository<User, Long>` даёт тебе:

| Метод | Что делает | Примерный SQL |
|---|---|---|
| `findAll()` | Все пользователи | `SELECT * FROM users` |
| `findById(Long id)` | По ID (возвращает `Optional<User>`) | `SELECT * FROM users WHERE id = ?` |
| `save(User user)` | Создать или обновить | `INSERT INTO...` или `UPDATE...` |
| `deleteById(Long id)` | Удалить по ID | `DELETE FROM users WHERE id = ?` |
| `count()` | Количество записей | `SELECT COUNT(*) FROM users` |
| `existsById(Long id)` | Существует ли запись | `SELECT ... WHERE id = ?` |

И ещё десятки методов. Все без единой строчки кода с твоей стороны!

### Что нужно написать

```java
// Не забудь import для JpaRepository и User

public interface UserRepository extends JpaRepository<User, Long> {
    // Здесь можно ничего не писать — все CRUD-методы уже есть!
}
```

Да, это **весь код** репозитория. Один интерфейс, одна строка `extends`, ноль методов.
Сравни с тем, сколько кода ты бы писал для JDBC (подключение, PreparedStatement,
ResultSet, маппинг, закрытие ресурсов...).

### Бонус: кастомные методы по соглашению имён

Spring Data умеет **генерировать запросы из имени метода**. Если тебе нужен поиск
по email, просто добавь:

```java
Optional<User> findByEmail(String email);
```

Spring увидит имя `findByEmail`, поймёт что надо искать в поле `email`, и автоматически
сгенерирует:
```sql
SELECT * FROM users WHERE email = ?
```

Другие примеры (необязательно реализовывать, просто для понимания мощи Spring Data):
```java
List<User> findByName(String name);
List<User> findByAgeGreaterThan(Integer age);
List<User> findByNameContainingIgnoreCase(String fragment);
```

**Задание:** создай интерфейс `UserRepository`, наследующий `JpaRepository<User, Long>`.
При желании добавь метод `findByEmail`.

---

## Шаг 7: UserService.java — сервисный слой

### Концепция: Зачем нужен Service-слой?

Может показаться, что Service — это лишний слой, ведь контроллер мог бы обращаться
к репозиторию напрямую. Но в реальных проектах Service-слой:

1. **Содержит бизнес-логику** — валидация, преобразования, вычисления
2. **Координирует работу нескольких репозиториев** — например, при создании заказа
   нужно обновить и таблицу заказов, и таблицу склада
3. **Управляет транзакциями** — несколько операций с БД в одной транзакции
4. **Упрощает тестирование** — можно тестировать бизнес-логику отдельно от HTTP

### Концепция: Dependency Injection через конструктор

В Spring есть три способа внедрения зависимостей:

```java
// СПОСОБ 1: Через поле (НЕ рекомендуется)
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;  // Плохо: нельзя сделать final
}

// СПОСОБ 2: Через конструктор (РЕКОМЕНДУЕТСЯ)
@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// СПОСОБ 3: Через сеттер (редко используется)
```

**Почему конструктор лучше:**
- Поле можно объявить `final` — гарантия неизменяемости
- Зависимости видны явно (не спрятаны за аннотацией)
- Легче тестировать — можно передать mock в конструктор
- Если в классе **один конструктор**, Spring автоматически использует его для DI —
  даже без `@Autowired`!

### Методы, которые нужно реализовать

**1. `getAllUsers()`**
```java
public List<User> getAllUsers() {
    // Вызови нужный метод репозитория, возвращающий все записи
}
```

**2. `getUserById(Long id)`**
```java
public User getUserById(Long id) {
    // repository.findById(id) возвращает Optional<User>
    // Если пользователь не найден — брось UserNotFoundException
    // Подсказка: используй .orElseThrow(...)
}
```

`Optional` — это контейнер, который может содержать значение или быть пустым.
Метод `.orElseThrow()` позволяет указать, какое исключение бросить, если значение
отсутствует:

```java
// Пример работы с Optional
Optional<User> optional = repository.findById(id);
User user = optional.orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
```

**3. `createUser(User user)`**
```java
public User createUser(User user) {
    // repository.save(user) сохраняет и возвращает сущность с назначенным id
}
```

**4. `updateUser(Long id, User userDetails)`**
```java
public User updateUser(Long id, User userDetails) {
    // 1. Найди существующего пользователя по id (или брось исключение)
    // 2. Обнови поля: name, email, age
    // 3. Сохрани и верни обновлённого пользователя
}
```

Почему нельзя просто `save(userDetails)`? Потому что `userDetails` пришёл из запроса —
у него может не быть `id`, или `id` будет другим. Нужно **найти** существующую сущность
и **обновить** её поля.

**5. `deleteUser(Long id)`**
```java
public void deleteUser(Long id) {
    // 1. Проверь, что пользователь существует (или брось исключение)
    //    Подсказка: можно использовать getUserById(id) — он сам бросит исключение
    // 2. Удали по id
}
```

### Что нужно написать

Аннотируй класс `@Service`, добавь конструктор с `UserRepository`, реализуй пять
методов. Не забудь импорты:

```java
import org.springframework.stereotype.Service;
import java.util.List;
```

---

## Шаг 8: UserController.java — REST-контроллер

### Концепция: @RestController

В сервисах 1-2 ты создавал `HttpHandler` и вручную разбирал URL, метод запроса,
читал тело, формировал ответ. В Spring Boot **всё декларативно** через аннотации:

```java
@RestController                    // Этот класс — REST-контроллер
@RequestMapping("/api/users")      // Базовый путь для всех эндпоинтов
public class UserController {
    ...
}
```

`@RestController` = `@Controller` + `@ResponseBody`. Это значит:
- Spring регистрирует этот класс как обработчик HTTP-запросов
- Все возвращаемые значения автоматически сериализуются в JSON (через Jackson)

### HTTP-аннотации

| Аннотация | HTTP-метод | Пример |
|---|---|---|
| `@GetMapping` | GET | Получение данных |
| `@PostMapping` | POST | Создание |
| `@PutMapping` | PUT | Обновление |
| `@DeleteMapping` | DELETE | Удаление |

### Параметры и тело запроса

```java
// Параметр из URL-пути: /api/users/42 → id = 42
@GetMapping("/{id}")
public User getById(@PathVariable Long id) { ... }

// Тело JSON-запроса автоматически десериализуется в объект User
// @Valid включает валидацию (аннотации @NotBlank, @Email и т.д.)
@PostMapping
public User create(@Valid @RequestBody User user) { ... }
```

### ResponseEntity — контроль HTTP-ответа

`ResponseEntity` позволяет задать не только тело ответа, но и HTTP-статус код:

```java
// 200 OK (по умолчанию)
return ResponseEntity.ok(user);

// 201 Created
return ResponseEntity.status(201).body(createdUser);
// или
return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);

// 204 No Content (для DELETE — тело не нужно)
return ResponseEntity.noContent().build();
```

### Эндпоинты, которые нужно реализовать

**1. GET /api/users — список всех пользователей**
```java
@GetMapping
public ResponseEntity<List<User>> getAllUsers() {
    // Вызови сервис, верни 200 + список
}
```

**2. GET /api/users/{id} — пользователь по ID**
```java
@GetMapping("/{id}")
public ResponseEntity<User> getUserById(@PathVariable Long id) {
    // Вызови сервис. Если не найден — UserNotFoundException полетит
    // в GlobalExceptionHandler (шаг 9), тебе тут не нужен try-catch
}
```

**3. POST /api/users — создание пользователя**
```java
@PostMapping
public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
    // Вызови сервис, верни 201 + созданного пользователя
    // Подсказка: ResponseEntity.status(201).body(...)
}
```

`@Valid` перед `@RequestBody` активирует валидацию. Если данные не прошли проверку
(пустое имя, невалидный email и т.д.), Spring автоматически выбросит
`MethodArgumentNotValidException` **до** вызова твоего метода. Обработаешь это
в GlobalExceptionHandler (шаг 9).

**4. PUT /api/users/{id} — обновление**
```java
@PutMapping("/{id}")
public ResponseEntity<User> updateUser(@PathVariable Long id,
                                        @Valid @RequestBody User user) {
    // Вызови сервис, верни 200 + обновлённого
}
```

**5. DELETE /api/users/{id} — удаление**
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    // Вызови сервис, верни 204 No Content
    // Подсказка: ResponseEntity.noContent().build()
}
```

### Что нужно написать

Аннотируй класс `@RestController` и `@RequestMapping("/api/users")`, внедри
`UserService` через конструктор, реализуй пять эндпоинтов. Не забудь импорты:

```java
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
```

---

## Шаг 9: Обработка ошибок

### 9.1: UserNotFoundException

Простой класс, наследующий `RuntimeException`. Нужен один конструктор, принимающий
сообщение (String) и передающий его в `super(message)`.

```java
public class UserNotFoundException extends RuntimeException {
    // Один конструктор: принимает String message, вызывает super(message)
}
```

Почему `RuntimeException`, а не `Exception`? Потому что `RuntimeException` — непроверяемое
(unchecked) исключение. Его не нужно объявлять в `throws` и оборачивать в try-catch
на каждом уровне. Spring перехватит его через `@ControllerAdvice`.

### 9.2: GlobalExceptionHandler

### Концепция: @ControllerAdvice

`@ControllerAdvice` — это специальный компонент Spring, который «советует» (advises)
всем контроллерам, как обрабатывать ошибки. Вместо того чтобы писать try-catch
в каждом методе контроллера, ты описываешь обработку в одном месте.

**Как это работает:**

```
Запрос → Controller → метод бросает исключение
                            ↓
         Spring ищет подходящий @ExceptionHandler в @ControllerAdvice
                            ↓
         Находит → вызывает обработчик → возвращает ответ клиенту
```

### Структура обработчика

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    // Обработка "пользователь не найден" → 404
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<...> handleUserNotFound(UserNotFoundException ex) {
        // Верни 404 с JSON-телом, например:
        // { "error": "User not found with id: 42" }
    }

    // Обработка ошибок валидации → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<...> handleValidation(MethodArgumentNotValidException ex) {
        // Достань ошибки валидации из объекта ex
        // Верни 400 с описанием ошибок
    }
}
```

### Обработка UserNotFoundException (404)

Для JSON-ответа с ошибкой можно использовать `Map<String, String>`:

```java
// Подсказка: верни ResponseEntity со статусом NOT_FOUND и телом-Map
Map<String, String> body = new HashMap<>();
body.put("error", ex.getMessage());
return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
```

Клиент получит:
```json
{
    "error": "User not found with id: 42"
}
```

### Обработка MethodArgumentNotValidException (400)

Когда `@Valid` обнаруживает невалидные данные, Spring бросает
`MethodArgumentNotValidException`. Из него можно извлечь все ошибки:

```java
// Подсказка: как извлечь ошибки валидации
Map<String, String> errors = new HashMap<>();
ex.getBindingResult().getFieldErrors().forEach(error ->
    errors.put(error.getField(), error.getDefaultMessage())
);
// errors будет содержать: {"name": "Имя не может быть пустым", "email": "..."}
```

Клиент получит:
```json
{
    "name": "Имя не может быть пустым",
    "email": "Некорректный формат email"
}
```

### Что нужно написать

1. `UserNotFoundException` — класс с конструктором
2. `GlobalExceptionHandler` — аннотируй `@ControllerAdvice`, добавь два метода
   с `@ExceptionHandler`

Не забудь импорты:
```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.HashMap;
import java.util.Map;
```

---

## Шаг 10: Dockerfile — контейнеризация

### Multi-stage build для Spring Boot

Паттерн тот же, что в сервисах 1-2, но с учётом Spring Boot:

**Stage 1 — сборка (builder):**
- Базовый образ: `maven:3.9-eclipse-temurin-17`
- Рабочая директория: `/app`
- Копируй `pom.xml` и скачай зависимости (`mvn dependency:go-offline`)
- Копируй исходный код (`src/`) и собери JAR (`mvn clean package -DskipTests`)

**Stage 2 — запуск:**
- Базовый образ: `eclipse-temurin:17-jre` (только JRE, не полный JDK — образ меньше)
- Рабочая директория: `/app`
- Копируй JAR-файл из builder-стадии
- `EXPOSE 8083`
- Команда запуска: `java -jar user-service-1.0.0.jar`

### Подсказка по структуре

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/user-service-1.0.0.jar ./app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Почему `dependency:go-offline` отдельным шагом?** Это хак для Docker-кэширования.
Слой с зависимостями закэшируется, и при изменении только исходного кода Docker не будет
заново скачивать все зависимости. Сильно ускоряет повторные сборки.

**Почему `-DskipTests`?** В Docker-сборке обычно тесты не запускают — они выполняются
на CI/CD до сборки образа.

---

## Шаг 11: Сборка и запуск

### Локальный запуск (без Docker)

```bash
# 1. Перейди в директорию сервиса
cd service-3-userservice

# 2. Собери проект
mvn clean package

# Если тестов нет или хочешь пропустить:
mvn clean package -DskipTests

# 3. Запусти JAR
java -jar target/user-service-1.0.0.jar
```

В консоли ты увидишь логи Spring Boot:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

Started UserServiceApplication in 2.3 seconds
```

### H2 Console

После запуска открой в браузере: `http://localhost:8083/h2-console`

Настройки подключения:
- **JDBC URL:** `jdbc:h2:mem:userdb`
- **User Name:** `sa`
- **Password:** (оставь пустым)

Нажми **Connect** — увидишь таблицу `USERS`, которую Hibernate создал автоматически.
Можешь выполнить SQL-запрос: `SELECT * FROM USERS;`

### Запуск в Docker

```bash
# Собрать образ
docker build -t user-service .

# Запустить контейнер
docker run -p 8083:8083 user-service

# Запустить в фоне
docker run -d -p 8083:8083 --name user-service user-service

# Посмотреть логи
docker logs user-service

# Остановить и удалить
docker stop user-service && docker rm user-service
```

---

## Шаг 12: Тестирование (curl)

Сервис запущен? Давай протестируем все эндпоинты!

### 12.1: Создание пользователей (POST)

```bash
# Создай первого пользователя
curl -s -X POST http://localhost:8083/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "alice@example.com", "age": 25}'
```

Ожидаемый ответ (201 Created):
```json
{
    "id": 1,
    "name": "Alice",
    "email": "alice@example.com",
    "age": 25
}
```

Обрати внимание: `id` назначился автоматически!

```bash
# Создай второго пользователя
curl -s -X POST http://localhost:8083/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Bob", "email": "bob@example.com", "age": 30}'
```

### 12.2: Получение всех пользователей (GET)

```bash
curl -s http://localhost:8083/api/users
```

Ожидаемый ответ (200 OK):
```json
[
    {"id": 1, "name": "Alice", "email": "alice@example.com", "age": 25},
    {"id": 2, "name": "Bob", "email": "bob@example.com", "age": 30}
]
```

### 12.3: Получение по ID (GET)

```bash
curl -s http://localhost:8083/api/users/1
```

Ожидаемый ответ (200 OK):
```json
{"id": 1, "name": "Alice", "email": "alice@example.com", "age": 25}
```

### 12.4: Обновление пользователя (PUT)

```bash
curl -s -X PUT http://localhost:8083/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice Updated", "email": "alice.new@example.com", "age": 26}'
```

Ожидаемый ответ (200 OK):
```json
{"id": 1, "name": "Alice Updated", "email": "alice.new@example.com", "age": 26}
```

### 12.5: Удаление пользователя (DELETE)

```bash
curl -s -X DELETE http://localhost:8083/api/users/2
```

Ожидаемый ответ: 204 No Content (пустое тело).

Проверь, что Bob удалён:
```bash
curl -s http://localhost:8083/api/users
```

Должен остаться только Alice.

### 12.6: Проверка ошибок — невалидные данные (400)

```bash
# Пустое имя
curl -s -X POST http://localhost:8083/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "", "email": "test@example.com", "age": 25}'
```

Ожидаемый ответ (400 Bad Request):
```json
{"name": "Имя не может быть пустым"}
```

```bash
# Невалидный email
curl -s -X POST http://localhost:8083/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "not-an-email", "age": 25}'
```

Ожидаемый ответ (400 Bad Request):
```json
{"email": "Некорректный формат email"}
```

```bash
# Отрицательный возраст
curl -s -X POST http://localhost:8083/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "email": "test@example.com", "age": -5}'
```

Ожидаемый ответ (400 Bad Request):
```json
{"age": "Возраст не может быть отрицательным"}
```

```bash
# Несколько ошибок сразу
curl -s -X POST http://localhost:8083/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "", "email": "bad-email", "age": -1}'
```

Ожидаемый ответ (400) — несколько ошибок в одном ответе:
```json
{
    "name": "Имя не может быть пустым",
    "email": "Некорректный формат email",
    "age": "Возраст не может быть отрицательным"
}
```

### 12.7: Проверка ошибок — несуществующий ID (404)

```bash
curl -s http://localhost:8083/api/users/999
```

Ожидаемый ответ (404 Not Found):
```json
{"error": "User not found with id: 999"}
```

```bash
# Удаление несуществующего
curl -s -X DELETE http://localhost:8083/api/users/999
```

Ожидаемый ответ (404 Not Found):
```json
{"error": "User not found with id: 999"}
```

---

## Шаг 13: Бонусные задания

Если основной функционал работает — попробуй расширить сервис. Эти задания опциональны,
но полезны для глубокого понимания Spring Boot.

### 13.1: Пагинация (Pageable)

Когда пользователей станет тысячи, возвращать всех сразу — плохая идея. Spring Data
поддерживает пагинацию из коробки.

**Подсказки:**
- В контроллере добавь параметр `Pageable` к методу `getAllUsers`
- `JpaRepository` уже имеет метод `findAll(Pageable pageable)`, возвращающий `Page<User>`
- Клиент сможет делать: `GET /api/users?page=0&size=10&sort=name,asc`
- Посмотри документацию по `org.springframework.data.domain.Pageable`

### 13.2: Поиск по имени и email

**Подсказки:**
- Добавь в `UserRepository` методы:
  ```java
  List<User> findByNameContainingIgnoreCase(String name);
  ```
- Добавь эндпоинт:
  ```java
  @GetMapping("/search")
  public List<User> search(@RequestParam(required = false) String name) { ... }
  ```
- `@RequestParam` читает query-параметры: `GET /api/users/search?name=ali`

### 13.3: DTO-паттерн (Data Transfer Object)

Сейчас ты используешь Entity-класс `User` и как JPA-сущность, и как объект для
JSON-запросов/ответов. В реальных проектах это плохая практика:

- Клиент может отправить `id` в JSON и перезаписать чужого пользователя
- Ты можешь случайно вернуть чувствительные данные (пароль, внутренние поля)

**Решение:** создай отдельные классы:
- `UserCreateRequest` — для создания (без id)
- `UserUpdateRequest` — для обновления (без id)
- `UserResponse` — для ответа (можно без чувствительных полей)

**Подсказки:**
- Создай пакет `dto` рядом с `model`
- В сервисе конвертируй между DTO и Entity
- В контроллере принимай DTO вместо Entity

### 13.4: Временные метки (createdAt / updatedAt)

Добавь автоматическое отслеживание времени создания и обновления.

**Подсказки:**
- Добавь поля в `User`:
  ```java
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  ```
- Используй аннотации JPA:
  ```java
  @Column(updatable = false)  // Не обновлять при UPDATE
  private LocalDateTime createdAt;
  ```
- В методах `createUser` и `updateUser` устанавливай время:
  ```java
  user.setCreatedAt(LocalDateTime.now());  // Только при создании
  user.setUpdatedAt(LocalDateTime.now());  // При создании и обновлении
  ```
- Продвинутый вариант: используй `@PrePersist` и `@PreUpdate` — JPA lifecycle callbacks

---

## Чеклист завершения

Перед тем как считать сервис готовым, проверь:

- [ ] `mvn clean package` проходит без ошибок
- [ ] Приложение стартует на порту 8083
- [ ] POST `/api/users` создаёт пользователя и возвращает 201
- [ ] GET `/api/users` возвращает список всех пользователей
- [ ] GET `/api/users/{id}` возвращает пользователя по ID
- [ ] PUT `/api/users/{id}` обновляет пользователя
- [ ] DELETE `/api/users/{id}` удаляет и возвращает 204
- [ ] POST с невалидными данными возвращает 400 с описанием ошибок
- [ ] GET/PUT/DELETE с несуществующим ID возвращает 404
- [ ] H2 Console доступна по `/h2-console`
- [ ] SQL-запросы видны в логах (show-sql: true)
- [ ] Docker-образ собирается и контейнер запускается
