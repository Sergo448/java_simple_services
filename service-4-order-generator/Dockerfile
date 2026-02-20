# ======= Этап 1: Сборка =======
FROM maven:3.9-eclipse-temurin-22 AS builder

WORKDIR /app

# Копируем только pom.xml для кеширования зависимостей
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код
COPY src ./src
RUN mvn clean package -DskipTests

# ======= Этап 2: Запуск =======
FROM eclipse-temurin:22-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8084

CMD ["java", "-jar", "app.jar"]