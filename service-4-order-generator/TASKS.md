# Service 4: Order Generator (Producer-сервис)

Генератор заказов --- сервис, который автоматически создает заказы по расписанию
и отдает их через REST API. Это **producer** (поставщик данных): он производит данные,
а Service 5 (Order Dashboard) будет их потреблять.

---

## Структура файлов

```
service-4-order-generator/
├── pom.xml
├── Dockerfile
├── src/main/resources/application.yml
└── src/main/java/com/tutorial/ordergen/
    ├── OrderGeneratorApplication.java
    ├── model/Order.java
    ├── repository/OrderRepository.java
    ├── service/OrderService.java
    ├── service/OrderScheduler.java
    └── controller/OrderController.java
```

---

## Шаг 1: Цель и что ты узнаешь

### Что делает этот сервис

Order Generator --- это **producer-сервис** (сервис-производитель). Он выполняет две задачи:

1. **Автогенерация**: каждые 10 секунд создает случайный заказ и сохраняет его в базу.
2. **REST API**: отдает заказы по HTTP --- все сразу, по ID или только новые за определённый период.

Service 5 (Order Dashboard) будет опрашивать этот сервис через эндпоинт
`GET /api/orders/new?since=...`, чтобы забирать свежие заказы. Это паттерн **polling**
(периодический опрос).

### Что нового ты изучишь

- **`@Scheduled`** --- запуск методов по расписанию (таймер внутри Spring).
- **`@EnableScheduling`** --- активация механизма планировщика.
- **JPA с датами** --- работа с `LocalDateTime` в сущностях и запросах.
- **Spring Data Query Methods** --- автоматическая генерация SQL из названия метода.
- **`@RequestParam`** --- получение параметров из URL-строки запроса.
- **Паттерн producer/consumer** --- разделение сервисов на поставщика и потребителя данных.

### Как это связано с другими сервисами

```
Service 4 (Order Generator)          Service 5 (Order Dashboard)
┌───────────────────────┐            ┌──────────────────────┐
│  @Scheduled каждые    │            │  Каждые N секунд     │
│  10 сек: создать      │  polling   │  запрашивает:        │
│  случайный заказ      │ ◄──────────│  GET /api/orders/new │
│                       │            │  ?since=...          │
│  База H2: заказы      │            │  Показывает на       │
│  копятся              │            │  дашборде            │
└───────────────────────┘            └──────────────────────┘
       порт 8084                           порт 8085
```

---

## Шаг 2: Настройка pom.xml

Открой файл `pom.xml`. Сейчас там только TODO-комментарии. Тебе нужно написать
полноценный Maven-проект.

### Что нужно указать

**Parent** (родительский POM):
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.1</version>
</parent>
```

**Координаты проекта:**
- `groupId`: `com.tutorial`
- `artifactId`: `order-generator`
- `version`: `1.0.0`

**Свойства:**
```xml
<properties>
    <java.version>17</java.version>
</properties>
```

**Зависимости** (секция `<dependencies>`):

| Зависимость | groupId | artifactId | Зачем |
|---|---|---|---|
| Web | `org.springframework.boot` | `spring-boot-starter-web` | REST-контроллеры, встроенный Tomcat |
| JPA | `org.springframework.boot` | `spring-boot-starter-data-jpa` | ORM, репозитории, работа с БД |
| H2 | `com.h2database` | `h2` | In-memory база данных (scope: `runtime`) |

> **Подсказка по H2**: для зависимости H2 добавь `<scope>runtime</scope>`. Это значит,
> что H2 нужна только при запуске, но не при компиляции. Код сервиса не обращается
> к H2 напрямую --- всю работу делает JPA.

**Плагин для сборки:**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

### Проверка

Структура `pom.xml` должна выглядеть так:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>

    <parent>...</parent>

    <groupId>...</groupId>
    <artifactId>...</artifactId>
    <version>...</version>

    <properties>...</properties>

    <dependencies>
        <!-- 3 зависимости -->
    </dependencies>

    <build>
        <plugins>...</plugins>
    </build>
</project>
```

Собери проект, чтобы убедиться, что pom.xml корректен:

```bash
cd service-4-order-generator
mvn clean compile
```

---

## Шаг 3: application.yml

Открой файл `src/main/resources/application.yml`. Настрой конфигурацию сервиса.

### Что нужно указать

