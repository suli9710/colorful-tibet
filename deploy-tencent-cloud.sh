#!/bin/bash

# 腾讯云服务器快速部署脚本
# 使用方法: ./deploy-tencent-cloud.sh

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装"
        return 1
    fi
    return 0
}

# 检查是否为 root 用户
check_root() {
    if [ "$EUID" -ne 0 ]; then 
        log_error "请使用 root 用户运行此脚本"
        exit 1
    fi
}

# 安装 Docker
install_docker() {
    log_step "检查 Docker 安装..."
    if check_command docker; then
        log_info "Docker 已安装"
        return 0
    fi
    
    log_info "开始安装 Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    systemctl start docker
    systemctl enable docker
    
    # 如果当前用户不是 root，将用户添加到 docker 组
    if [ "$EUID" -ne 0 ]; then
        log_info "将当前用户添加到 docker 组..."
        usermod -aG docker $USER
        log_warn "已添加用户到 docker 组，请重新登录或运行 'newgrp docker' 使配置生效"
    fi
    
    rm get-docker.sh
    log_info "Docker 安装完成"
}

# 安装 Docker Compose
install_docker_compose() {
    log_step "检查 Docker Compose 安装..."
    if check_command docker-compose; then
        log_info "Docker Compose 已安装"
        return 0
    fi
    
    log_info "开始安装 Docker Compose..."
    DOCKER_COMPOSE_VERSION="v2.20.0"
    curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    log_info "Docker Compose 安装完成"
}

# 配置防火墙
configure_firewall() {
    log_step "配置防火墙..."
    
    if command -v ufw &> /dev/null; then
        log_info "配置 UFW 防火墙规则..."
        ufw allow 22/tcp
        ufw allow 80/tcp
        ufw allow 443/tcp
        ufw allow 8080/tcp
        ufw --force enable
        log_info "防火墙配置完成"
    else
        log_warn "未检测到 UFW，请手动配置防火墙规则"
    fi
}

# 创建必要的目录
create_directories() {
    log_step "创建必要的目录..."
    mkdir -p /opt/colorful-tibet/data
    mkdir -p /opt/colorful-tibet/logs
    log_info "目录创建完成"
}

# 配置环境变量
configure_env() {
    log_step "配置环境变量..."
    
    if [ ! -f "/opt/colorful-tibet/.env" ]; then
        log_info "创建 .env 文件..."
        cat > /opt/colorful-tibet/.env << EOF
# 豆包 API 配置
DOUBAO_API_KEY=your-doubao-api-key
DOUBAO_API_URL=https://ark.cn-beijing.volces.com/api/v3/chat/completions
DOUBAO_MODEL=your-model-name

# JWT 密钥（生产环境请使用强随机字符串，至少32字符）
JWT_SECRET=$(openssl rand -hex 32)

# 管理员加密密钥
ADMIN_ENCRYPTION_KEY=$(openssl rand -hex 32)
EOF
        log_warn ".env 文件已创建，请编辑 /opt/colorful-tibet/.env 填入正确的配置"
        log_warn "特别是 DOUBAO_API_KEY、DOUBAO_API_URL 和 DOUBAO_MODEL"
    else
        log_info ".env 文件已存在，跳过创建"
    fi
}

# 获取服务器 IP
get_server_ip() {
    # 尝试多种方法获取公网 IP
    SERVER_IP=$(curl -s ifconfig.me || curl -s ipinfo.io/ip || curl -s icanhazip.com || echo "localhost")
    echo "$SERVER_IP"
}

# 更新 docker-compose.yml 中的 API 地址
update_docker_compose() {
    log_step "更新 docker-compose.yml 配置..."
    
    SERVER_IP=$(get_server_ip)
    log_info "检测到服务器 IP: $SERVER_IP"
    
    read -p "请输入前端访问后端的 API 地址 [默认: http://${SERVER_IP}:8080/api]: " API_URL
    API_URL=${API_URL:-"http://${SERVER_IP}:8080/api"}
    
    # 更新 docker-compose.yml
    if [ -f "/opt/colorful-tibet/docker-compose.yml" ]; then
        sed -i "s|VITE_API_BASE_URL=.*|VITE_API_BASE_URL=${API_URL}|g" /opt/colorful-tibet/docker-compose.yml
        log_info "docker-compose.yml 已更新"
    else
        log_error "docker-compose.yml 文件不存在"
        return 1
    fi
}

# 构建和启动服务
build_and_start() {
    log_step "构建 Docker 镜像..."
    cd /opt/colorful-tibet
    docker-compose build
    
    log_step "启动服务..."
    docker-compose up -d
    
    log_info "等待服务启动..."
    sleep 10
    
    # 检查服务状态
    if docker-compose ps | grep -q "Up"; then
        log_info "服务启动成功！"
    else
        log_error "服务启动失败，请检查日志: docker-compose logs"
        return 1
    fi
}

# 显示部署信息
show_deployment_info() {
    SERVER_IP=$(get_server_ip)
    
    echo ""
    log_info "========================================="
    log_info "  部署完成！"
    log_info "========================================="
    echo ""
    log_info "前端访问地址: http://${SERVER_IP}"
    log_info "后端 API 地址: http://${SERVER_IP}:8080/api"
    echo ""
    log_info "常用命令:"
    echo "  查看日志: docker-compose logs -f"
    echo "  停止服务: docker-compose down"
    echo "  重启服务: docker-compose restart"
    echo "  查看状态: docker-compose ps"
    echo ""
    log_warn "请确保:"
    log_warn "1. 已编辑 /opt/colorful-tibet/.env 文件，填入正确的 API 配置"
    log_warn "2. 腾讯云安全组已开放 80、443、8080 端口"
    log_warn "3. 如需使用域名，请配置域名解析和 SSL 证书"
    echo ""
}

# 主函数
main() {
    log_info "========================================="
    log_info "  西藏旅游网站 - 腾讯云部署脚本"
    log_info "========================================="
    echo ""
    
    # 检查 root 权限
    check_root
    
    # 检查项目目录
    if [ ! -d "/opt/colorful-tibet" ]; then
        log_error "项目目录 /opt/colorful-tibet 不存在"
        log_info "请先将项目代码上传到 /opt/colorful-tibet 目录"
        log_info "可以使用 git clone 或 scp 上传"
        exit 1
    fi
    
    # 安装 Docker
    install_docker
    
    # 安装 Docker Compose
    install_docker_compose
    
    # 配置防火墙
    configure_firewall
    
    # 创建目录
    create_directories
    
    # 配置环境变量
    configure_env
    
    # 更新 docker-compose.yml
    update_docker_compose
    
    # 构建和启动
    read -p "是否现在构建并启动服务? [y/N]: " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        build_and_start
        show_deployment_info
    else
        log_info "已跳过构建和启动步骤"
        log_info "您可以稍后运行以下命令手动启动:"
        log_info "  cd /opt/colorful-tibet"
        log_info "  docker-compose up -d"
    fi
}

# 执行主函数
main "$@"

