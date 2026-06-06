# ====== 轻量运行镜像（本地打好 jar 后上传） ======
FROM openjdk:17-jdk-alpine

LABEL maintainer="luckzll"

WORKDIR /app

# 安装字体支持（Tika/POI 解析文档可能需要） + 设置时区为东八区
RUN apk add --no-cache fontconfig ttf-dejavu tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

ENV TZ=Asia/Shanghai

# 复制本地已打好的 jar（构建前需将 jar 放在 Dockerfile 同级目录）
COPY app.jar app.jar

# 创建上传目录
RUN mkdir -p /app/uploads/knowledge

# 暴露端口
EXPOSE 8080

# JVM 参数优化（容器环境）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
