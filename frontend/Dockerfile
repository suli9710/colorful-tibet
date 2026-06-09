# 多阶段构建：先构建前端，再使用nginx服务
FROM node:22-alpine@sha256:968df39aedcea65eeb078fb336ed7191baf48f972b4479711397108be0966920 AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

# 构建时传入的环境变量
ARG VITE_API_BASE_URL=/api
ARG VITE_RECAPTCHA_ENABLED=false
ARG VITE_RECAPTCHA_MODE=v3
ARG VITE_RECAPTCHA_SITE_KEY=
ARG VITE_AMAP_KEY=
ARG VITE_AMAP_SECURITY_CODE=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_RECAPTCHA_ENABLED=$VITE_RECAPTCHA_ENABLED
ENV VITE_RECAPTCHA_MODE=$VITE_RECAPTCHA_MODE
ENV VITE_RECAPTCHA_SITE_KEY=$VITE_RECAPTCHA_SITE_KEY
ENV VITE_AMAP_KEY=$VITE_AMAP_KEY
ENV VITE_AMAP_SECURITY_CODE=$VITE_AMAP_SECURITY_CODE

RUN npm run build

FROM nginx:alpine@sha256:8b1e78743a03dbb2c95171cc58639fef29abc8816598e27fb910ed2e621e589a
RUN apk add --no-cache gettext \
    && mkdir -p /etc/nginx/certs /var/cache/nginx /var/run /run \
    && chown -R 101:101 /etc/nginx/conf.d /etc/nginx/certs /var/cache/nginx /var/run /run

# 复制构建好的文件
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制nginx配置
COPY nginx.conf /etc/nginx/default.conf.template
COPY nginx.http.conf /etc/nginx/http.conf.template

ENV NGINX_SERVER_NAME=localhost
ENV NGINX_REDIRECT_HOST=localhost
ENV NGINX_CERT_DOMAIN=localhost
ENV NGINX_HTTP_ONLY=false

RUN printf '%s\n' \
    '#!/bin/sh' \
    'set -eu' \
    'set -f' \
    '' \
    'fail() {' \
    '    echo "$1" >&2' \
    '    exit 1' \
    '}' \
    '' \
    "host_label='[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?'" \
    'host_pattern="^${host_label}(\\.${host_label})*$"' \
    '' \
    'is_dns_host() {' \
    "    printf '%s' \"\$1\" | grep -Eq \"\$host_pattern\"" \
    '}' \
    '' \
    'require_single_host() {' \
    '    name="$1"' \
    '    value="$2"' \
    '    [ -n "$value" ] || fail "$name must be set."' \
    "    if printf '%s' \"\$value\" | grep -q '[[:space:]]'; then" \
    '        fail "$name must be a single DNS host without scheme, path, port, comma, whitespace, or control characters."' \
    '    fi' \
    '    is_dns_host "$value" || fail "$name must be a single DNS host without scheme, path, port, comma, whitespace, or control characters."' \
    '}' \
    '' \
    'require_server_names() {' \
    '    value="$1"' \
    "    if printf '%s' \"\$value\" | grep -q '[[:cntrl:]]'; then" \
    '        fail "NGINX_SERVER_NAME entries must be DNS hosts separated by spaces only."' \
    '    fi' \
    '    set -- $value' \
    '    [ "$#" -gt 0 ] || fail "NGINX_SERVER_NAME must be set."' \
    '    for host do' \
    '        is_dns_host "$host" || fail "NGINX_SERVER_NAME entries must be DNS hosts separated by spaces only."' \
    '    done' \
    '}' \
    '' \
    'require_server_names "$NGINX_SERVER_NAME"' \
    'if [ "$NGINX_HTTP_ONLY" = "true" ]; then' \
    "    envsubst '\$NGINX_SERVER_NAME \$NGINX_CERT_DOMAIN \$NGINX_REDIRECT_HOST' < /etc/nginx/http.conf.template > /etc/nginx/conf.d/default.conf" \
    'else' \
    '    require_single_host NGINX_REDIRECT_HOST "$NGINX_REDIRECT_HOST"' \
    '    require_single_host NGINX_CERT_DOMAIN "$NGINX_CERT_DOMAIN"' \
    '    cert_dir="/etc/nginx/certs/$NGINX_CERT_DOMAIN"' \
    '    for cert_file in fullchain.pem privkey.pem chain.pem; do' \
    '        if [ ! -f "$cert_dir/$cert_file" ]; then' \
    '            echo "Missing TLS certificate file: $cert_dir/$cert_file" >&2' \
    '            exit 1' \
    '        fi' \
    '    done' \
    "    envsubst '\$NGINX_SERVER_NAME \$NGINX_CERT_DOMAIN \$NGINX_REDIRECT_HOST' < /etc/nginx/default.conf.template > /etc/nginx/conf.d/default.conf" \
    'fi' \
    "nginx -g 'daemon off;'" \
    > /usr/local/bin/render-nginx-config \
    && chmod 755 /usr/local/bin/render-nginx-config

# Expose unprivileged ports; compose maps public 80/443 to these.
EXPOSE 8080 8443

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/health >/dev/null || exit 1

USER 101:101

# Render only our deployment variables and keep nginx runtime variables intact.
CMD ["/usr/local/bin/render-nginx-config"]
