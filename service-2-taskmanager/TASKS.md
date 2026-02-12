# Service 2: Task Manager — CRUD REST API на чистой Java

---

## Шаг 1: Цель и что ты узнаешь

В этом сервисе ты построишь **полноценный CRUD REST API** для управления задачами (Task Manager).
Никаких фреймворков — только встроенный `com.sun.net.httpserver.HttpServer` и библиотека Gson для работы с JSON.

### Что ты освоишь:

- **CRUD-операции** — Create (создание), Read (чтение), Update (обновление), Delete (удаление)
- **Ручная маршрутизация** — один обработчик обрабатывает разные HTTP-методы и пути
- **Парсинг пути** — извлечение ID из URL вида `/tasks/123`
- **Чтение тела запроса** — получение JSON из POST/PUT запросов
- **HTTP статус-коды** — правильное использование 200, 201, 400, 404, 405
- **In-memory хранилище** — потокобезопасное хранение данных в `ConcurrentHashMap`
- **Генерация ID** — использование `AtomicLong` для уникальных идентификаторов
- **Валидация данных** — проверка обязательных полей перед созданием объекта
- **Работа с Optional** — безопасная обработка случаев "объект не найден"

### Какие эндпоинты ты реализуешь:

| Метод    | Путь            | Описание              | Статус ответа  |
|----------|-----------------|----------------------|----------------|
| `GET`    | `/tasks`        | Список всех задач    | 200 OK         |
| `POST`   | `/tasks`        | Создать задачу       | 201 Created    |
| `GET`    | `/tasks/{id}`   | Задача по ID         | 200 / 404      |
| `PUT`    | `/tasks/{id}`   | Обновить задачу      | 200 / 404      |
| `DELETE` | `/tasks/{id}`   | Удалить задачу       | 200 / 404      |

### Структура проекта:

```
service-2-taskmanager/
├── pom.xml                          ← конфигурация Maven
├── Dockerfile                       ← контейнеризация
├── TASKS.md                         ← это руководство
└── src/main/java/com/tutorial/taskmanager/
    ├── App.java                     ← точка входа, запуск сервера
    ├── model/Task.java              ← модель данных (POJO)
    ├── storage/TaskStorage.java     ← in-memory хранилище
    └── handlers/TaskHandler.java    ← обработка HTTP-запросов + маршрутизация
```

> **Отличие от service-1:** в первом сервисе каждый путь имел свой обработчик.
> Здесь **один обработчик** (`TaskHandler`) обрабатывает **все** запросы к `/tasks` и `/tasks/{id}`,
> сам определяя, что делать, по HTTP-методу и структуре пути.

---

## Шаг 2: Настройка pom.xml

Открой файл `pom.xml`. Сейчас он содержит только TODO-комментарии. Тебе нужно написать полную конфигурацию Maven.

### Что нужно сделать:

1. Объяви базовую структуру `<project>` с `modelVersion` 4.0.0
2. Укажи координаты проекта:
   - `groupId`: `com.tutorial`
   - `artifactId`: `task-manager`
   - `version`: `1.0.0`
   - `packaging`: `jar`
3. Задай свойства:
   - `maven.compiler.source` и `maven.compiler.target`: `17`
   - `project.build.sourceEncoding`: `UTF-8`
4. Добавь зависимость **Gson**:
   - `groupId`: `com.google.code.gson`
   - `artifactId`: `gson`
   - `version`: `2.10.1`
5. Настрой плагины:
   - **maven-jar-plugin** — укажи `mainClass`: `com.tutorial.taskmanager.App`
   - **maven-shade-plugin** — для создания fat JAR (JAR со всеми зависимостями)

### Подсказка по структуре pom.xml:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>???</groupId>
    <artifactId>???</artifactId>
    <version>???</version>
    <packaging>jar</packaging>

    <properties>
        <!-- версия Java и кодировка -->
    </properties>

    <dependencies>
        <!-- Gson -->
    </dependencies>

    <build>
        <plugins>
            <!-- maven-jar-plugin: mainClass -->
            <!-- maven-shade-plugin: fat jar -->
        </plugins>
    </build>
