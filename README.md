# Java Services Tutorial — 5 мини-сервисов

Учебный проект: от чистой Java до Spring Boot микросервисов.

## Структура

```
java_simple_service/
├── service-1-hello/           # Чистая Java — HTTP-сервер "Hello World"
├── service-2-taskmanager/     # Чистая Java — CRUD REST API для задач
├── service-3-userservice/     # Spring Boot — управление пользователями + H2
├── service-4-order-generator/ # Spring Boot — генератор заказов (producer)
├── service-5-order-dashboard/ # Spring Boot — дашборд заказов (consumer)
├── docker-compose.yml
└── README.md
```

## Порядок прохождения

| # | Сервис | Технологии | Порт | Что изучаешь |
|---|--------|-----------|------|-------------|
| 1 | Hello Service | Java + HttpServer + Gson | 8081 | Как работает HTTP-сервер изнутри |
| 2 | Task Manager | Java + HttpServer + Gson | 8082 | CRUD REST API без фреймворков |
| 3 | User Service | Spring Boot + JPA + H2 | 8083 | Spring Boot, JPA, валидация, обработка ошибок |
| 4 | Order Generator | Spring Boot + JPA + H2 | 8084 | @Scheduled, генерация данных, producer |
| 5 | Order Dashboard | Spring Boot + RestTemplate | 8085 | Межсервисное взаимодействие, consumer |

**Сервисы 1-3** — независимые, делай в любом порядке.
**Сервисы 4 и 5** — связаны: дашборд (5) получает данные от генератора (4).

## Как работать

1. Открой папку нужного сервиса
2. Прочитай `TASKS.md` — там пошаговые инструкции
3. Заполни пустые файлы-болванки своим кодом
4. Собери и протестируй

## Требования

- **Java 17+**: `java -version`
- **Maven 3.8+**: `mvn -version`
- **Docker** (опционально): `docker --version`

## Запуск из консоли

Каждый сервис запускается одинаково:

```bash
cd service-N-name
mvn clean package
java -jar target/*.jar
```

## Запуск через Docker

Один сервис:

```bash
cd service-N-name
docker build -t service-name .
docker run -p PORT:PORT service-name
```

Все вместе:

```bash
docker-compose up --build
```

## Проверка

После запуска всех сервисов:

```bash
# Сервис 1
curl http://localhost:8081/hello
curl http://localhost:8081/time
curl http://localhost:8081/health

# Сервис 2
curl http://localhost:8082/tasks
curl -X POST http://localhost:8082/tasks -H "Content-Type: application/json" -d "{\"title\":\"Test\"}"

# Сервис 3
curl http://localhost:8083/api/users
curl -X POST http://localhost:8083/api/users -H "Content-Type: application/json" -d "{\"name\":\"Ivan\",\"email\":\"ivan@test.com\",\"age\":25}"

# Сервис 4
curl http://localhost:8084/api/orders

# Сервис 5 (подожди 15 сек после запуска, чтобы прошёл polling)
curl http://localhost:8085/api/dashboard/orders
curl http://localhost:8085/api/dashboard/stats
```

## Архитектура сервисов 4 + 5

```
┌──────────────────┐    REST (polling)    ┌──────────────────┐
│  Order Generator │ ◄──────────────────  │  Order Dashboard │
│    (порт 8084)   │  GET /api/orders/new │    (порт 8085)   │
│                  │                      │                  │
│  @Scheduled      │                      │  @Scheduled      │
│  генерация       │                      │  polling каждые  │
│  каждые 10 сек   │                      │  15 сек          │
│                  │                      │                  │
│  H2 Database     │                      │  In-memory Map   │
└──────────────────┘                      └──────────────────┘
```
