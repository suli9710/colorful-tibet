# 多阶段构建：先构建，再运行
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建必要的目录
RUN mkdir -p /app/data /app/logs

# 复制构建好的jar文件
COPY --from=builder /app/target/tourism-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=local"]



