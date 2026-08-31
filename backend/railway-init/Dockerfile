# PerfFlow Railway 建表执行器
# 一次性容器：启动时连接 Railway 内网 MySQL 执行建表脚本，完成后退出
FROM mysql:8

# mysql 客户端在镜像 PATH 中
COPY perfflow.sql /app/perfflow.sql
COPY init.sh /app/init.sh
RUN chmod +x /app/init.sh

# 覆盖 mysql 镜像默认入口（默认会启动 mysqld 服务器），改为执行建表脚本
ENTRYPOINT ["/app/init.sh"]
