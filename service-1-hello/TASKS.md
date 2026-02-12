# Service-1-Hello: HTTP-сервер на чистой Java

---

## Шаг 1: Цель и что ты узнаешь

В этом задании ты построишь **HTTP-сервер с нуля**, используя только встроенные средства Java --- без Spring, без фреймворков, без магии.

### Зачем это нужно?

Когда ты работаешь со Spring Boot, он скрывает от тебя огромное количество деталей: как приходит HTTP-запрос, как парсятся заголовки, как формируется ответ. Ты вызываешь `@GetMapping` --- и всё работает. Но ты не понимаешь **почему** это работает.

В этом проекте ты разберёшься:

- Как устроен HTTP-сервер изнутри --- приём соединений, маршрутизация запросов, отправка ответов
- Что такое `HttpHandler` и как он обрабатывает входящие запросы
- Как вручную разбирать query-параметры из URL
- Как формировать JSON-ответ и отправлять его клиенту с правильными заголовками
- Как собирать Java-проект через Maven и упаковывать его в Docker-контейнер

### Что ты будешь строить

Сервер на порту **8081** с тремя эндпоинтами:

| Эндпоинт | Метод | Описание |
|-----------|-------|----------|
| `/hello` | GET | Приветствие, опционально принимает `?name=` |
| `/time` | GET | Текущая дата и время сервера |
| `/health` | GET | Статус здоровья сервиса и uptime |

### Структура файлов

```
service-1-hello/
├── pom.xml                              # Конфигурация Maven
├── Dockerfile                           # Сборка Docker-образа
└── src/main/java/com/tutorial/hello/
    ├── App.java                         # Точка входа, запуск сервера
    └── handlers/
        ├── HelloHandler.java            # Обработчик /hello
        ├── TimeHandler.java             # Обработчик /time
        └── HealthHandler.java           # Обработчик /health
```

---

## Шаг 2: Настройка pom.xml

### Что такое pom.xml?

`pom.xml` (Project Object Model) --- это главный файл конфигурации Maven. Он описывает:

- **Кто ты** --- координаты твоего проекта
- **Что тебе нужно** --- зависимости (библиотеки)
- **Как собирать** --- плагины для сборки

### Координаты проекта

Каждый Maven-проект идентифицируется тремя координатами:

```xml
<groupId>com.tutorial</groupId>
<artifactId>hello-service</artifactId>
<version>1.0-SNAPSHOT</version>
```

- **groupId** --- обратное доменное имя твоей организации. Это пространство имён, чтобы твой проект не путался с чужими. Например, `com.tutorial` означает, что проект принадлежит организации `tutorial.com`.
- **artifactId** --- имя конкретного проекта/модуля. Это то, как будет называться твой JAR-файл. `hello-service` превратится в `hello-service-1.0-SNAPSHOT.jar`.
- **version** --- версия проекта. `SNAPSHOT` означает, что это "разрабатываемая" версия, ещё не финальный релиз.

### Свойства

Задай версию Java и кодировку. Без указания кодировки Maven будет ругаться предупреждениями:

```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

### Зависимости

Тебе нужна одна внешняя библиотека --- **Gson** от Google для работы с JSON:

```xml
<dependencies>
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
</dependencies>
```

Когда ты указываешь зависимость, Maven автоматически скачает её из центрального репозитория (Maven Central) и добавит в classpath при компиляции и запуске.

### Плагины сборки

Тебе нужны два плагина. Оба размещаются внутри секции `<build><plugins>...</plugins></build>`.

#### maven-jar-plugin

Этот плагин указывает, какой класс является точкой входа (содержит `main`):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>3.3.0</version>
    <configuration>
        <archive>
            <manifest>
                <mainClass>com.tutorial.hello.App</mainClass>
            </manifest>
        </archive>
    </configuration>
</plugin>
```

Без этой настройки `java -jar` не будет знать, какой класс запускать.

#### maven-shade-plugin

Этот плагин создаёт **fat JAR** (uber JAR) --- один JAR-файл, в который упакованы и твой код, и все зависимости (Gson). Без него при запуске `java -jar` ты получишь `ClassNotFoundException`, потому что Gson не будет найден.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

`<phase>package</phase>` означает, что плагин сработает на этапе `mvn package` --- когда Maven формирует JAR-файл.

### Задание

Создай файл `pom.xml` в корне `service-1-hello/`, собрав все секции выше в единый файл. Обёртка выглядит так:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Сюда вставь координаты, свойства, зависимости и плагины -->

