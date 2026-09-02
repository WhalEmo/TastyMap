# -------- Build stage --------
FROM gradle:8.10.2-jdk17 AS build
WORKDIR /workspace
COPY build.gradle.kts settings.gradle.kts gradlew* ./
COPY gradle ./gradle

RUN gradle dependencies --no-daemon || true
COPY . .
RUN gradle clean bootJar -x test --no-daemon

# -------- Runtime stage --------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Yüklenen resimlerin saklanacağı klasörü oluşturuyoruz ve izinlerini ayarlıyoruz
RUN mkdir -p /app/uploads/profiles && chmod -R 777 /app/uploads

# Tüm jar’ları kopyala ve izinlerini ayarla
COPY --from=build /workspace/build/libs/*.jar .
RUN chmod +r *.jar

ENTRYPOINT ["java","-jar","/app/TastyMap-0.0.1-SNAPSHOT.jar"]