**1. Порт сервера:**
```yaml
server:
  port: 8084
```

**2. Подключение к базе H2:**

Настрой `spring.datasource` --- точно так же, как в Service 3 (User Service):

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:orderdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
```

> **Разбор URL**: `jdbc:h2:mem:orderdb` означает:
> - `jdbc:h2` --- драйвер H2
> - `mem` --- база в оперативной памяти (пропадёт при остановке)
> - `orderdb` --- имя базы (выбираем своё, отличное от других сервисов)

**3. Консоль H2** (для отладки):
```yaml
  h2:
    console:
      enabled: true
```

Консоль будет доступна по адресу `http://localhost:8084/h2-console`.

**4. Настройки JPA:**
```yaml
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

> **`ddl-auto: update`** --- Hibernate автоматически создаст таблицу `orders` по
> полям сущности `Order`. При каждом запуске он проверяет, совпадает ли таблица с
> классом, и обновляет структуру при необходимости.
>
> **`show-sql: true`** --- каждый SQL-запрос выводится в консоль. Очень полезно
> для обучения: ты увидишь, какие запросы генерирует JPA за кулисами.

### Итоговая структура application.yml

```yaml
server:
  port: ???

spring:
  datasource:
    url: ???
    driver-class-name: ???
    username: ???
    password:
  h2:
    console:
      enabled: ???
  jpa:
    hibernate:
      ddl-auto: ???
    show-sql: ???
```

Заполни значения самостоятельно.

---

## Шаг 4: OrderGeneratorApplication.java --- главный класс

Открой `src/main/java/com/tutorial/ordergen/OrderGeneratorApplication.java`.

### Что нужно сделать

1. Добавь аннотацию `@SpringBootApplication` на класс.
2. Добавь аннотацию `@EnableScheduling` на класс.
3. В методе `main` вызови `SpringApplication.run(...)`.

### Что такое @EnableScheduling

По умолчанию Spring Boot **не** запускает планировщик задач. Аннотация
`@EnableScheduling` активирует его:

- Spring создаёт внутренний `TaskScheduler` (пул потоков для отложенных задач).
- После этого Spring начинает искать методы с аннотацией `@Scheduled` во всех бинах.
- Найденные методы регистрируются в планировщике и вызываются по расписанию.

Без `@EnableScheduling` аннотация `@Scheduled` на методах будет **полностью
игнорироваться** --- метод никогда не вызовется автоматически.

### Подсказка по коду

```java
// Нужные импорты:
// org.springframework.boot.SpringApplication
// org.springframework.boot.autoconfigure.SpringBootApplication
// org.springframework.scheduling.annotation.EnableScheduling

@???
@???
public class OrderGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.???(OrderGeneratorApplication.class, args);
    }
}
```

> **Совет**: `@EnableScheduling` можно поставить на любой `@Configuration`-класс,
> но по соглашению её ставят на главный класс приложения --- это самое очевидное место.

---

## Шаг 5: Order.java --- JPA-сущность

Открой `src/main/java/com/tutorial/ordergen/model/Order.java`. Это главная модель
данных --- JPA-сущность, которая маппится на таблицу в базе.

### Поля сущности

| Поле | Тип | Описание | Аннотации |
|---|---|---|---|
| `id` | `Long` | Уникальный идентификатор | `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `product` | `String` | Название товара | `@Column(nullable = false)` |
| `quantity` | `Integer` | Количество | `@Column(nullable = false)` |
| `price` | `Double` | Цена | `@Column(nullable = false)` |
| `status` | `String` | Статус заказа | `@Column(nullable = false)` |
| `createdAt` | `LocalDateTime` | Время создания | `@Column(nullable = false)` |

### Возможные значения status

- `"NEW"` --- только что создан
- `"PROCESSING"` --- в обработке
- `"COMPLETED"` --- выполнен

### Подсказка по аннотациям класса

```java
@Entity
@Table(name = "orders")
public class Order {
    // ...
}
```

> **Почему `orders`, а не `order`?** Слово `ORDER` --- зарезервированное ключевое
> слово SQL (оператор `ORDER BY`). Если назвать таблицу `order`, H2 выдаст ошибку.
> Поэтому используем `orders` (множественное число).

### Подробнее об аннотациях

**`@Entity`** --- говорит JPA, что этот класс представляет таблицу в базе данных.

