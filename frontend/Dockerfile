# 多阶段构建：先构建前端，再使用nginx服务
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi
COPY . .

# 构建时传入的环境变量
ARG VITE_API_BASE_URL=/api
ARG VITE_RECAPTCHA_SITE_KEY=
ARG VITE_AMAP_KEY=
ARG VITE_AMAP_SECURITY_CODE=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_RECAPTCHA_SITE_KEY=$VITE_RECAPTCHA_SITE_KEY
ENV VITE_AMAP_KEY=$VITE_AMAP_KEY
ENV VITE_AMAP_SECURITY_CODE=$VITE_AMAP_SECURITY_CODE

RUN npm run build

FROM nginx:alpine
RUN apk add --no-cache gettext openssl

# 复制构建好的文件
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制nginx配置
COPY nginx.conf /etc/nginx/default.conf.template

ENV NGINX_SERVER_NAME=localhost
ENV NGINX_CERT_DOMAIN=localhost

# 暴露端口
EXPOSE 80 443

# Render only our deployment variables and keep nginx runtime variables intact.
# A short-lived self-signed cert keeps local Docker Compose usable when real
# Let's Encrypt files are not mounted.
CMD ["sh", "-c", "set -e; cert_dir=\"/etc/letsencrypt/live/$NGINX_CERT_DOMAIN\"; if [ ! -f \"$cert_dir/fullchain.pem\" ] || [ ! -f \"$cert_dir/privkey.pem\" ]; then mkdir -p \"$cert_dir\"; openssl req -x509 -nodes -newkey rsa:2048 -days 1 -subj \"/CN=$NGINX_CERT_DOMAIN\" -keyout \"$cert_dir/privkey.pem\" -out \"$cert_dir/fullchain.pem\" >/dev/null 2>&1; cp \"$cert_dir/fullchain.pem\" \"$cert_dir/chain.pem\"; fi; envsubst '$NGINX_SERVER_NAME $NGINX_CERT_DOMAIN' < /etc/nginx/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