</project>
```

### Подсказка по maven-shade-plugin:

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

> **Зачем shade-plugin?** Обычный JAR не содержит библиотеку Gson внутри себя.
> Shade-plugin "вшивает" все зависимости в один JAR-файл, чтобы его можно было
> запустить командой `java -jar` без дополнительных настроек classpath.

---

## Шаг 3: App.java — точка входа

Открой файл `src/main/java/com/tutorial/taskmanager/App.java`.

### Что нужно сделать:

1. Создай `HttpServer` на порту **8082**
2. Создай экземпляр `TaskStorage` (хранилище задач)
3. Создай экземпляр `TaskHandler`, передав ему `TaskStorage`
4. Зарегистрируй `TaskHandler` на путь `"/tasks"`
5. Запусти сервер

### Подсказка:

```java
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

// Внутри main:
HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);

TaskStorage storage = new TaskStorage();
server.createContext("/tasks", new TaskHandler(storage));

server.setExecutor(null); // использовать стандартный executor
server.start();

System.out.println("TaskManager server started on port 8082");
```

### Важный момент — путь "/tasks":

Когда ты регистрируешь обработчик на путь `"/tasks"`, сервер направит в него **все** запросы,
начинающиеся с `/tasks`:
- `/tasks` — попадает в обработчик
- `/tasks/` — попадает в обработчик
- `/tasks/123` — тоже попадает в обработчик
- `/tasks/123/subtask` — тоже попадает

Поэтому **внутри обработчика** тебе нужно самому разобрать путь и определить, что делать.

> **Не забудь** добавить правильные `import`-ы для классов `TaskStorage` и `TaskHandler`.

---

## Шаг 4: Модель Task.java

Открой файл `src/main/java/com/tutorial/taskmanager/model/Task.java`.

Это **POJO** (Plain Old Java Object) — простой Java-класс, описывающий структуру данных задачи.

### Поля класса:

| Поле          | Тип       | Описание                         |
|---------------|-----------|----------------------------------|
| `id`          | `long`    | Уникальный идентификатор         |
| `title`       | `String`  | Название задачи (обязательное)   |
| `description` | `String`  | Описание задачи (необязательное) |
| `completed`   | `boolean` | Статус выполнения                |
| `createdAt`   | `String`  | Дата создания (ISO-формат)       |

### Что нужно реализовать:

1. **Приватные поля** — все поля должны быть `private`
2. **Конструктор** — принимает `id`, `title`, `description`; устанавливает `completed = false` и `createdAt` в текущее время
3. **Геттеры** — метод для чтения каждого поля (`getId()`, `getTitle()`, ...)
4. **Сеттеры** — метод для изменения каждого поля (`setTitle(String title)`, ...)

### Подсказка по конструктору:

```java
public Task(long id, String title, String description) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.completed = false;
    this.createdAt = java.time.LocalDateTime.now().toString();
}
```

### Подсказка по геттерам и сеттерам:

```java
// Геттер — возвращает значение поля
public long getId() {
    return id;
}

// Сеттер — устанавливает значение поля
public void setTitle(String title) {
    this.title = title;
}

// Для boolean геттер обычно начинается с "is"
public boolean isCompleted() {
    return completed;
}
```

### Зачем POJO?

- **Инкапсуляция** — данные спрятаны за private, доступ только через методы
- **Gson автоматически** конвертирует POJO в JSON и обратно, используя имена полей
- **Контроль** — в сеттере можно добавить валидацию (например, `title` не может быть пустым)

### Как Gson работает с POJO:

Gson использует **имена полей** для формирования JSON. Если поле называется `createdAt`,
в JSON оно будет `"createdAt"`. Пример результата:

```json
{
  "id": 1,
  "title": "Изучить Java",
  "description": "Прочитать главу 5",
  "completed": false,
  "createdAt": "2025-01-15T10:30:00"
}
```

---

## Шаг 5: TaskStorage.java — in-memory хранилище

Открой файл `src/main/java/com/tutorial/taskmanager/storage/TaskStorage.java`.

Это класс, который хранит все задачи в памяти и предоставляет методы для CRUD-операций.

### Что нужно знать перед написанием:

**ConcurrentHashMap** — потокобезопасная версия `HashMap`.
HTTP-сервер обрабатывает запросы в нескольких потоках одновременно. Обычный `HashMap`
может сломаться при одновременной записи из разных потоков. `ConcurrentHashMap` решает эту проблему.

```java
// Обычный HashMap — НЕ потокобезопасный (может сломаться)
Map<Long, Task> tasks = new HashMap<>();

// ConcurrentHashMap — потокобезопасный (можно использовать из разных потоков)
Map<Long, Task> tasks = new ConcurrentHashMap<>();
```

**AtomicLong** — потокобезопасный счетчик. Гарантирует, что даже при одновременных запросах
каждая задача получит уникальный ID.

```java
AtomicLong idCounter = new AtomicLong(1);