**`@Table(name = "orders")`** --- явно указывает имя таблицы. Без этой аннотации
JPA возьмёт имя класса (`Order`), а оно конфликтует с SQL-ключевым словом.

**`@Id`** --- помечает поле как первичный ключ.

**`@GeneratedValue(strategy = GenerationType.IDENTITY)`** --- база сама генерирует
ID (автоинкремент).

**`@Column(nullable = false)`** --- поле обязательное, в базе будет `NOT NULL`.
Можно также указать `@Column(name = "product_name")`, если хочешь другое имя
столбца, но для нас подойдут имена по умолчанию (JPA берёт имя поля).

### Работа с LocalDateTime в JPA

`LocalDateTime` из пакета `java.time` отлично работает с JPA начиная с версии 2.2.
Никаких дополнительных конвертеров не нужно --- Hibernate маппит `LocalDateTime`
на тип `TIMESTAMP` в базе данных автоматически.

```java
import java.time.LocalDateTime;

@Column(nullable = false)
private LocalDateTime createdAt;
```

> **Важно**: не используй старый `java.util.Date` --- `LocalDateTime` проще,
> безопаснее и не содержит информации о часовом поясе (что подходит для нашей задачи).

### Подсказка по структуре класса

```java
import jakarta.persistence.*;
import java.time.LocalDateTime;

@???
@???(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = ???)
    private Long id;

    @Column(nullable = false)
    private String product;

    // ... остальные поля по таблице выше

    // Пустой конструктор (обязателен для JPA!)
    public Order() {}

    // Конструктор с параметрами (без id и createdAt --- они заполнятся автоматически)
    public Order(String product, Integer quantity, Double price, String status) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

    // Геттеры и сеттеры для ВСЕХ полей
    // ...
}
```

> **Напоминание**: JPA требует **пустой конструктор без аргументов**. Hibernate
> использует его при создании объектов из данных базы (через рефлексию). Если ты
> создаёшь конструктор с параметрами, Java перестаёт генерировать конструктор по
> умолчанию --- поэтому пустой конструктор нужно написать явно.

---

## Шаг 6: OrderRepository.java --- репозиторий

Открой `src/main/java/com/tutorial/ordergen/repository/OrderRepository.java`.

### Что нужно сделать

1. Интерфейс `OrderRepository` должен наследовать `JpaRepository<Order, Long>`.
2. Добавь кастомный метод `findByCreatedAtAfter(LocalDateTime since)`.

### Подсказка

```java
import org.springframework.data.jpa.repository.JpaRepository;
import com.tutorial.ordergen.model.Order;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends ???<Order, Long> {

    List<Order> findByCreatedAtAfter(LocalDateTime since);
}
```

### Как работают Spring Data Query Methods

Это **ключевая концепция** этого шага. Spring Data умеет **генерировать SQL-запросы
из имени метода**. Тебе не нужно писать SQL или JPQL --- Spring разберёт имя метода
по правилам и сам создаст запрос.

**Разбор имени `findByCreatedAtAfter`:**

```
find         By        CreatedAt       After
─────        ──        ─────────       ─────
действие   разделитель    поле         условие
(SELECT)   (WHERE)    (created_at)    (> значение)
```

Spring преобразует это в SQL:

```sql
SELECT * FROM orders WHERE created_at > ?
```

Параметр `LocalDateTime since` подставляется вместо `?`.

### Другие примеры Query Methods (для справки)

| Имя метода | SQL-эквивалент |
|---|---|
| `findByStatus(String s)` | `WHERE status = ?` |
| `findByPriceGreaterThan(Double p)` | `WHERE price > ?` |
| `findByProductContaining(String s)` | `WHERE product LIKE '%s%'` |
| `findByStatusAndCreatedAtAfter(...)` | `WHERE status = ? AND created_at > ?` |

> **Почему это важно для Service 5?** Service 5 (Order Dashboard) будет запрашивать
> свежие заказы: "дай все заказы, созданные после такого-то момента". Эндпоинт
> `GET /api/orders/new?since=...` использует именно метод `findByCreatedAtAfter`.
> Так Service 5 не будет получать дубликаты --- только новые заказы.

### Обрати внимание

- `OrderRepository` --- это **интерфейс**, не класс. Реализацию Spring создаёт сам.
- Не нужна аннотация `@Repository` --- Spring Data автоматически регистрирует
  интерфейсы, наследующие `JpaRepository`.

