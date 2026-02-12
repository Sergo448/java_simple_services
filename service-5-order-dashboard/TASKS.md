# Service 5: Order Dashboard --- межсервисное взаимодействие через REST

---

## Шаг 1: Цель и что ты узнаешь

В этом задании ты построишь **сервис-потребитель** (consumer), который общается с другим микросервисом по REST API. Это ключевая концепция микросервисной архитектуры --- сервисы не существуют в изоляции, они обмениваются данными.

### Как это работает

У тебя уже есть **Service 4 (Order Generator)** --- он генерирует заказы и предоставляет REST API для их получения. Теперь ты создашь **Service 5 (Order Dashboard)** --- он будет:

1. **Опрашивать** (polling) Service 4 каждые 15 секунд через HTTP
2. **Сохранять** полученные заказы в локальную память (ConcurrentHashMap)
3. **Агрегировать** данные и отдавать статистику через свой собственный REST API

```
┌──────────────────┐    HTTP GET    ┌──────────────────────┐
│  Service 4       │ <───────────── │  Service 5           │
│  Order Generator │ ────────────>  │  Order Dashboard     │
│  :8084           │   JSON ответ   │  :8085               │
│                  │                │                      │
│  /api/orders     │                │  /api/dashboard/     │
│  /api/orders/new │                │    orders            │
└──────────────────┘                │    stats             │
                                    └──────────────────────┘
                                           ↑
                                    Клиент (curl/браузер)
```

### Что ты узнаешь

- **Межсервисное взаимодействие** (inter-service communication) --- как один микросервис вызывает API другого
- **RestTemplate** --- HTTP-клиент Spring для выполнения REST-запросов
- **Паттерн polling** --- периодический опрос внешнего сервиса по расписанию
- **@Scheduled** --- планировщик задач в Spring Boot
- **@Bean** --- регистрация управляемых компонентов в контексте Spring
- **@Value** --- чтение пользовательских свойств из конфигурации
- **ConcurrentHashMap** --- потокобезопасная коллекция для хранения данных в памяти
- **Stream API** --- агрегация данных и вычисление статистики
- **DTO vs Entity** --- разница между объектом для передачи данных и JPA-сущностью

### Ключевая идея: сервис-потребитель

Service 5 --- это **потребитель** (consumer) API, предоставленного Service 4. Он не имеет собственной базы данных. Он не генерирует заказы сам. Он **забирает данные** у другого сервиса и **добавляет ценность** --- агрегирует, считает статистику, предоставляет удобный дашборд.

Это очень распространённый паттерн в реальных системах: один сервис отвечает за данные, другой --- за их представление и анализ.

### Структура файлов

```
service-5-order-dashboard/
├── pom.xml                                          # Конфигурация Maven
├── Dockerfile                                       # Сборка Docker-образа
├── src/main/resources/application.yml               # Конфигурация приложения
└── src/main/java/com/tutorial/dashboard/
    ├── OrderDashboardApplication.java               # Точка входа + конфигурация бинов
    ├── model/Order.java                             # DTO-модель (не JPA!)
    ├── client/OrderClient.java                      # HTTP-клиент для Service 4
    ├── service/DashboardService.java                # Бизнес-логика + polling + статистика
    └── controller/DashboardController.java          # REST API дашборда
```

---

## Шаг 2: Настройка pom.xml

### Чем этот pom.xml отличается от Service 4?

Service 4 использовал **три** зависимости: `spring-boot-starter-web`, `spring-boot-starter-data-jpa` и `h2`. Ему нужна была база данных для хранения заказов.

Service 5 использует **только одну** зависимость: `spring-boot-starter-web`. Почему?

- **Нет JPA** --- мы не используем базу данных, заказы хранятся в памяти (`ConcurrentHashMap`)
- **Нет H2** --- нет базы данных, нет драйвера
- `spring-boot-starter-web` даёт нам и REST-контроллеры (для нашего API), и `RestTemplate` (для вызова чужого API)

### Структура pom.xml

Это Spring Boot проект, поэтому используем `spring-boot-starter-parent`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- TODO: parent с spring-boot-starter-parent версии 3.2.1 -->

    <!-- TODO: координаты проекта -->
    <!-- groupId: com.tutorial -->
    <!-- artifactId: order-dashboard -->
    <!-- version: 1.0.0 -->

    <!-- TODO: properties с java.version 17 -->

    <!-- TODO: одна зависимость: spring-boot-starter-web -->

    <!-- TODO: плагин spring-boot-maven-plugin -->