</project>
```

---

## Шаг 3: App.java --- точка входа

### Что такое com.sun.net.httpserver.HttpServer?

Это **встроенный HTTP-сервер**, который идёт вместе с JDK. Он не требует никаких внешних библиотек. Да, он простой. Да, он не предназначен для продакшена. Но он идеально подходит, чтобы понять, как работает HTTP на уровне Java.

### Ключевые вызовы API

Вот четыре шага, которые тебе нужны для запуска сервера:

#### 1. Создание сервера

```java
HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
```

- `InetSocketAddress(8081)` --- сервер будет слушать порт 8081
- Второй аргумент `0` --- размер очереди ожидающих соединений (0 = системное значение по умолчанию)

#### 2. Регистрация маршрутов

```java
server.createContext("/hello", new HelloHandler());
```

Метод `createContext` связывает URL-путь с обработчиком. Когда придёт запрос на `/hello`, сервер вызовет метод `handle()` у `HelloHandler`.

Тебе нужно зарегистрировать три маршрута: `/hello`, `/time`, `/health`.

#### 3. Настройка executor

```java
server.setExecutor(null);
```

`null` означает использование стандартного executor (каждый запрос обрабатывается в своём потоке). Для нашего учебного сервера этого достаточно.

#### 4. Запуск

```java
server.start();
```

После этого вызова сервер начинает принимать HTTP-запросы.

### Задание

Создай файл `src/main/java/com/tutorial/hello/App.java`:

- Объяви класс `App` в пакете `com.tutorial.hello`
- В методе `main` создай сервер на порту 8081
- Зарегистрируй три контекста для трёх обработчиков
- Не забудь вывести в консоль сообщение, что сервер запустился (например, через `System.out.println`)
- Импортируй нужные классы: `com.sun.net.httpserver.HttpServer`, `java.net.InetSocketAddress`, а также свои обработчики из пакета `handlers`

---

## Шаг 4: Обработчики (Handlers)

### Интерфейс HttpHandler

Каждый обработчик --- это класс, реализующий интерфейс `HttpHandler`:

```java
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public class MyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Здесь вся логика обработки запроса
    }
}
```

Метод `handle` вызывается каждый раз, когда приходит запрос на зарегистрированный путь. Объект `HttpExchange` содержит всю информацию о запросе и позволяет отправить ответ.

### Как отправить JSON-ответ

Это базовый паттерн, который ты будешь использовать во всех обработчиках:

```java
String jsonResponse = /* ... твой JSON ... */;
byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

exchange.getResponseHeaders().set("Content-Type", "application/json");
exchange.sendResponseHeaders(200, responseBytes.length);

OutputStream os = exchange.getResponseBody();
os.write(responseBytes);
os.close();
```

Обрати внимание на порядок: **сначала** заголовки, **потом** `sendResponseHeaders`, **потом** запись в body. Если перепутаешь порядок --- получишь ошибку.

### Как разобрать query-параметры

URL вида `/hello?name=Java` содержит query-строку `name=Java`. Вот как её разобрать:

```java
String query = exchange.getRequestURI().getQuery(); // "name=Java" или null
```

`getQuery()` возвращает `null`, если параметров нет. Не забудь это проверить!

Если параметров несколько (например, `?name=Java&age=25`), тебе нужно разбить строку по `&`, а затем каждую пару по `=`:

```java
// Пример парсинга одного параметра из query-строки:
// query = "name=Java"
// query.split("=") даст ["name", "Java"]
```

Подумай, как обработать случай, когда `query == null`.

---

### HelloHandler (GET /hello)

**Путь:** `/hello`
**Метод:** GET
**Query-параметры:** `name` (необязательный)

**Логика:**
- Если передан `?name=Вася`, приветствуй по имени
- Если параметр не передан, используй `"World"` по умолчанию
- Верни JSON с полями `message` и `service`

**Ожидаемый ответ без параметра:**

```json
{
  "message": "Hello, World!",
  "service": "hello-service"
}
```

**Ожидаемый ответ с `?name=Java`:**

```json
{
  "message": "Hello, Java!",
  "service": "hello-service"
}
```

**Подсказки:**
- Получи query-строку через `exchange.getRequestURI().getQuery()`
- Проверь, что query не `null` и что он содержит `name=`
- Для формирования JSON используй Gson (см. Шаг 6)

---

### TimeHandler (GET /time)

**Путь:** `/time`
**Метод:** GET

**Логика:**
- Получи текущее время через `LocalDateTime.now()`
- Верни JSON с полями `currentTime`, `date`, `time`

**Ожидаемый ответ:**

```json
{
  "currentTime": "2025-01-15T14:30:45.123",
  "date": "2025-01-15",
  "time": "14:30:45"
}
```

**Подсказки:**
- Используй `LocalDateTime.now()` для получения текущего момента
- Для форматирования даты и времени по отдельности используй `DateTimeFormatter`:
  ```java
  DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
  String dateStr = now.format(dateFormatter);
  ```
- Для `currentTime` можно использовать `now.toString()`

**Необходимые импорты:**
- `java.time.LocalDateTime`
- `java.time.format.DateTimeFormatter`

---

### HealthHandler (GET /health)

**Путь:** `/health`
**Метод:** GET

**Логика:**
- Верни статус здоровья сервиса
- Посчитай uptime (время работы) через `ManagementFactory`
- Верни JSON с полями `status`, `service`, `uptimeSeconds`

**Ожидаемый ответ:**

```json
{
  "status": "UP",
  "service": "hello-service",
  "uptimeSeconds": 42
}
```

**Подсказки:**
- Для получения uptime используй `ManagementFactory.getRuntimeMXBean()`:
  ```java
  long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
  ```
  Этот метод возвращает время в **миллисекундах**. Раздели на 1000, чтобы получить секунды.
- Не забудь импорт: `java.lang.management.ManagementFactory`

---

## Шаг 5: Как работает HttpExchange --- шпаргалка

`HttpExchange` --- это объект, через который ты общаешься с клиентом. Вот самые важные методы:

### Получение информации о запросе

```java
// HTTP-метод (GET, POST, PUT, DELETE)
String method = exchange.getRequestMethod();