---

## Шаг 7: OrderService.java --- сервисный слой

Открой `src/main/java/com/tutorial/ordergen/service/OrderService.java`.

### Что нужно сделать

1. Аннотируй класс `@Service`.
2. Внедри `OrderRepository` через конструктор.
3. Реализуй четыре метода.

### Методы

| Метод | Что делает | Подробности |
|---|---|---|
| `findAll()` | Возвращает все заказы | `repository.findAll()` |
| `findById(Long id)` | Находит заказ по ID | Бросает исключение, если не найден |
| `create(Order order)` | Создаёт новый заказ | Устанавливает `createdAt = LocalDateTime.now()` |
| `findNewSince(LocalDateTime since)` | Заказы после указанного времени | `repository.findByCreatedAtAfter(since)` |

### Подсказка по коду

```java
import org.springframework.stereotype.Service;
import com.tutorial.ordergen.model.Order;
import com.tutorial.ordergen.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;

@???
public class OrderService {

    private final OrderRepository orderRepository;

    // Конструктор для внедрения зависимости
    public OrderService(???) {
        this.orderRepository = orderRepository;
    }

    public List<Order> findAll() {
        return orderRepository.???();
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public Order create(Order order) {
        order.setCreatedAt(LocalDateTime.???());
        return orderRepository.???(order);
    }

    public List<Order> findNewSince(LocalDateTime since) {
        return orderRepository.???(since);
    }
}
```

### Обрати внимание на create()

Метод `create` устанавливает `createdAt` **в сервисе**, а не ждёт, пока его передаст
клиент. Это правильный подход:

- Клиент не может подделать время создания.
- Время всегда определяется сервером.
- `LocalDateTime.now()` возвращает текущую дату и время.

> **Альтернатива**: можно использовать аннотацию `@PrePersist` в сущности, чтобы
> время устанавливалось автоматически перед сохранением в базу. Но для простоты
> мы делаем это в сервисе явно.

---

## Шаг 8: OrderScheduler.java --- автогенерация заказов

Открой `src/main/java/com/tutorial/ordergen/service/OrderScheduler.java`.

Это **ключевая особенность** данного сервиса --- автоматическая генерация заказов
по расписанию.

### Что нужно сделать

1. Аннотируй класс `@Component`.
2. Внедри `OrderService` через конструктор.
3. Напиши метод `generateOrder()` с аннотацией `@Scheduled(fixedRate = 10000)`.
4. Внутри метода генерируй случайный заказ и сохраняй его через `OrderService`.
5. Логируй каждый созданный заказ в консоль.

### Логика генерации случайного заказа

**Массив товаров:**
```java
String[] products = {"Laptop", "Phone", "Tablet", "Headphones", "Monitor"};
```

**Массив статусов:**
```java
String[] statuses = {"NEW", "PROCESSING", "COMPLETED"};
```

**Случайные значения:**
- Товар: случайный элемент из массива `products`
- Количество: случайное число от 1 до 10
- Цена: случайное число от 100.0 до 2000.0
- Статус: случайный элемент из массива `statuses`

### Подсказка по работе со случайными числами

Используй `java.util.Random` или `java.util.concurrent.ThreadLocalRandom`:

```java
import java.util.concurrent.ThreadLocalRandom;

// Случайное целое число от 1 (включительно) до 11 (не включительно) → от 1 до 10
int quantity = ThreadLocalRandom.current().nextInt(1, 11);

// Случайное дробное число от 100.0 до 2000.0
double price = ThreadLocalRandom.current().nextDouble(100.0, 2000.0);

// Случайный элемент массива
String product = products[ThreadLocalRandom.current().nextInt(products.length)];
```

> **`ThreadLocalRandom` vs `Random`**: `ThreadLocalRandom` потокобезопасен без
> блокировок и рекомендуется для использования в многопоточных приложениях.
> `Random` тоже подойдёт для нашей задачи, но `ThreadLocalRandom` --- более
> современный подход.

### Подсказка по структуре класса