long newId = idCounter.getAndIncrement(); // вернет текущее значение и увеличит на 1
// Первый вызов: вернет 1, станет 2
// Второй вызов: вернет 2, станет 3
```

### Поля класса:

```java
private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
private final AtomicLong idCounter = new AtomicLong(1);
```

### Методы, которые нужно реализовать:

#### 1. `create(String title, String description)` — создание задачи

- Генерирует новый ID через `idCounter.getAndIncrement()`
- Создает объект `Task` с этим ID, title и description
- Кладет задачу в `tasks` (ключ — ID)
- Возвращает созданную задачу

```java
public Task create(String title, String description) {
    long id = idCounter.getAndIncrement();
    Task task = new Task(id, title, description);
    tasks.put(id, task);
    return task;
}
```

#### 2. `findAll()` — получение всех задач

- Возвращает все задачи из Map в виде списка

**Подсказка:** `Map` имеет метод `.values()`, который возвращает коллекцию всех значений.
Из нее можно создать `ArrayList`:

```java
public List<Task> findAll() {
    return new ArrayList<>(tasks.values());
}
```

#### 3. `findById(long id)` — поиск задачи по ID

- Возвращает `Optional<Task>` — контейнер, который может содержать задачу или быть пустым

**Зачем Optional?** Вместо того чтобы возвращать `null` (который легко забыть проверить),
`Optional` заставляет программиста **явно** обработать случай "не найдено".

```java
public Optional<Task> findById(long id) {
    return Optional.ofNullable(tasks.get(id));
    // tasks.get(id) вернет null, если ключа нет
    // Optional.ofNullable(null) создаст пустой Optional
    // Optional.ofNullable(task) создаст Optional с задачей внутри
}
```

**Как потом использовать Optional:**

```java
Optional<Task> optTask = storage.findById(id);

if (optTask.isPresent()) {
    Task task = optTask.get();
    // работаем с задачей
} else {
    // задача не найдена — вернуть 404
}
```

#### 4. `update(long id, String title, String description, Boolean completed)` — обновление

- Находит задачу по ID
- Если найдена — обновляет **только те поля, которые не null** (частичное обновление)
- Возвращает `Optional<Task>` — обновленную задачу или пустой Optional

**Подсказка:** обрати внимание, параметр `completed` имеет тип `Boolean` (объект), а не `boolean` (примитив).
Это позволяет передать `null`, если клиент не хочет менять это поле.

```java
public Optional<Task> update(long id, String title, String description, Boolean completed) {
    Task task = tasks.get(id);
    if (task == null) {
        return Optional.empty();
    }
    // Обновляем только если значение не null
    if (title != null) {
        task.setTitle(title);
    }
    // ... аналогично для description и completed
    return Optional.of(task);
}
```

#### 5. `delete(long id)` — удаление

- Удаляет задачу из Map
- Возвращает `true`, если задача была удалена, `false` если не была найдена

**Подсказка:** метод `Map.remove(key)` возвращает удаленное значение или `null`.

```java
public boolean delete(long id) {
    return tasks.remove(id) != null;
}
```

### Необходимые импорты:

```java
import com.tutorial.taskmanager.model.Task;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
```

---

## Шаг 6: TaskHandler.java — маршрутизация

Открой файл `src/main/java/com/tutorial/taskmanager/handlers/TaskHandler.java`.

**Это главный вызов этого проекта.** Здесь ты реализуешь:
- Разбор HTTP-метода (GET, POST, PUT, DELETE)
- Парсинг пути (различение `/tasks` и `/tasks/123`)
- Чтение тела запроса (для POST и PUT)
- Формирование ответов с правильными статус-кодами

### Что нужно сделать:

1. Класс должен реализовать интерфейс `HttpHandler`
2. Принимать `TaskStorage` через конструктор
3. В методе `handle(HttpExchange exchange)` реализовать маршрутизацию

### Базовая структура класса:

```java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tutorial.taskmanager.storage.TaskStorage;

public class TaskHandler implements HttpHandler {

    private final TaskStorage storage;