// Полный URI запроса (например, /hello?name=Java)
URI uri = exchange.getRequestURI();

// Только путь (например, /hello)
String path = exchange.getRequestURI().getPath();

// Только query-строка (например, name=Java), может быть null
String query = exchange.getRequestURI().getQuery();

// Заголовки запроса
Headers requestHeaders = exchange.getRequestHeaders();
String contentType = requestHeaders.getFirst("Content-Type");
```

### Отправка ответа

```java
// 1. Установить заголовки ответа
exchange.getResponseHeaders().set("Content-Type", "application/json");

// 2. Отправить статус-код и длину тела ответа
//    200 = OK, responseBytes.length = размер тела в байтах
exchange.sendResponseHeaders(200, responseBytes.length);

// 3. Записать тело ответа и закрыть поток
OutputStream os = exchange.getResponseBody();
os.write(responseBytes);
os.close();
```

### Распространённые HTTP-коды

| Код | Значение | Когда использовать |
|-----|----------|-------------------|
| 200 | OK | Запрос успешно обработан |
| 400 | Bad Request | Клиент отправил некорректный запрос |
| 404 | Not Found | Запрошенный ресурс не найден |
| 405 | Method Not Allowed | Неподдерживаемый HTTP-метод |
| 500 | Internal Server Error | Ошибка на стороне сервера |

### Проверка HTTP-метода

Полезный паттерн --- отклонять запросы с неправильным методом:

```java
if (!"GET".equals(exchange.getRequestMethod())) {
    String error = "{\"error\": \"Method not allowed\"}";
    byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(405, errorBytes.length);
    OutputStream os = exchange.getResponseBody();
    os.write(errorBytes);
    os.close();
    return; // Важно! Прекращаем обработку
}
```

Попробуй вынести отправку ответа в отдельный вспомогательный метод, чтобы не повторять один и тот же код в каждом обработчике.

---

## Шаг 6: Работа с JSON через Gson

### Что такое Gson?

Gson --- это библиотека от Google для преобразования Java-объектов в JSON и обратно. Мы используем её для формирования JSON-ответов.

### Создание экземпляра Gson

```java
import com.google.gson.Gson;

// Обычный --- компактный JSON в одну строку
Gson gson = new Gson();

// Красивый --- JSON с отступами и переносами строк (удобно для отладки)
Gson gson = new GsonBuilder().setPrettyPrinting().create();
```

Рекомендуется использовать `GsonBuilder().setPrettyPrinting()`, чтобы ответы были удобочитаемы. Не забудь импортировать `com.google.gson.GsonBuilder`.

### Формирование JSON из Map

Самый простой способ создать JSON --- использовать `Map` и `gson.toJson()`:

```java
Gson gson = new GsonBuilder().setPrettyPrinting().create();

Map<String, Object> response = new HashMap<>();
response.put("message", "Hello, World!");
response.put("service", "hello-service");

