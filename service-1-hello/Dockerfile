# ======= Этап 1: Сборка =======
FROM maven:3.9-eclipse-temurin-17 AS buider

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
