# -------- Build stage --------
FROM gradle:8.10.2-jdk17 AS build
WORKDIR /workspace
COPY build.gradle.kts settings.gradle.kts gradlew* ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon || true
COPY . .
RUN ./gradlew clean bootJar -x test --no-daemon

# -------- Runtime stage --------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
# Tüm jar’ları kopyala ve izinlerini ayarla
COPY --from=build /workspace/build/libs/*.jar .
RUN chmod +r *.jar
ENTRYPOINT ["java","-jar","/app/TastyMap-0.0.1-SNAPSHOT.jar"]