```java
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

@???
public class OrderScheduler {

    private final OrderService orderService;

    public OrderScheduler(???) {
        this.orderService = orderService;
    }

    @Scheduled(fixedRate = ???)
    public void generateOrder() {
        String[] products = { "Laptop", "Phone", "Tablet", "Headphones", "Monitor" };
        String[] statuses = { "NEW", "PROCESSING", "COMPLETED" };

        // TODO: выбери случайный товар из массива
        // TODO: сгенерируй случайное количество (1-10)
        // TODO: сгенерируй случайную цену (100.0-2000.0)
        // TODO: выбери случайный статус

        // TODO: создай объект Order через конструктор с параметрами
        // TODO: сохрани через orderService.create(order)
        // TODO: выведи информацию в консоль через System.out.println

        System.out.println(">>> Generated order: " + product
            + " | qty: " + quantity
            + " | price: " + String.format("%.2f", price)
            + " | status: " + status);
    }
}
```

### Подробнее об аннотации @Scheduled

Аннотация `@Scheduled` определяет расписание запуска метода. Основные параметры:

**`fixedRate`** --- интервал между **началами** запусков (в миллисекундах):
```java
@Scheduled(fixedRate = 10000)  // Каждые 10 секунд
```
Если метод выполняется 3 секунды, следующий запуск будет через 7 секунд после
его завершения (чтобы соблюсти интервал 10 секунд от начала до начала).

**`fixedDelay`** --- интервал между **окончанием** одного запуска и **началом**
следующего:
```java
@Scheduled(fixedDelay = 10000)  // 10 секунд после завершения предыдущего
```
Если метод выполняется 3 секунды, следующий запуск будет через 13 секунд после
начала предыдущего (3 + 10).

**`cron`** --- cron-выражение для сложных расписаний:
```java
@Scheduled(cron = "0 0 * * * *")    // Каждый час, в 0 минут 0 секунд
@Scheduled(cron = "0 */5 * * * *")  // Каждые 5 минут
@Scheduled(cron = "0 0 9 * * MON")  // Каждый понедельник в 9:00
```

> **Для нашей задачи** используем `fixedRate = 10000` (10 секунд). Генерация
> заказа --- мгновенная операция, поэтому разница между `fixedRate` и `fixedDelay`
> здесь несущественна.

### Важно: @EnableScheduling

Помни --- без аннотации `@EnableScheduling` на главном классе приложения
(Шаг 4), метод с `@Scheduled` **никогда не вызовется**. Это частая ошибка.

---

## Шаг 9: OrderController.java --- REST-контроллер

Открой `src/main/java/com/tutorial/ordergen/controller/OrderController.java`.

### Эндпоинты

| HTTP-метод | URL | Описание |
|---|---|---|
| `GET` | `/api/orders` | Все заказы |
| `GET` | `/api/orders/{id}` | Один заказ по ID |
| `POST` | `/api/orders` | Ручное создание заказа |
| `GET` | `/api/orders/new` | Новые заказы после указанного времени |

### Что нужно сделать

1. Аннотируй класс `@RestController` и `@RequestMapping("/api/orders")`.
2. Внедри `OrderService` через конструктор.
3. Реализуй четыре эндпоинта.

### Подсказка по коду

```java
import org.springframework.web.bind.annotation.*;
import com.tutorial.ordergen.model.Order;
import com.tutorial.ordergen.service.OrderService;
import java.time.LocalDateTime;
import java.util.List;

@???
@???(???)
public class OrderController {

    private final OrderService orderService;

    public OrderController(???) {
        this.orderService = orderService;
    }

    // GET /api/orders
    @GetMapping
    public List<Order> getAll() {
        return orderService.???();
    }

    // GET /api/orders/{id}
    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.???(id);
    }

    // POST /api/orders
    @PostMapping
    public Order create(@RequestBody Order order) {
        return orderService.???(order);
    }

    // GET /api/orders/new?since=2024-01-01T00:00:00
    @GetMapping("/new")
    public List<Order> getNewOrders(@RequestParam String since) {
        LocalDateTime sinceDateTime = LocalDateTime.parse(since);
        return orderService.???(sinceDateTime);
    }
}
```

### Подробнее об эндпоинте /new

Это **ключевой эндпоинт** для интеграции с Service 5. Разберём его подробно.

**`@RequestParam String since`** --- извлекает параметр `since` из строки запроса URL:

```
GET /api/orders/new?since=2024-01-01T00:00:00
                    ^^^^^
                    имя параметра
                          ^^^^^^^^^^^^^^^^^^^
                          значение параметра
```

`@RequestParam` по умолчанию делает параметр обязательным. Если клиент не передаст
`since`, Spring вернёт ошибку 400 (Bad Request).

