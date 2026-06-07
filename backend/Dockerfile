# 多阶段构建：先构建，再运行
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ARG APP_UID=10001
ARG APP_GID=10001

# 创建必要的目录并安装运行期依赖
RUN apk add --no-cache curl \
    && addgroup -S -g "$APP_GID" app \
    && adduser -S -D -H -u "$APP_UID" -G app app \
    && mkdir -p /app/data /app/logs \
    && chown -R app:app /app

# 复制构建好的jar文件
COPY --from=builder --chown=app:app /app/target/tourism-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
USER app

# 暴露端口
EXPOSE 8080

# 启动应用，运行环境通过 SPRING_PROFILES_ACTIVE 控制
ENTRYPOINT ["java", "-jar", "app.jar"]

