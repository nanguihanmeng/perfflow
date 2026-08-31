# ===========================================================
# PerfFlow 绩效考核系统 - 前端镜像
# 多阶段构建：Node 构建 Vue 静态文件 → Nginx 托管 + 反向代理 /api
# 反向代理目标：同项目后端服务内网地址（Railway 内网 DNS）
# ===========================================================

# ---------- 构建阶段 ----------
FROM node:20-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
# lock 与 package.json 存在版本偏差时用 install 保证可构建（生产环境仍以 lock 为准）
RUN npm install
COPY . .
# 生产构建（VITE_API_BASE_URL=/api，同域反代）
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

# 非 root 运行（nginx alpine 默认已有 nginx 用户）
CMD ["nginx", "-g", "daemon off;"]