**`LocalDateTime.parse(since)`** --- парсит строку в ISO 8601 формате:

```
2024-01-01T00:00:00
^^^^
год-месяц-день
          ^
          разделитель T
           ^^^^^^^^
           часы:минуты:секунды
```

> **Как Service 5 будет использовать этот эндпоинт:**
> 1. Запоминает время последнего опроса (например, `2024-06-15T10:30:00`).
> 2. Отправляет запрос: `GET /api/orders/new?since=2024-06-15T10:30:00`.
> 3. Получает только заказы, созданные ПОСЛЕ этого момента.
> 4. Обновляет время последнего опроса.
> 5. Повторяет через N секунд.

### Почему маппинг "/new" работает вместе с "/{id}"

Spring различает эти два маппинга:
- `/api/orders/new` --- точное совпадение, приоритет выше
- `/api/orders/{id}` --- шаблон с переменной, приоритет ниже

Запрос `GET /api/orders/new` попадёт в метод `getNewOrders`, а не в `getById`,
потому что точное совпадение всегда побеждает шаблон.

---

## Шаг 10: Dockerfile --- контейнеризация

Открой файл `Dockerfile`. Напиши multi-stage сборку.

### Stage 1: Сборка (Build)

- Базовый образ: `maven:3.9-eclipse-temurin-17`
- Рабочая директория: `/app`
- Скопируй `pom.xml` и загрузи зависимости отдельным шагом (для кэширования)
- Скопируй `src/`
- Собери проект: `mvn clean package -DskipTests`

### Stage 2: Запуск (Run)

- Базовый образ: `eclipse-temurin:17-jre`
- Скопируй JAR из первого этапа
- Открой порт `8084`
- Команда запуска: `java -jar app.jar`

### Подсказка

```dockerfile
# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-17 AS ???
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package ???

# --- Stage 2: Run ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=??? /app/target/*.jar app.jar
EXPOSE ???
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> **Зачем multi-stage?** Первый этап содержит Maven, JDK и все исходники ---
> это ~500 МБ. Второй этап содержит только JRE и JAR --- это ~200 МБ. Экономия
> более чем в два раза.
>
> **Зачем `dependency:go-offline` отдельным шагом?** Docker кэширует каждый слой.
> Если `pom.xml` не изменился, Docker не будет заново скачивать зависимости ---
> только пересоберёт код. Это значительно ускоряет повторные сборки.

---

## Шаг 11: Сборка и запуск

### Вариант 1: Через Maven (без Docker)

```bash
cd service-4-order-generator
mvn clean package
java -jar target/order-generator-1.0.0.jar
```

### Вариант 2: Через Docker

```bash
cd service-4-order-generator
docker build -t order-generator .
docker run -p 8084:8084 order-generator
```

### Что должно произойти при запуске

1. Spring Boot стартует, создаёт таблицу `orders` в H2.
2. Через 10 секунд в консоли появляется первое сообщение:
   ```
   >>> Generated order: Phone | qty: 7 | price: 543.21 | status: NEW
   ```
3. Каждые 10 секунд --- новый заказ.
4. В логах видны SQL-запросы `INSERT INTO orders ...` (благодаря `show-sql: true`).

> **Обрати внимание**: заказы начинают генерироваться **автоматически** --- тебе
> не нужно ничего вызывать. Это работа `@Scheduled`.

---

## Шаг 12: Тестирование (curl)

### Тест 1: Подождать и получить автогенерированные заказы

Подожди 30 секунд после запуска (чтобы накопилось ~3 заказа), затем:

```bash
curl http://localhost:8084/api/orders
```

Ожидаемый ответ --- массив из ~3 заказов:
```json
[
  {
    "id": 1,
    "product": "Monitor",
    "quantity": 3,
    "price": 1234.56,
    "status": "NEW",
    "createdAt": "2024-06-15T10:30:10"
  },
  {
    "id": 2,
    "product": "Phone",
    "quantity": 8,
    "price": 789.01,
    "status": "PROCESSING",
    "createdAt": "2024-06-15T10:30:20"
  },
  ...
]
```

### Тест 2: Получить заказ по ID

```bash
curl http://localhost:8084/api/orders/1
```

### Тест 3: Создать заказ вручную

```bash
curl -X POST http://localhost:8084/api/orders ^
  -H "Content-Type: application/json" ^
  -d "{\"product\": \"Keyboard\", \"quantity\": 2, \"price\": 150.00, \"status\": \"NEW\"}"
