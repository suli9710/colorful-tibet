# 多阶段构建：先构建前端，再使用nginx服务
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi
COPY . .

# 构建时传入的环境变量
ARG VITE_API_BASE_URL=/api
ARG VITE_RECAPTCHA_SITE_KEY=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_RECAPTCHA_SITE_KEY=$VITE_RECAPTCHA_SITE_KEY

RUN npm run build

FROM nginx:alpine
# 复制构建好的文件
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80 443

# nginx会自动启动
CMD ["nginx", "-g", "daemon off;"]