</project>
```

### Что даёт spring-boot-starter-parent?

`<parent>` --- это механизм наследования в Maven. Когда ты указываешь `spring-boot-starter-parent` как родительский проект, ты автоматически получаешь:

- Управление версиями всех Spring-зависимостей (не нужно указывать `<version>` для spring-boot-starter-web)
- Настройки компилятора
- Конфигурацию плагинов

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.1</version>
</parent>
```

### Задание

Создай файл `pom.xml` с:

1. `spring-boot-starter-parent` версии `3.2.1` как parent
2. Координатами: `com.tutorial` / `order-dashboard` / `1.0.0`
3. Свойством `java.version` = `17`
4. **Единственной** зависимостью: `spring-boot-starter-web` (без указания версии --- она наследуется от parent)
5. Плагином `spring-boot-maven-plugin` в секции `<build><plugins>`

---

## Шаг 3: application.yml

### Что такое application.yml?

Это конфигурационный файл Spring Boot. Он читается автоматически при запуске приложения. В нём ты задаёшь настройки: порт сервера, подключения к базам данных, пользовательские свойства и т.д.

### Какие настройки нужны

#### 1. Порт сервера

```yaml
server:
  port: 8085
```

Service 5 работает на порту 8085. Не 8084 --- тот занят Service 4!

#### 2. Пользовательское свойство: URL сервиса 4

```yaml
order-generator:
  url: http://localhost:8084
```

Это **пользовательское свойство** (custom property). Spring Boot не знает о нём заранее --- это ты сам его придумал. Но Spring позволяет читать любые свойства из конфигурации с помощью аннотации `@Value`.

#### Как читать пользовательские свойства в коде

В Java-классе ты можешь внедрить значение из конфигурации так:

```java
@Value("${order-generator.url}")
private String baseUrl;
```

Аннотация `@Value` говорит Spring: "Найди в конфигурации свойство `order-generator.url` и подставь его значение в это поле". При запуске Spring прочитает `application.yml` и подставит `http://localhost:8084`.

#### Зачем выносить URL в конфигурацию?

Представь, что ты захардкодил URL прямо в Java-коде:

```java
// ПЛОХО --- захардкоженный URL
String url = "http://localhost:8084/api/orders";
```

Проблема: когда ты запустишь сервис в Docker, Service 4 будет доступен не по `localhost`, а по имени Docker-контейнера `order-generator`. Тебе пришлось бы менять Java-код и перекомпилировать!

С конфигурацией ты просто меняешь `application.yml` (или передаёшь переменную окружения):

```yaml
# Для локального запуска:
order-generator:
  url: http://localhost:8084

# В Docker это будет:
# order-generator:
#   url: http://order-generator:8084
```

Docker автоматически разрешает имена сервисов в IP-адреса внутри своей сети. Контейнер `order-dashboard` может обратиться к контейнеру `order-generator` по его имени --- точно так же, как DNS разрешает доменные имена в интернете.

### Задание

Создай файл `src/main/resources/application.yml` с двумя настройками:

1. Порт сервера: 8085
2. URL генератора заказов: `http://localhost:8084`

---

## Шаг 4: OrderDashboardApplication.java --- точка входа

### Базовая структура

Это стандартный главный класс Spring Boot приложения. Но в отличие от предыдущих сервисов, здесь тебе нужны **две дополнительные вещи**: включение планировщика и регистрация бина RestTemplate.

### @EnableScheduling

Аннотация `@EnableScheduling` включает поддержку планируемых задач в Spring. Без неё аннотация `@Scheduled` на методах не будет работать.

```java
@SpringBootApplication
@EnableScheduling   // Включает поддержку @Scheduled
public class OrderDashboardApplication {
    // ...
}
```

Не забудь импорт: `org.springframework.scheduling.annotation.EnableScheduling`.

### @Bean и RestTemplate

Здесь мы подходим к важной концепции Spring --- **бины** (beans).

#### Что такое бин?

Бин --- это объект, которым **управляет Spring**. Spring создаёт его, хранит в своём контейнере и **внедряет** (inject) туда, где он нужен. Ты уже использовал бины, когда писал `@Service`, `@Component`, `@RestController` --- все эти аннотации говорят Spring: "Создай экземпляр этого класса и управляй им".

#### Зачем нужен @Bean?

Иногда тебе нужно зарегистрировать бин для класса, который ты **не писал сам** --- например, `RestTemplate` из библиотеки Spring. Ты не можешь добавить `@Component` на чужой класс. Вместо этого ты создаёшь **метод-фабрику** с аннотацией `@Bean`:

```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

Что происходит:
1. Spring видит метод с `@Bean` при запуске приложения
2. Вызывает этот метод **один раз**
3. Сохраняет возвращённый объект в контейнере
4. Когда другой класс просит `RestTemplate` (через конструктор или `@Autowired`), Spring отдаёт этот сохранённый экземпляр

Без `@Bean` Spring не будет знать, как создать `RestTemplate`, и ты получишь ошибку при запуске:

```
No qualifying bean of type 'org.springframework.web.client.RestTemplate'
```

### Что такое RestTemplate?

`RestTemplate` --- это HTTP-клиент Spring для выполнения REST-запросов. Он умеет:

- Отправлять GET, POST, PUT, DELETE запросы
- Автоматически сериализовать/десериализовать JSON в Java-объекты
- Обрабатывать HTTP-коды ответов

По сути, это "curl для Java" --- инструмент для вызова чужих REST API из твоего кода.

### Задание

Создай файл `OrderDashboardApplication.java`:

1. Пакет `com.tutorial.dashboard`
2. Аннотации `@SpringBootApplication` и `@EnableScheduling`
3. Метод `main` с `SpringApplication.run(...)`
4. Метод `restTemplate()` с аннотацией `@Bean`, возвращающий `new RestTemplate()`

Необходимые импорты:
- `org.springframework.boot.SpringApplication`
- `org.springframework.boot.autoconfigure.SpringBootApplication`
- `org.springframework.scheduling.annotation.EnableScheduling`
- `org.springframework.context.annotation.Bean`
- `org.springframework.web.client.RestTemplate`

---

## Шаг 5: Order.java --- DTO-модель

### DTO vs JPA Entity --- в чём разница?

В Service 4 класс `Order` был **JPA-сущностью** (Entity):

```java
// Service 4 --- JPA Entity
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ...
}
```

JPA Entity --- это объект, привязанный к таблице в базе данных. Аннотации `@Entity`, `@Table`, `@Id`, `@GeneratedValue` --- всё это инструкции для JPA/Hibernate, как маппить объект на строку в таблице.

В Service 5 класс `Order` --- это **DTO** (Data Transfer Object):

```java
// Service 5 --- DTO (простой POJO)
public class Order {
    private Long id;
    private String product;
    // ...
}
```

DTO --- это простой Java-объект (POJO --- Plain Old Java Object) без каких-либо аннотаций JPA. Он существует только для **передачи данных**. Когда Service 5 получает JSON от Service 4, Jackson (библиотека сериализации) преобразует JSON в объект `Order`. Никакой базы данных нет --- объект просто живёт в памяти.

### Почему поля должны совпадать с JSON?

Когда Service 4 возвращает JSON-ответ, он выглядит примерно так:

```json
{
  "id": 1,
  "product": "Laptop",
  "quantity": 2,
  "price": 1299.99,
  "status": "NEW",
  "createdAt": "2025-01-15T10:30:00"
}
```

Jackson (который встроен в Spring Boot) автоматически сопоставляет JSON-поля с полями Java-класса **по имени**. Если JSON содержит `"product"`, Jackson ищет поле `product` (или метод `setProduct()`) в твоём классе.

### Почему createdAt --- String, а не LocalDateTime?

В Service 4 поле `createdAt` хранится как `LocalDateTime`. Но при передаче по HTTP оно сериализуется в строку ISO-формата. В Service 5 мы принимаем его как `String`, чтобы избежать проблем с десериализацией дат. Это упрощение для учебного проекта.

### Задание

Создай файл `model/Order.java`:

1. Пакет `com.tutorial.dashboard.model`
2. Поля: `id` (Long), `product` (String), `quantity` (Integer), `price` (Double), `status` (String), `createdAt` (String)
3. **Пустой конструктор** (обязательно! Jackson нуждается в нём для десериализации)
4. Конструктор со всеми полями (опционально, для удобства)
5. Геттеры и сеттеры для всех полей

Никаких аннотаций JPA! Это чистый POJO.

---

## Шаг 6: OrderClient.java --- HTTP-клиент

### Это ключевой класс всего проекта

`OrderClient` --- это класс, который **делает HTTP-запросы к Service 4**. Именно здесь происходит межсервисное взаимодействие. Без этого класса Service 5 не сможет получить ни одного заказа.

### Как устроен RestTemplate

`RestTemplate` предоставляет набор методов для HTTP-запросов. Вот самые важные:

| Метод | Что делает | Возвращает |
|-------|-----------|-----------|
| `getForObject(url, Class)` | GET-запрос, возвращает тело | Десериализованный объект |
| `getForEntity(url, Class)` | GET-запрос, возвращает тело + заголовки + статус | `ResponseEntity<T>` |
| `exchange(url, method, body, Class)` | Любой HTTP-метод | `ResponseEntity<T>` |
| `postForObject(url, body, Class)` | POST-запрос | Десериализованный объект |

Для нашего случая (GET-запросы к Service 4) проще всего использовать `getForEntity()` или `getForObject()`.

### Внедрение зависимостей

Класс `OrderClient` нуждается в двух вещах:

1. **RestTemplate** --- для HTTP-запросов (бин, который мы зарегистрировали в Шаге 4)
2. **URL сервиса 4** --- из конфигурации `application.yml`

```java
@Component
public class OrderClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrderClient(RestTemplate restTemplate,
                       @Value("${order-generator.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    // методы...
}
```

Обрати внимание: `RestTemplate` внедряется через конструктор --- Spring находит бин, который мы создали с `@Bean`, и передаёт его сюда. А `@Value` читает значение из `application.yml`.

### Метод fetchAllOrders() --- получить все заказы

Этот метод вызывает `GET {baseUrl}/api/orders` и возвращает список заказов.

Ключевая подсказка --- как десериализовать JSON-массив в Java-массив:

```java
ResponseEntity<Order[]> response = restTemplate.getForEntity(
    baseUrl + "/api/orders", Order[].class);
```

`Order[].class` говорит RestTemplate: "Ожидай JSON-массив и преобразуй каждый элемент в объект Order". Метод `getForEntity` возвращает `ResponseEntity`, который содержит:

- `response.getBody()` --- тело ответа (массив `Order[]`)
- `response.getStatusCode()` --- HTTP-статус (200, 404 и т.д.)

Чтобы вернуть `List<Order>`, используй:

```java
Order[] orders = response.getBody();
return orders != null ? Arrays.asList(orders) : Collections.emptyList();
```

### Метод fetchNewOrders(String since) --- получить новые заказы

Этот метод вызывает `GET {baseUrl}/api/orders/new?since={since}`, где `since` --- это timestamp (ISO-формат), с которого нужны новые заказы.

Подсказка --- как добавить query-параметр:

```java
String url = baseUrl + "/api/orders/new?since=" + since;
ResponseEntity<Order[]> response = restTemplate.getForEntity(url, Order[].class);
```

Если `since` равен `null` (первый запуск, когда ещё нет ни одного заказа), используй `fetchAllOrders()` вместо этого. Это логическая проверка, которую ты реализуешь сам.

### Обработка ошибок --- ОБЯЗАТЕЛЬНО!

Что произойдёт, если Service 4 не запущен? `RestTemplate` бросит исключение `RestClientException` (или его подкласс), и без обработки твой сервис упадёт.

В микросервисной архитектуре **другой сервис может быть недоступен** --- это нормальная ситуация. Твой код должен это учитывать:

```java
try {
    ResponseEntity<Order[]> response = restTemplate.getForEntity(
        baseUrl + "/api/orders", Order[].class);
    // обработка ответа...
} catch (RestClientException e) {
    // Логируем ошибку, но НЕ падаем
    System.err.println("Ошибка при вызове Order Generator: " + e.getMessage());
    return Collections.emptyList();
}
```

Важно: при ошибке возвращай пустой список, а не `null`. Это защитит вызывающий код от `NullPointerException`.

### Логирование (рекомендация)

Вместо `System.err.println` лучше использовать SLF4J-логгер:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(OrderClient.class);

// ...
log.error("Ошибка при вызове Order Generator: {}", e.getMessage());
log.info("Получено {} заказов от Order Generator", orders.length);
```

### Задание

Создай файл `client/OrderClient.java`:

1. Пакет `com.tutorial.dashboard.client`
2. Аннотация `@Component`
3. Внедри `RestTemplate` и `@Value("${order-generator.url}")` через конструктор
4. Метод `fetchAllOrders()` --- GET-запрос к `/api/orders`, возвращает `List<Order>`
5. Метод `fetchNewOrders(String since)` --- GET-запрос к `/api/orders/new?since={since}`, возвращает `List<Order>`
6. Оба метода обёрнуты в try-catch для `RestClientException`
7. При ошибке --- логирование + возврат пустого списка

Необходимые импорты:
- `org.springframework.stereotype.Component`
- `org.springframework.beans.factory.annotation.Value`
- `org.springframework.web.client.RestTemplate`
- `org.springframework.web.client.RestClientException`
- `org.springframework.http.ResponseEntity`
- `java.util.Arrays`
- `java.util.Collections`
- `java.util.List`

---

## Шаг 7: DashboardService.java --- бизнес-логика и polling

### Обзор

Этот класс --- "мозг" дашборда. Он делает три вещи:

1. **Polling** --- каждые 15 секунд опрашивает Service 4 через `OrderClient`
2. **Хранение** --- сохраняет полученные заказы в `ConcurrentHashMap`
3. **Статистика** --- агрегирует данные и вычисляет метрики

### ConcurrentHashMap --- потокобезопасное хранилище

Почему `ConcurrentHashMap`, а не обычный `HashMap`?

Метод `pollNewOrders()` вызывается по расписанию (из потока планировщика), а методы `getOrders()` и `getStats()` вызываются из потоков HTTP-запросов. Это **разные потоки**, которые одновременно читают и пишут в одну коллекцию. Обычный `HashMap` не потокобезопасен --- при одновременном доступе из разных потоков он может сломаться.

`ConcurrentHashMap` решает эту проблему --- он позволяет безопасный одновременный доступ из нескольких потоков.

```java
private final ConcurrentHashMap<Long, Order> orderStorage = new ConcurrentHashMap<>();
```

Ключ --- `Long` (ID заказа), значение --- `Order`. Почему именно Map, а не List? Потому что Map **автоматически предотвращает дубликаты**: если мы получим заказ с ID=5 повторно, он просто перезапишет предыдущий, а не создаст дубль.

### Отслеживание времени последнего опроса

Для инкрементального polling тебе нужно хранить время последнего опроса:

```java
private String lastPollTime;
```

При первом запуске `lastPollTime` равен `null` --- значит, нужно забрать **все** заказы. При последующих опросах мы запрашиваем только новые заказы, созданные **после** `lastPollTime`.

### Метод pollNewOrders() --- @Scheduled

Аннотация `@Scheduled(fixedRate = 15000)` говорит Spring: "Вызывай этот метод каждые 15000 миллисекунд (15 секунд)".

```java
@Scheduled(fixedRate = 15000)
public void pollNewOrders() {
    // Логика опроса
}
```

Алгоритм:

1. Если `lastPollTime == null` --- вызвать `orderClient.fetchAllOrders()`
2. Иначе --- вызвать `orderClient.fetchNewOrders(lastPollTime)`
3. Для каждого полученного заказа --- добавить в `orderStorage` (ключ = `order.getId()`)
4. Обновить `lastPollTime` на текущее время в ISO-формате
5. Залогировать количество новых заказов

Для получения текущего времени в ISO-формате:

```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
// Результат: "2025-01-15T14:30:45.123"
```

### Метод getOrders() --- все заказы

Просто возвращает все значения из `orderStorage` как список:

```java
public List<Order> getOrders() {
    return new ArrayList<>(orderStorage.values());
}
```

### Метод getStats() --- статистика (Stream API)

Это самая интересная часть. Тебе нужно вычислить:

| Метрика | Описание | Как считать |
|---------|---------|------------|
| `totalOrders` | Общее число заказов | `orderStorage.size()` |
| `totalRevenue` | Суммарная выручка | Сумма `price * quantity` по всем заказам |
| `averageOrderValue` | Средняя стоимость заказа | `totalRevenue / totalOrders` |
| `ordersByStatus` | Заказы по статусам | Группировка и подсчёт |

Метод возвращает `Map<String, Object>` --- универсальный формат для JSON:

```java
public Map<String, Object> getStats() {
    List<Order> orders = getOrders();
    Map<String, Object> stats = new HashMap<>();

    stats.put("totalOrders", orders.size());
    // TODO: остальные метрики...

    return stats;
}
```

#### Подсказка: totalRevenue через Stream

```java
double totalRevenue = orders.stream()
    .mapToDouble(o -> o.getPrice() * o.getQuantity())
    .sum();
```

Что здесь происходит:
- `orders.stream()` --- создаём поток (stream) из списка заказов
- `.mapToDouble(o -> ...)` --- преобразуем каждый заказ в число (стоимость = цена * количество)
- `.sum()` --- суммируем все числа

#### Подсказка: averageOrderValue

```java
double averageOrderValue = orders.isEmpty() ? 0 :
    orders.stream()
        .mapToDouble(o -> o.getPrice() * o.getQuantity())
        .average()
        .orElse(0.0);
```

Важно: проверяй `orders.isEmpty()` перед вычислением среднего --- деление на ноль вызовет ошибку!

#### Подсказка: ordersByStatus через Collectors.groupingBy

```java
Map<String, Long> ordersByStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
```

Разбор:
- `Collectors.groupingBy(Order::getStatus, ...)` --- группирует заказы по значению `status`
- `Collectors.counting()` --- подсчитывает количество элементов в каждой группе
- Результат: `{"NEW": 3, "PROCESSING": 2, "COMPLETED": 1}`

Не забудь импорт: `java.util.stream.Collectors`.

### Задание

Создай файл `service/DashboardService.java`:

1. Пакет `com.tutorial.dashboard.service`
2. Аннотация `@Service`
3. Внедри `OrderClient` через конструктор
4. Поле `ConcurrentHashMap<Long, Order> orderStorage`
5. Поле `String lastPollTime` (начальное значение `null`)
6. Метод `pollNewOrders()` с `@Scheduled(fixedRate = 15000)`
7. Метод `getOrders()` --- возвращает `List<Order>`
8. Метод `getStats()` --- возвращает `Map<String, Object>` с четырьмя метриками

Необходимые импорты:
- `org.springframework.stereotype.Service`
- `org.springframework.scheduling.annotation.Scheduled`
- `java.util.concurrent.ConcurrentHashMap`
- `java.util.List`
- `java.util.ArrayList`
- `java.util.Map`
- `java.util.HashMap`
- `java.util.stream.Collectors`
- `java.time.LocalDateTime`
- `java.time.format.DateTimeFormatter`

---

## Шаг 8: DashboardController.java --- REST API дашборда

### Обзор

Контроллер дашборда --- это самый простой класс в проекте. Он просто **делегирует** запросы в `DashboardService` и возвращает результат.

### Два эндпоинта

| Метод | Путь | Описание | Возвращает |
|-------|------|---------|-----------|
| GET | `/api/dashboard/orders` | Все локально сохранённые заказы | `List<Order>` |
| GET | `/api/dashboard/stats` | Статистика по заказам | `Map<String, Object>` |

### Структура

```java
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    // TODO: внедри DashboardService через конструктор

    // TODO: GET /orders --- вернуть все заказы из локального хранилища

    // TODO: GET /stats --- вернуть статистику
}
```

Вспомни, как работают аннотации:
- `@RestController` = `@Controller` + `@ResponseBody` (автоматическая сериализация в JSON)
- `@RequestMapping("/api/dashboard")` --- базовый путь для всех эндпоинтов
- `@GetMapping("/orders")` --- обработка GET-запросов на `/api/dashboard/orders`

Spring автоматически преобразует возвращаемый `List<Order>` или `Map<String, Object>` в JSON-ответ. Тебе не нужно вызывать `gson.toJson()` или что-то подобное --- Jackson (встроенный в Spring) делает это за тебя.

### Задание

Создай файл `controller/DashboardController.java`:

1. Пакет `com.tutorial.dashboard.controller`
2. Аннотации `@RestController` и `@RequestMapping("/api/dashboard")`
3. Внедри `DashboardService` через конструктор
4. Эндпоинт `GET /orders` --- возвращает `dashboardService.getOrders()`
5. Эндпоинт `GET /stats` --- возвращает `dashboardService.getStats()`

---

## Шаг 9: Dockerfile

### Многоступенчатая сборка

Как и в предыдущих сервисах, используем multi-stage build. Два этапа:

**Этап 1 (builder):** Образ с Maven и JDK. Копируем `pom.xml`, скачиваем зависимости, копируем код, собираем JAR.

**Этап 2 (runtime):** Лёгкий образ только с JRE. Копируем готовый JAR из первого этапа и запускаем.

### Структура

```dockerfile
# ======= Этап 1: Сборка =======
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Копируем pom.xml и качаем зависимости (кеширование слоёв Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем код и собираем
COPY src ./src
RUN mvn clean package -DskipTests

# ======= Этап 2: Запуск =======
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8085

CMD ["java", "-jar", "app.jar"]
```

### Задание

Создай `Dockerfile` в корне `service-5-order-dashboard/`. Порт --- 8085.

---

## Шаг 10: Сборка и запуск

### ВАЖНО: Service 4 должен быть запущен первым!

Service 5 опрашивает Service 4 по HTTP. Если Service 4 не запущен, Service 5 стартует, но будет получать ошибки при polling (и логировать их). Заказов в дашборде не будет.

### Порядок запуска

#### Шаг 1: Запусти Service 4 (Order Generator)

Открой **первый** терминал:

```bash
cd service-4-order-generator
mvn clean package -DskipTests
java -jar target/order-generator-1.0.0.jar
```

Убедись, что Service 4 запустился на порту 8084. Подожди 10-15 секунд, чтобы он сгенерировал несколько заказов.

Проверь:

```bash
curl http://localhost:8084/api/orders
```

Ты должен увидеть JSON-массив с заказами.

#### Шаг 2: Запусти Service 5 (Order Dashboard)

Открой **второй** терминал:

```bash
cd service-5-order-dashboard
mvn clean package -DskipTests
java -jar target/order-dashboard-1.0.0.jar
```

Service 5 запустится на порту 8085. В логах ты увидишь сообщения о polling --- каждые 15 секунд он будет запрашивать новые заказы у Service 4.

#### Шаг 3: Подожди 30 секунд

Дай Service 5 время на несколько циклов polling. Первый цикл произойдёт почти сразу после запуска, следующие --- каждые 15 секунд.

### Что если Service 4 не запущен?

Service 5 **не упадёт**. Благодаря try-catch в `OrderClient`, ошибки соединения будут залогированы, а polling продолжит работать. Как только Service 4 станет доступен, следующий цикл polling успешно получит заказы.

Это важное свойство микросервисной архитектуры: **сервисы должны быть устойчивы к недоступности зависимостей**.

---

## Шаг 11: Тестирование (curl)

### Тест 1: Проверь, что Service 4 работает

```bash
curl http://localhost:8084/api/orders
```

Ожидаемый ответ --- JSON-массив заказов:

```json
[
  {
    "id": 1,
    "product": "Laptop",
    "quantity": 2,
    "price": 1299.99,
    "status": "NEW",
    "createdAt": "2025-01-15T10:30:00"
  },
  ...
]
```

Если ответа нет --- Service 4 не запущен. Вернись к Шагу 10.

### Тест 2: Получи заказы из дашборда

```bash
curl http://localhost:8085/api/dashboard/orders
```

Ожидаемый ответ --- те же заказы, что и в Service 4 (Service 5 скопировал их к себе):

```json
[
  {
    "id": 1,
    "product": "Laptop",
    "quantity": 2,
    "price": 1299.99,
    "status": "NEW",
    "createdAt": "2025-01-15T10:30:00"
  },
  ...
]
```

Если массив пустой --- подожди ещё 15 секунд (polling ещё не сработал) или проверь логи на ошибки.

### Тест 3: Проверь статистику

```bash
curl http://localhost:8085/api/dashboard/stats
```

Ожидаемый ответ (значения зависят от сгенерированных заказов):

```json
{
  "totalOrders": 6,
  "totalRevenue": 15420.0,
  "averageOrderValue": 2570.0,
  "ordersByStatus": {
    "NEW": 3,
    "PROCESSING": 2,
    "COMPLETED": 1
  }
}
```

### Тест 4: Проверь инкрементальный polling

1. Запроси статистику, запомни `totalOrders`
2. Подожди 30 секунд (Service 4 сгенерирует новые заказы)
3. Запроси статистику снова --- `totalOrders` должен вырасти

```bash
# Первый запрос
curl http://localhost:8085/api/dashboard/stats
# Ждём 30 секунд...
# Второй запрос
curl http://localhost:8085/api/dashboard/stats
```

Если `totalOrders` растёт --- polling работает корректно!

---

## Шаг 12: Docker --- запуск обоих сервисов

### Зачем docker-compose?

Запускать два сервиса вручную (в двух терминалах) неудобно. `docker-compose` позволяет запустить **оба сервиса одной командой** и автоматически настроить сетевое взаимодействие между ними.

### Ключевая концепция: Docker-сеть

Когда ты запускаешь сервисы через docker-compose, Docker создаёт **виртуальную сеть**. Внутри этой сети контейнеры обращаются друг к другу **по имени сервиса**, а не по `localhost`.

```
┌─────────────────────────────────────────────────┐
│  Docker Network                                 │
│                                                 │
│  ┌─────────────────┐    ┌─────────────────────┐ │
│  │ order-generator  │    │ order-dashboard      │ │
│  │ :8084           │◄───│ :8085               │ │
│  └─────────────────┘    └─────────────────────┘ │
│                                                 │
│  URL внутри сети: http://order-generator:8084   │
└─────────────────────────────────────────────────┘
          ↑                         ↑
     localhost:8084            localhost:8085
     (для хоста)               (для хоста)
```

Поэтому в `application.yml` для Docker-среды URL должен быть `http://order-generator:8084` (имя сервиса вместо `localhost`).

### Переопределение конфигурации через переменные окружения

Spring Boot позволяет **переопределять** свойства из `application.yml` через переменные окружения. Формат: точки и дефисы заменяются на подчёркивания, всё в верхнем регистре.

Свойство `order-generator.url` можно переопределить переменной `ORDER_GENERATOR_URL`:

```yaml
# docker-compose.yml (фрагмент)
services:
  order-dashboard:
    environment:
      - ORDER_GENERATOR_URL=http://order-generator:8084
```

Это позволяет **не менять** `application.yml` --- для локального запуска используется `localhost`, для Docker --- переменная окружения.

### Структура docker-compose.yml

Тебе нужно описать два сервиса. Вот каркас:

```yaml
version: '3.8'
services:

  order-generator:
    # TODO: build из директории service-4-order-generator
    # TODO: ports: 8084:8084
    # TODO: restart: unless-stopped

  order-dashboard:
    # TODO: build из директории service-5-order-dashboard
    # TODO: ports: 8085:8085
    # TODO: depends_on: order-generator
    # TODO: environment: переопределить URL на http://order-generator:8084
    # TODO: restart: unless-stopped
```

### depends_on

`depends_on` гарантирует **порядок запуска**: Docker сначала запустит `order-generator`, потом `order-dashboard`. Однако `depends_on` **не ждёт**, пока Service 4 полностью инициализируется --- он просто запускает контейнер. Но это не проблема: наш `OrderClient` обрабатывает ошибки соединения (try-catch), и после нескольких неудачных попыток polling подключится, когда Service 4 будет готов.

### Запуск

```bash
# Из корня проекта (где лежит docker-compose.yml)
docker-compose up --build
```

Флаг `--build` пересобирает образы. Без него Docker использует кеш.

### Проверка

```bash
# Service 4 доступен
curl http://localhost:8084/api/orders

# Service 5 доступен
curl http://localhost:8085/api/dashboard/stats
```

### Остановка

```bash
docker-compose down
```

---

## Шаг 13: Бонусные задания

Если основная часть готова и работает, попробуй эти усложнения.

### Бонус 1: WebClient вместо RestTemplate

`RestTemplate` --- это классический (синхронный) HTTP-клиент Spring. Он работает, но считается **устаревшим** в пользу `WebClient` --- реактивного HTTP-клиента из Spring WebFlux.

Попробуй:
- Добавь зависимость `spring-boot-starter-webflux` в `pom.xml`
- Замени `RestTemplate` на `WebClient` в `OrderClient`
- `WebClient` использует реактивные типы (`Mono`, `Flux`), но их можно превратить в обычные значения через `.block()`

Подсказка:

```java
WebClient webClient = WebClient.builder()
    .baseUrl(baseUrl)
    .build();

List<Order> orders = webClient.get()
    .uri("/api/orders")
    .retrieve()
    .bodyToFlux(Order.class)
    .collectList()
    .block();
```

### Бонус 2: Circuit Breaker (предохранитель)

Что если Service 4 упал надолго? Каждые 15 секунд твой polling пытается подключиться, получает ошибку, логирует её. Это создаёт лишнюю нагрузку на сеть.

Паттерн **Circuit Breaker** (автоматический выключатель) работает так:
- Если N последних запросов завершились ошибкой --- "размыкаем цепь" (прекращаем запросы)
- Через определённый таймаут --- пробуем один запрос
- Если он успешен --- "замыкаем цепь" (возобновляем нормальную работу)

Реализуй простой circuit breaker:
- Считай количество последовательных ошибок
- Если больше 3 ошибок подряд --- пропускай polling на 60 секунд
- После паузы --- пробуй снова

### Бонус 3: WebSocket для реального времени

Сейчас дашборд обновляется только по запросу клиента (pull-модель). С WebSocket можно сделать push-модель --- сервер сам отправляет обновления клиенту.

Попробуй:
- Добавь зависимость `spring-boot-starter-websocket`
- Настрой `WebSocketConfigurer`
- При получении новых заказов через polling --- отправляй уведомление подписанным клиентам

### Бонус 4: Эндпоинт для ручного polling

Добавь эндпоинт `POST /api/dashboard/poll`, который запускает polling немедленно (без ожидания 15 секунд). Это полезно для тестирования.

```
POST /api/dashboard/poll → вызвать dashboardService.pollNewOrders() вручную
```

Возвращай количество новых заказов или полную статистику.

---

**Когда тесты из Шага 11 проходят и дашборд показывает растущую статистику --- ты успешно реализовал межсервисное взаимодействие! Переходи к следующему сервису.**