```

> **Обрати внимание**: `createdAt` не передаётся в запросе --- сервис устанавливает
> его автоматически (см. метод `create` в `OrderService`).

### Тест 4: Получить новые заказы (ключевой эндпоинт)

```bash
curl "http://localhost:8084/api/orders/new?since=2024-01-01T00:00:00"
```

Этот запрос вернёт все заказы, созданные после 1 января 2024 года (то есть все).

Попробуй использовать более близкое время --- например, время на 20 секунд назад ---
чтобы получить только самые свежие заказы. Именно так будет работать Service 5.

### Тест 5: Наблюдение за консолью

Во время всех тестов следи за консолью (терминалом, где запущен сервис). Ты увидишь:

1. Сообщения `>>> Generated order: ...` каждые 10 секунд.
2. SQL-запросы `INSERT INTO orders ...` при генерации.
3. SQL-запросы `SELECT ... FROM orders ...` при GET-запросах.

Это помогает понять, что происходит "под капотом".

### Тест 6: H2 Console (бонус)

Открой в браузере: `http://localhost:8084/h2-console`

- **JDBC URL**: `jdbc:h2:mem:orderdb`
- **User Name**: `sa`
- **Password**: (пусто)

Можешь выполнять SQL-запросы напрямую:

```sql
SELECT * FROM orders;
SELECT COUNT(*) FROM orders;
SELECT * FROM orders WHERE status = 'NEW';
```

---

## Шаг 13: Бонусные задания

Если основная часть работает --- попробуй расширить сервис.

### Бонус 1: Настраиваемый интервал генерации

Сейчас интервал жёстко задан в коде (`fixedRate = 10000`). Вынеси его в конфигурацию:

1. В `application.yml` добавь:
   ```yaml
   order:
     generation:
       interval: 10000
   ```

2. В `@Scheduled` используй SpEL-выражение:
   ```java
   @Scheduled(fixedRateString = "${order.generation.interval}")
   ```

> **Подсказка**: обрати внимание --- `fixedRateString`, а не `fixedRate`.
> Для значений из конфигурации нужна строковая версия параметра.

Теперь интервал можно менять без перекомпиляции --- просто измени `application.yml`
или передай переменную окружения `ORDER_GENERATION_INTERVAL=5000`.

### Бонус 2: Отмена заказа

Добавь эндпоинт для смены статуса заказа на `"CANCELLED"`:

```
PUT /api/orders/{id}/cancel
```

Подсказки:
- В `OrderService` добавь метод `cancelOrder(Long id)`.
- Найди заказ по ID, измени `status` на `"CANCELLED"`, сохрани.
- Подумай: нужно ли разрешать отмену уже завершённых (`COMPLETED`) заказов?

### Бонус 3: Фильтрация по статусу

Добавь эндпоинт для получения заказов определённого статуса:

```
GET /api/orders/status/{status}
```

Подсказки:
- В `OrderRepository` добавь метод `findByStatus(String status)`.
- Подумай: как назвать метод в репозитории, чтобы Spring Data понял, что нужен
  запрос `WHERE status = ?`?

### Бонус 4: Статистика по товарам

Добавь эндпоинт для получения статистики:

```
GET /api/orders/stats
```

Ожидаемый ответ:
```json
{
  "totalOrders": 42,
  "totalRevenue": 45678.90,
  "ordersByProduct": {
    "Laptop": 8,
    "Phone": 12,
    "Tablet": 7,
    "Headphones": 9,
    "Monitor": 6
  }
}
```

Подсказки:
- Создай отдельный DTO-класс `OrderStats` для ответа.
- Используй Java Streams для подсчёта: `stream().collect(Collectors.groupingBy(...))`.
- Для `totalRevenue` используй `stream().mapToDouble(o -> o.getPrice() * o.getQuantity()).sum()`.

---

## Что дальше?

Когда этот сервис работает и генерирует заказы --- переходи к **Service 5 (Order Dashboard)**.
Service 5 будет потребителем (consumer): он будет опрашивать `GET /api/orders/new?since=...`
и отображать свежие заказы в реальном времени.

Связка Service 4 + Service 5 --- это твой первый пример **межсервисного взаимодействия**
через REST API.
