# Используем OpenJDK 17
FROM openjdk:17-jdk-slim

# Рабочая директория внутри контейнера
WORKDIR /app

# Копируем собранный JAR (перед этим надо скомпилировать)
COPY target/*.jar app.jar

# Запускаем Spring Boot приложение
CMD ["java", "-jar", "app.jar"]