String json = gson.toJson(response);
// Результат:
// {
//   "message": "Hello, World!",
//   "service": "hello-service"
// }
```

Если ты используешь Java 11+, можно использовать `Map.of()` для создания неизменяемой карты:

```java
Map<String, Object> response = Map.of(
    "message", "Hello, World!",
    "service", "hello-service"
);
String json = gson.toJson(response);
```

> **Важно:** `Map.of()` создаёт неизменяемую карту и не гарантирует порядок ключей. Если порядок полей в JSON важен, используй `LinkedHashMap`.

### Где создавать Gson?

Создавай экземпляр `Gson` один раз как поле класса, а не в каждом вызове `handle()`:

```java
public class HelloHandler implements HttpHandler {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Используй this.gson для формирования JSON
    }
}
```

Gson потокобезопасен, поэтому один экземпляр можно использовать из нескольких потоков.

---

## Шаг 7: Dockerfile

### Что такое многоступенчатая сборка (multi-stage build)?

Docker позволяет использовать несколько этапов (`FROM`) в одном Dockerfile. Это даёт два преимущества:

1. **Этап сборки** --- содержит JDK, Maven, исходники, все инструменты для компиляции
2. **Этап запуска** --- содержит только JRE и скомпилированный JAR

Финальный образ будет маленьким, потому что в нём нет ни Maven, ни исходного кода, ни JDK --- только то, что нужно для запуска.

### Структура Dockerfile

```dockerfile
# ======= Этап 1: Сборка =======
FROM maven:3.9-eclipse-temurin-17 AS builder

# Рабочая директория внутри контейнера
WORKDIR /app

# Сначала копируем только pom.xml и скачиваем зависимости
# Это отдельный слой --- он закешируется, если pom.xml не менялся
COPY pom.xml .
RUN mvn dependency:go-offline

# Теперь копируем исходный код и собираем
COPY src ./src
RUN mvn clean package -DskipTests

# ======= Этап 2: Запуск =======
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Копируем JAR из этапа сборки
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]
```

### Разбор ключевых команд

| Команда | Что делает |
|---------|-----------|
| `FROM image AS name` | Начинает новый этап сборки на базе указанного образа |
| `WORKDIR /app` | Устанавливает рабочую директорию (создаёт, если не существует) |
| `COPY src dest` | Копирует файлы из хост-машины в контейнер |
| `COPY --from=builder` | Копирует файлы из другого этапа сборки |
| `RUN command` | Выполняет команду во время сборки образа |
| `EXPOSE port` | Документирует, какой порт использует приложение |
| `CMD [...]` | Команда по умолчанию при запуске контейнера |

### Зачем двухэтапная сборка?

Сравни размер образов:

- Одноэтапный (JDK + Maven + код + JAR): **~800 МБ**
- Двухэтапный (только JRE + JAR): **~180 МБ**

### Задание

Создай `Dockerfile` в корне `service-1-hello/`. Используй структуру выше как ориентир. Обрати внимание на порядок `COPY` --- сначала `pom.xml`, потом `src`. Это важно для кеширования слоёв Docker.

---

## Шаг 8: Сборка и запуск

### Локальная сборка через Maven

Открой терминал в директории `service-1-hello/` и выполни:

```bash
# Собрать проект (компиляция + упаковка в JAR)
mvn clean package
```

- `clean` --- удаляет директорию `target/` с результатами предыдущей сборки
- `package` --- компилирует код, скачивает зависимости, запускает тесты, создаёт JAR

Если сборка прошла успешно, ты увидишь `BUILD SUCCESS` и файл `target/hello-service-1.0-SNAPSHOT.jar`.

### Локальный запуск

```bash
# Запустить сервер
java -jar target/hello-service-1.0-SNAPSHOT.jar
```

Ты должен увидеть в консоли сообщение о запуске (то, которое ты написал в `App.java`). Сервер будет слушать порт 8081.

Для остановки нажми `Ctrl+C`.

### Сборка Docker-образа

```bash
# Собрать образ с тегом hello-service
docker build -t hello-service .
```

Точка `.` в конце --- это контекст сборки (текущая директория). Docker отправит все файлы из неё в daemon для сборки.

### Запуск в Docker

```bash
# Запустить контейнер
docker run -d -p 8081:8081 --name hello-service hello-service
```

Разбор флагов:

| Флаг | Значение |
|------|----------|
| `-d` | Запуск в фоновом режиме (detached) |
| `-p 8081:8081` | Проброс порта: порт хоста : порт контейнера |
| `--name hello-service` | Имя контейнера (чтобы можно было легко остановить) |

### Управление контейнером

```bash
# Посмотреть логи
docker logs hello-service

# Остановить контейнер
docker stop hello-service

# Удалить контейнер
docker rm hello-service