    public TaskHandler(TaskStorage storage) {
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Вся логика маршрутизации здесь
    }
}
```

---

### 6.1 Как определить маршрут

У тебя есть два параметра для маршрутизации:

1. **HTTP-метод** — `exchange.getRequestMethod()` возвращает `"GET"`, `"POST"`, `"PUT"`, `"DELETE"`
2. **Путь** — `exchange.getRequestURI().getPath()` возвращает `"/tasks"` или `"/tasks/123"`

Нужно различать два типа путей:
- **Коллекция:** `/tasks` — работа со всеми задачами (GET список, POST создание)
- **Конкретный ресурс:** `/tasks/{id}` — работа с одной задачей (GET, PUT, DELETE по ID)

### 6.2 Парсинг пути — извлечение ID

Чтобы извлечь ID из пути `/tasks/123`, разбей путь по символу `/`:

```java
String path = exchange.getRequestURI().getPath(); // "/tasks/123"
String[] parts = path.split("/");
// parts[0] = ""       (пустая строка перед первым /)
// parts[1] = "tasks"
// parts[2] = "123"    (это ID)
```

Определить тип пути можно по количеству частей:

```java
boolean hasId = parts.length == 3 && !parts[2].isEmpty();
```

Если есть ID, нужно его распарсить:

```java
if (hasId) {
    try {
        long id = Long.parseLong(parts[2]);
        // ... обработка запроса с ID
    } catch (NumberFormatException e) {
        // путь типа /tasks/abc — невалидный ID
        // вернуть 400 Bad Request
    }
}
```

### 6.3 Структура маршрутизации (if/else)

```java
@Override
public void handle(HttpExchange exchange) throws IOException {
    String method = exchange.getRequestMethod();
    String path = exchange.getRequestURI().getPath();
    String[] parts = path.split("/");
    boolean hasId = parts.length == 3 && !parts[2].isEmpty();

    try {
        if (!hasId) {
            // Маршруты для /tasks (коллекция)
            if ("GET".equals(method)) {
                handleGetAll(exchange);
            } else if ("POST".equals(method)) {
                handleCreate(exchange);
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } else {
            // Маршруты для /tasks/{id} (конкретный ресурс)
            long id = Long.parseLong(parts[2]);

            if ("GET".equals(method)) {
                handleGetById(exchange, id);
            } else if ("PUT".equals(method)) {
                handleUpdate(exchange, id);
            } else if ("DELETE".equals(method)) {
                handleDelete(exchange, id);
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        }
    } catch (NumberFormatException e) {
        sendResponse(exchange, 400, "{\"error\":\"Invalid ID format\"}");
    } catch (Exception e) {
        sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
    }
}
```

> **Примечание:** этот код — каркас маршрутизации.
> Тебе нужно самому реализовать каждый из методов `handleGetAll`, `handleCreate`,
> `handleGetById`, `handleUpdate`, `handleDelete` и вспомогательный `sendResponse`.

---

### 6.4 Вспомогательный метод: отправка ответа

Этот метод тебе понадобится во всех обработчиках. Реализуй его первым:

```java
private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
    byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
    }
}
```

**Важные моменты:**
- `Content-Type: application/json` — сообщает клиенту, что ответ в формате JSON
- `charset=UTF-8` — поддержка кириллицы и других символов
- `sendResponseHeaders(statusCode, length)` — первый аргумент: HTTP-код, второй: длина тела в байтах
- `try (OutputStream os = ...)` — конструкция **try-with-resources** автоматически закроет поток

---

### 6.5 Чтение тела запроса (для POST и PUT)

Когда клиент отправляет POST или PUT, данные передаются в теле запроса (request body).
Тебе нужно прочитать это тело как строку:

```java
private String readRequestBody(HttpExchange exchange) throws IOException {
    try (InputStream is = exchange.getRequestBody()) {
        return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
```

**Или вариант с BufferedReader:**

```java
private String readRequestBody(HttpExchange exchange) throws IOException {
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
```

---

### 6.6 Парсинг JSON тела запроса

Когда тело прочитано в строку, нужно извлечь из него данные. Используй `Gson`
через класс `JsonParser`:

```java
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// Внутри handleCreate или handleUpdate:
String body = readRequestBody(exchange);
JsonObject json = JsonParser.parseString(body).getAsJsonObject();

// Извлечение полей:
String title = json.has("title") ? json.get("title").getAsString() : null;
String description = json.has("description") ? json.get("description").getAsString() : null;
```

**Важно:** всегда проверяй `json.has("поле")` перед `json.get("поле")`,
иначе получишь `NullPointerException`.

Для поля `completed` типа `Boolean`:

```java
Boolean completed = json.has("completed") ? json.get("completed").getAsBoolean() : null;
```

---

### 6.7 Реализация обработчиков — что нужно сделать самому

Теперь реализуй каждый метод. Вот описание логики каждого:

#### `handleGetAll(HttpExchange exchange)`

1. Получи список всех задач из `storage.findAll()`
2. Преврати список в JSON-строку с помощью `new Gson().toJson(list)`
3. Отправь ответ со статусом **200**

#### `handleCreate(HttpExchange exchange)`

1. Прочитай тело запроса
2. Распарси JSON
3. Извлеки поле `title` — оно **обязательное**
4. Если `title` отсутствует или пустой — верни **400** с сообщением об ошибке
5. Извлеки `description` (необязательное, может быть `null` или пустой строкой)
6. Создай задачу через `storage.create(title, description)`
7. Преврати созданную задачу в JSON
8. Отправь ответ со статусом **201**

> **Подсказка по валидации title:**
> ```java
> if (title == null || title.trim().isEmpty()) {
>     sendResponse(exchange, 400, "{\"error\":\"Title is required\"}");
>     return;
> }
> ```

#### `handleGetById(HttpExchange exchange, long id)`

1. Получи задачу через `storage.findById(id)`
2. Если `Optional` пустой — верни **404**
3. Если задача найдена — верни **200** с задачей в JSON

#### `handleUpdate(HttpExchange exchange, long id)`

1. Прочитай и распарси тело запроса
2. Извлеки поля `title`, `description`, `completed` (все необязательные — частичное обновление)
3. Вызови `storage.update(id, title, description, completed)`
4. Если `Optional` пустой — верни **404**
5. Если задача обновлена — верни **200** с обновленной задачей в JSON

#### `handleDelete(HttpExchange exchange, long id)`

1. Вызови `storage.delete(id)`
2. Если вернулось `true` — верни **200** с сообщением об успехе
3. Если вернулось `false` — верни **404**

> **Подсказка по сообщению успешного удаления:**
> ```java
> sendResponse(exchange, 200, "{\"message\":\"Task deleted\"}");
> ```

---

### 6.8 Необходимые импорты для TaskHandler

```java
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tutorial.taskmanager.model.Task;
import com.tutorial.taskmanager.storage.TaskStorage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
```

---

## Шаг 7: Понимание HTTP статус-кодов

Каждый HTTP-ответ содержит числовой код, который сообщает клиенту результат операции.

### Справочная таблица:

| Код | Имя                    | Когда использовать                                         |
|-----|------------------------|------------------------------------------------------------|
| 200 | OK                     | Запрос выполнен успешно (GET, PUT, DELETE)                  |
| 201 | Created                | Ресурс успешно создан (POST)                               |
| 400 | Bad Request            | Клиент прислал невалидные данные (нет title, битый JSON)   |
| 404 | Not Found              | Ресурс не найден (задача с таким ID не существует)         |
| 405 | Method Not Allowed     | HTTP-метод не поддерживается для этого пути                |
| 500 | Internal Server Error  | Непредвиденная ошибка на сервере                           |

### Категории кодов:

- **2xx** — успех
- **4xx** — ошибка клиента (клиент прислал что-то не то)
- **5xx** — ошибка сервера (сервер сломался)

### Почему это важно:

```
POST /tasks с пустым title
→ 400 Bad Request + {"error": "Title is required"}

GET /tasks/999 (задачи с ID 999 нет)
→ 404 Not Found + {"error": "Task not found"}

PATCH /tasks (метод PATCH не реализован)
→ 405 Method Not Allowed + {"error": "Method Not Allowed"}
```

Клиентское приложение (фронтенд, мобильное приложение) использует эти коды, чтобы понять,
что произошло, и показать пользователю правильное сообщение.

---

## Шаг 8: Работа с JSON (Gson)

### 8.1 Создание JSON из объекта (сериализация)

Gson умеет автоматически преобразовывать Java-объекты в JSON-строку:

```java
Gson gson = new Gson();

// Один объект
Task task = storage.create("Изучить Java", "Глава 5");
String json = gson.toJson(task);
// Результат: {"id":1,"title":"Изучить Java","description":"Глава 5","completed":false,"createdAt":"2025-01-15T10:30:00"}

// Список объектов
List<Task> tasks = storage.findAll();
String jsonArray = gson.toJson(tasks);
// Результат: [{"id":1,...}, {"id":2,...}]
```

### 8.2 Парсинг JSON из запроса (десериализация)

В этом проекте мы **не** используем `gson.fromJson()` для десериализации тела запроса в объект `Task`,
потому что клиент не должен устанавливать `id` и `createdAt` — это делает сервер.

Вместо этого мы парсим JSON вручную, извлекая только нужные поля:

```java
String body = readRequestBody(exchange);
JsonObject json = JsonParser.parseString(body).getAsJsonObject();

// Безопасное извлечение полей:
String title = json.has("title") ? json.get("title").getAsString() : null;
String description = json.has("description") ? json.get("description").getAsString() : null;
Boolean completed = json.has("completed") ? json.get("completed").getAsBoolean() : null;
```

### 8.3 Обработка невалидного JSON

Клиент может прислать невалидный JSON (например, `{broken`). Нужно это обрабатывать:

```java
try {
    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
    // ... работаем с json
} catch (Exception e) {
    sendResponse(exchange, 400, "{\"error\":\"Invalid JSON\"}");
    return;
}
```

### 8.4 Формирование JSON-ответов об ошибках

Для ответов об ошибках можно использовать простую строковую конкатенацию или `JsonObject`:

**Вариант 1 — строка (простой):**

```java
sendResponse(exchange, 404, "{\"error\":\"Task not found\"}");
```

**Вариант 2 — через JsonObject (правильнее):**

```java
JsonObject error = new JsonObject();
error.addProperty("error", "Task not found");
sendResponse(exchange, 404, error.toString());
```

> Второй вариант безопаснее — он правильно экранирует спецсимволы в сообщении.

---

## Шаг 9: Dockerfile

Открой файл `Dockerfile`. Напиши multi-stage сборку — такой же паттерн, как в service-1.

### Два этапа:

**Stage 1: Сборка (Build)**
- Базовый образ: `maven:3.9-eclipse-temurin-17`
- Рабочая директория: `/app`
- Скопируй `pom.xml` и `src/`
- Выполни `mvn clean package -DskipTests`

**Stage 2: Запуск (Run)**
- Базовый образ: `eclipse-temurin:17-jre` (только JRE — меньше размер)
- Рабочая директория: `/app`
- Скопируй JAR из первого этапа
- Открой порт `8082`
- Команда запуска: `java -jar app.jar`

### Подсказка:

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE ???
CMD ["java", "-jar", "app.jar"]
```

> **Замени `???`** на правильный порт.

### Зачем multi-stage?

- Stage 1 содержит Maven, JDK, исходный код — это тяжелый образ (~800 МБ)
- Stage 2 содержит только JRE и скомпилированный JAR — это легкий образ (~300 МБ)
- В продакшене нам не нужен компилятор и исходный код — только готовый JAR

---

## Шаг 10: Сборка и запуск

### 10.1 Сборка Maven:

```bash
cd service-2-taskmanager
mvn clean package
```

Если все правильно, в папке `target/` появится файл `task-manager-1.0.0.jar`.

### 10.2 Запуск локально:

```bash
java -jar target/task-manager-1.0.0.jar
```

Ты увидишь в консоли:
```
TaskManager server started on port 8082
```

### 10.3 Сборка Docker-образа:

```bash
docker build -t task-manager .
```

### 10.4 Запуск в Docker:

```bash
docker run -p 8082:8082 task-manager
```

### 10.5 Проверка:

```bash
curl http://localhost:8082/tasks
```

Ожидаемый ответ (пустой список):
```json
[]
```

### Если что-то пошло не так:

| Проблема                        | Что проверить                                             |
|---------------------------------|-----------------------------------------------------------|
| `mvn clean package` не работает | Проверь `pom.xml` — правильный ли `groupId`, `artifactId` |
| `ClassNotFoundException`        | Проверь `mainClass` в `maven-jar-plugin`                  |
| `Address already in use`        | Порт 8082 уже занят — останови другой процесс             |
| `404` на все запросы            | Проверь, зарегистрирован ли handler на `/tasks`           |
| `Connection refused`            | Сервер не запущен или неправильный порт                   |

---

## Шаг 11: Тестирование (curl)

Проверь все CRUD-операции по порядку. Запусти сервер и выполни команды в отдельном терминале.

### 11.1 Создание задачи (POST)

**Первая задача:**

```bash
curl -s -X POST http://localhost:8082/tasks -H "Content-Type: application/json" -d "{\"title\":\"Изучить Java\",\"description\":\"Прочитать главу про коллекции\"}"
```

Ожидаемый ответ (**201 Created**):
```json
{
  "id": 1,
  "title": "Изучить Java",
  "description": "Прочитать главу про коллекции",
  "completed": false,
  "createdAt": "2025-01-15T10:30:00.123"
}
```

**Вторая задача:**

```bash
curl -s -X POST http://localhost:8082/tasks -H "Content-Type: application/json" -d "{\"title\":\"Написать REST API\",\"description\":\"Реализовать CRUD для задач\"}"
```

Ожидаемый ответ (**201 Created**):
```json
{
  "id": 2,
  "title": "Написать REST API",
  "description": "Реализовать CRUD для задач",
  "completed": false,
  "createdAt": "2025-01-15T10:31:00.456"
}
```

---

### 11.2 Получение всех задач (GET)

```bash
curl -s http://localhost:8082/tasks
```

Ожидаемый ответ (**200 OK**):
```json
[
  {
    "id": 1,
    "title": "Изучить Java",
    "description": "Прочитать главу про коллекции",
    "completed": false,
    "createdAt": "2025-01-15T10:30:00.123"
  },
  {
    "id": 2,
    "title": "Написать REST API",
    "description": "Реализовать CRUD для задач",
    "completed": false,
    "createdAt": "2025-01-15T10:31:00.456"
  }
]
```

---

### 11.3 Получение задачи по ID (GET)

```bash
curl -s http://localhost:8082/tasks/1
```

Ожидаемый ответ (**200 OK**):
```json
{
  "id": 1,
  "title": "Изучить Java",
  "description": "Прочитать главу про коллекции",
  "completed": false,
  "createdAt": "2025-01-15T10:30:00.123"
}
```

---

### 11.4 Обновление задачи (PUT)

**Пометить задачу как выполненную и обновить описание:**

```bash
curl -s -X PUT http://localhost:8082/tasks/1 -H "Content-Type: application/json" -d "{\"completed\":true,\"description\":\"Глава прочитана!\"}"
```

Ожидаемый ответ (**200 OK**):
```json
{
  "id": 1,
  "title": "Изучить Java",
  "description": "Глава прочитана!",
  "completed": true,
  "createdAt": "2025-01-15T10:30:00.123"
}
```

> Обрати внимание: `title` не был передан в теле запроса, поэтому он **не изменился**.
> Это называется **частичное обновление** (partial update).

---

### 11.5 Удаление задачи (DELETE)

```bash
curl -s -X DELETE http://localhost:8082/tasks/2
```

Ожидаемый ответ (**200 OK**):
```json
{
  "message": "Task deleted"
}
```

**Проверяем, что задача удалена:**

```bash
curl -s http://localhost:8082/tasks
```

Ожидаемый ответ — только одна задача:
```json
[
  {
    "id": 1,
    "title": "Изучить Java",
    "description": "Глава прочитана!",
    "completed": true,
    "createdAt": "2025-01-15T10:30:00.123"
  }
]
```

---

### 11.6 Запрос несуществующей задачи (404)

```bash
curl -s http://localhost:8082/tasks/999
```

Ожидаемый ответ (**404 Not Found**):
```json
{
  "error": "Task not found"
}
```

---

### 11.7 Создание задачи без title (400)

```bash
curl -s -X POST http://localhost:8082/tasks -H "Content-Type: application/json" -d "{\"description\":\"Без заголовка\"}"
```

Ожидаемый ответ (**400 Bad Request**):
```json
{
  "error": "Title is required"
}
```

---

### 11.8 Создание с пустым телом (400)

```bash
curl -s -X POST http://localhost:8082/tasks -H "Content-Type: application/json" -d "{}"
```

Ожидаемый ответ (**400 Bad Request**):
```json
{
  "error": "Title is required"
}
```

---

### 11.9 Удаление несуществующей задачи (404)

```bash
curl -s -X DELETE http://localhost:8082/tasks/999
```

Ожидаемый ответ (**404 Not Found**):
```json
{
  "error": "Task not found"
}
```

---

### 11.10 Просмотр статус-кода ответа

Чтобы увидеть HTTP статус-код, добавь флаг `-v` (verbose) или `-w`:

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8082/tasks -H "Content-Type: application/json" -d "{\"title\":\"Test\"}"
```

Результат: `201`

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/tasks/999
```

Результат: `404`

---

## Шаг 12: Бонусные задания

Ты реализовал базовый CRUD. Теперь можешь усложнить сервис. Каждое задание — самостоятельное, можно делать в любом порядке.

---

### 12.1 Фильтрация по статусу (completed)

**Цель:** `GET /tasks?completed=true` возвращает только выполненные задачи.

**Подсказки:**
- Query-параметры доступны через `exchange.getRequestURI().getQuery()`
- Этот метод возвращает строку вида `"completed=true&sort=title"` или `null` если параметров нет
- Тебе нужно распарсить эту строку и извлечь значение `completed`

```java
String query = exchange.getRequestURI().getQuery(); // "completed=true" или null

// Парсинг query-параметров:
Map<String, String> params = new HashMap<>();
if (query != null) {
    for (String param : query.split("&")) {
        String[] pair = param.split("=", 2);
        if (pair.length == 2) {
            params.put(pair[0], pair[1]);
        }
    }
}

// Использование:
String completedParam = params.get("completed"); // "true", "false" или null
```

Дальше отфильтруй список задач с помощью **Stream API**:

```java
List<Task> tasks = storage.findAll();
if (completedParam != null) {
    boolean completed = Boolean.parseBoolean(completedParam);
    tasks = tasks.stream()
        .filter(t -> t.isCompleted() == completed)
        .collect(Collectors.toList());
}
```

**Тестирование:**
```bash
# Только выполненные
curl -s "http://localhost:8082/tasks?completed=true"

# Только невыполненные
curl -s "http://localhost:8082/tasks?completed=false"
```

---

### 12.2 Поиск по названию (title)

**Цель:** `GET /tasks?search=java` возвращает задачи, в `title` которых содержится "java" (без учета регистра).

**Подсказка:** используй `String.toLowerCase().contains()` для поиска подстроки:

```java
String search = params.get("search");
if (search != null) {
    String searchLower = search.toLowerCase();
    tasks = tasks.stream()
        .filter(t -> t.getTitle().toLowerCase().contains(searchLower))
        .collect(Collectors.toList());
}
```

**Тестирование:**
```bash
curl -s "http://localhost:8082/tasks?search=java"
curl -s "http://localhost:8082/tasks?search=REST"
```

---

### 12.3 Пагинация (limit/offset)

**Цель:** `GET /tasks?limit=5&offset=10` возвращает 5 задач, начиная с 11-й.

Это нужно, когда задач много и нельзя отдать все разом.

**Подсказка:** используй `stream().skip().limit()`:

```java
String limitParam = params.get("limit");
String offsetParam = params.get("offset");

int limit = (limitParam != null) ? Integer.parseInt(limitParam) : tasks.size();
int offset = (offsetParam != null) ? Integer.parseInt(offsetParam) : 0;

List<Task> page = tasks.stream()
    .skip(offset)
    .limit(limit)
    .collect(Collectors.toList());
```

**Продвинутый вариант:** верни в ответе мета-информацию о пагинации:

```json
{
  "tasks": [...],
  "total": 25,
  "limit": 5,
  "offset": 10
}
```

Для этого нужно сформировать `JsonObject` вручную.

**Тестирование:**
```bash
# Первые 2 задачи
curl -s "http://localhost:8082/tasks?limit=2&offset=0"

# Следующие 2 задачи
curl -s "http://localhost:8082/tasks?limit=2&offset=2"
```

---

### 12.4 Сортировка

**Цель:** `GET /tasks?sort=title` или `GET /tasks?sort=createdAt` — сортировка по полю.

**Подсказка:** используй `Comparator`:

```java
String sort = params.get("sort");
if ("title".equals(sort)) {
    tasks.sort(Comparator.comparing(Task::getTitle));
} else if ("createdAt".equals(sort)) {
    tasks.sort(Comparator.comparing(Task::getCreatedAt));
}
```

**Обратная сортировка:** добавь параметр `order=desc`:

```java
String order = params.get("order");
if ("desc".equals(order)) {
    tasks.sort(comparator.reversed());
}
```

**Тестирование:**
```bash
# По названию
curl -s "http://localhost:8082/tasks?sort=title"

# По дате создания, новые первые
curl -s "http://localhost:8082/tasks?sort=createdAt&order=desc"
```

---

### 12.5 Комбинирование всех параметров

Когда все бонусы реализованы, можно комбинировать:

```bash
# Невыполненные задачи, содержащие "java", отсортированные по дате, первые 5
curl -s "http://localhost:8082/tasks?completed=false&search=java&sort=createdAt&limit=5"
```

**Важно:** порядок применения фильтров:
1. Фильтрация (completed, search)
2. Сортировка (sort, order)
3. Пагинация (limit, offset)

---

> **Поздравляю!** Ты реализовал полноценный REST API с ручной маршрутизацией,
> валидацией, статус-кодами и in-memory хранилищем. Это фундамент, на котором
> построены все веб-фреймворки — Spring, Express, Django делают то же самое,
> только с автоматической маршрутизацией и дополнительными удобствами.
