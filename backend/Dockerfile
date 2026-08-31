# ===========================================================
# PerfFlow 绩效考核系统 - 后端镜像
# 多阶段构建：Maven 编译打包 → JRE 运行
# 生产默认激活 prod profile，数据库连接通过环境变量注入：
#   DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PWD
# ===========================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
# 预下载依赖（后续改代码时利用 Docker 层缓存）
RUN mvn -B dependency:go-offline -q || true
COPY src ./src
RUN mvn -B -DskipTests package -q

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 非 root 运行 + 日志目录
RUN useradd -r -s /sbin/nologin perfflow \
    && mkdir -p /var/log/perfflow \
    && chown -R perfflow:perfflow /var/log/perfflow

COPY --from=build /build/target/perfflow-backend.jar /app/perfflow-backend.jar

USER perfflow
EXPOSE 8080

# 覆盖 application.yml 中写死的 dev profile，生产走 prod
ENV SPRING_PROFILES_ACTIVE=prod
ENV DB_HOST=mysql DB_PORT=3306 DB_NAME=perfflow DB_USER=perfflow DB_PWD=perfflow@123
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/perfflow-backend.jar"]