# Или одной командой --- остановить и удалить
docker rm -f hello-service
```

---

## Шаг 9: Тестирование (curl)

После запуска сервера открой новый терминал и протестируй все эндпоинты.

### Тест 1: HelloHandler

**Без параметров:**

```bash
curl http://localhost:8081/hello
```

Ожидаемый ответ:

```json
{
  "message": "Hello, World!",
  "service": "hello-service"
}
```

**С параметром имени:**

```bash
curl http://localhost:8081/hello?name=Java
```

Ожидаемый ответ:

```json
{
  "message": "Hello, Java!",
  "service": "hello-service"
}
```

### Тест 2: TimeHandler

```bash
curl http://localhost:8081/time
```

Ожидаемый ответ (значения будут другими):

```json
{
  "currentTime": "2025-01-15T14:30:45.123456",
  "date": "2025-01-15",
  "time": "14:30:45"
}
```

### Тест 3: HealthHandler

```bash
curl http://localhost:8081/health
```

Ожидаемый ответ (uptime будет другим):

```json
{
  "status": "UP",
  "service": "hello-service",
  "uptimeSeconds": 42
}
```

### Тест 4: Проверка неправильного метода

```bash
curl -X POST http://localhost:8081/hello
```

Если ты реализовал проверку метода, ожидается ответ с кодом 405:

```json
{
  "error": "Method not allowed"
}
```

### Тест 5: Проверка Content-Type

```bash
curl -v http://localhost:8081/hello
```

Флаг `-v` (verbose) покажет заголовки ответа. Убедись, что среди них есть:

```
Content-Type: application/json
```

---

## Шаг 10: Бонусные задания

Если ты справился с основными задачами --- попробуй эти дополнительные.

### Бонус 1: Echo-эндпоинт (POST /echo)

Создай обработчик `EchoHandler`, который принимает POST-запрос с JSON-телом и возвращает его обратно, обёрнутое в ответ.

**Запрос:**

```bash
curl -X POST http://localhost:8081/echo \
  -H "Content-Type: application/json" \
  -d '{"text": "Hello!"}'
```

**Ответ:**

```json
{
  "echo": {
    "text": "Hello!"
  },
  "receivedAt": "2025-01-15T14:30:45"
}
```

**Подсказки:**
- Чтение тела запроса: `exchange.getRequestBody()` возвращает `InputStream`
- Для чтения `InputStream` в строку используй `new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)`
- Для парсинга JSON-строки в объект используй `gson.fromJson(body, Map.class)`
- Не забудь проверить, что метод --- именно POST

### Бонус 2: Счётчик запросов

Добавь в каждый обработчик подсчёт количества обработанных запросов. Создай эндпоинт `GET /stats`, который показывает статистику.

**Ответ:**

```json
{
  "totalRequests": 15,
  "endpoints": {
    "/hello": 8,
    "/time": 4,
    "/health": 3
  }
}
```

**Подсказки:**
- Используй `AtomicLong` или `AtomicInteger` для потокобезопасного счётчика (запросы обрабатываются в разных потоках!)
- Можно создать отдельный класс `RequestCounter` с `ConcurrentHashMap<String, AtomicLong>` и передавать его во все обработчики через конструктор

### Бонус 3: Middleware для логирования

Создай обёртку, которая логирует каждый запрос: метод, путь, код ответа и время выполнения.

**Пример вывода в консоль:**

```
[2025-01-15T14:30:45] GET /hello?name=Java -> 200 (3ms)
[2025-01-15T14:30:46] POST /hello -> 405 (1ms)
```

**Подсказки:**
- Создай класс `LoggingHandler`, который реализует `HttpHandler` и принимает другой `HttpHandler` в конструкторе (паттерн "декоратор")
- Замеряй время через `System.currentTimeMillis()` до и после вызова внутреннего обработчика
- Регистрируй маршруты через обёртку: `server.createContext("/hello", new LoggingHandler(new HelloHandler()))`

### Бонус 4: CORS-заголовки

Добавь поддержку CORS (Cross-Origin Resource Sharing), чтобы твой API можно было вызывать из браузерного JavaScript с другого домена.

**Нужные заголовки:**

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, OPTIONS
Access-Control-Allow-Headers: Content-Type
```

**Подсказки:**
- Добавь эти заголовки во все ответы
- Обработай preflight-запросы: если метод `OPTIONS`, верни пустой ответ с кодом 204 и CORS-заголовками
- Лучше всего реализовать это через обёртку-декоратор (как в Бонусе 3), чтобы не дублировать код в каждом обработчике

---

**Удачи! Когда все тесты из Шага 9 проходят --- ты готов двигаться к следующему сервису.**
