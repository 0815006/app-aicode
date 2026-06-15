# ============================================
# 本地 docker-compose 前端镜像 (Vue 2 + Nginx)
# 构建方式: 由 docker-compose.yml 自动构建
# ============================================

FROM node:16-alpine AS build
WORKDIR /app
COPY package.json ./
# 注意: 此项目无 package-lock.json，使用 npm install
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
RUN npm run build

FROM nginx:alpine
# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html
# 使用自定义 nginx 配置（含 /api 反向代理）
COPY nginx-default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
