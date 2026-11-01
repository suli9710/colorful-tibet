# 多阶段构建：先构建前端，再使用nginx服务
FROM node:22-alpine@sha256:16e22a550f3863206a3f701448c45f7912c6896a62de43add43bb9c86130c3e2 AS builder
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
ARG VITE_FRONTEND_ERROR_REPORT_URL=/api/client-errors
ARG VITE_APP_VERSION=unknown
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_RECAPTCHA_ENABLED=$VITE_RECAPTCHA_ENABLED
ENV VITE_RECAPTCHA_MODE=$VITE_RECAPTCHA_MODE
ENV VITE_RECAPTCHA_SITE_KEY=$VITE_RECAPTCHA_SITE_KEY
ENV VITE_AMAP_KEY=$VITE_AMAP_KEY
ENV VITE_AMAP_SECURITY_CODE=$VITE_AMAP_SECURITY_CODE
ENV VITE_FRONTEND_ERROR_REPORT_URL=$VITE_FRONTEND_ERROR_REPORT_URL
ENV VITE_APP_VERSION=$VITE_APP_VERSION

RUN npm run build

FROM nginx:alpine@sha256:4a73073bd557c65b759505da037898b61f1be6cbcc3c2c3aeac22d2a470c1752
RUN apk add --no-cache gettext \
    && apk upgrade --no-cache \
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
ENV NGINX_REAL_IP_FROM=
ENV NGINX_REAL_IP_HEADER=X-Forwarded-For

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
    "cidr_pattern='^([0-9]{1,3}[.]){3}[0-9]{1,3}(/[0-9]{1,2})?\$|^[0-9A-Fa-f:]+(/[0-9]{1,3})?\$'" \
    '' \
    'render_real_ip() {' \
    '    : > /etc/nginx/conf.d/real-ip.conf' \
    '    [ -n "${NGINX_REAL_IP_FROM:-}" ] || return 0' \
    '    real_ip_header_name="${NGINX_REAL_IP_HEADER:-X-Forwarded-For}"' \
    '    case "$real_ip_header_name" in' \
    '        X-Forwarded-For|X-Real-IP|CF-Connecting-IP|True-Client-IP|proxy_protocol) ;;' \
    '        *) fail "NGINX_REAL_IP_HEADER must be one of X-Forwarded-For, X-Real-IP, CF-Connecting-IP, True-Client-IP, proxy_protocol." ;;' \
    '    esac' \
    "    set -- \$(printf '%s' \"\$NGINX_REAL_IP_FROM\" | tr ',' ' ')" \
    '    [ "$#" -gt 0 ] || fail "NGINX_REAL_IP_FROM must contain at least one address or CIDR."' \
    '    for trusted_cidr do' \
    "        printf '%s' \"\$trusted_cidr\" | grep -Eq \"\$cidr_pattern\" || fail \"NGINX_REAL_IP_FROM entries must be IPv4/IPv6 addresses or CIDRs, comma or space separated.\"" \
    '        echo "set_real_ip_from $trusted_cidr;" >> /etc/nginx/conf.d/real-ip.conf' \
    '    done' \
    '    echo "real_ip_header $real_ip_header_name;" >> /etc/nginx/conf.d/real-ip.conf' \
    '    echo "real_ip_recursive on;" >> /etc/nginx/conf.d/real-ip.conf' \
    '}' \
    '' \
    'require_server_names "$NGINX_SERVER_NAME"' \
    'render_real_ip' \
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
    "exec nginx -g 'daemon off;'" \
    > /usr/local/bin/render-nginx-config \
    && chmod 755 /usr/local/bin/render-nginx-config

# Expose unprivileged ports; compose maps public 80/443 to these.
EXPOSE 8080 8443

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/health >/dev/null || exit 1

USER 101:101

# Render only our deployment variables and keep nginx runtime variables intact.
CMD ["/usr/local/bin/render-nginx-config"]
