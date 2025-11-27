#!/bin/bash

# 快速部署脚本
# 使用方法: ./deploy.sh [backend|frontend|all]

set -e

PROJECT_DIR="/opt/colorful-tibet"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"
LOG_DIR="$PROJECT_DIR/logs"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装"
        exit 1
    fi
}

# 部署后端
deploy_backend() {
    log_info "开始部署后端..."
    
    cd $BACKEND_DIR
    
    # 检查是否有Git仓库
    if [ -d ".git" ]; then
        log_info "拉取最新代码..."
        git pull
    else
        log_warn "未检测到Git仓库，跳过代码更新"
    fi
    
    # 编译
    log_info "编译后端项目..."
    mvn clean package -DskipTests
    
    if [ ! -f "target/tourism-0.0.1-SNAPSHOT.jar" ]; then
        log_error "编译失败，JAR文件不存在"
        exit 1
    fi
    
    # 重启服务
    log_info "重启后端服务..."
    if systemctl is-active --quiet tibet-backend; then
        systemctl restart tibet-backend
        log_info "后端服务已重启"
    else
        log_warn "后端服务未运行，正在启动..."
        systemctl start tibet-backend
    fi
    
    # 等待服务启动
    sleep 5
    
    # 检查服务状态
    if systemctl is-active --quiet tibet-backend; then
        log_info "后端服务运行正常"
    else
        log_error "后端服务启动失败，请检查日志"
        journalctl -u tibet-backend -n 50
        exit 1
    fi
}

# 部署前端
deploy_frontend() {
    log_info "开始部署前端..."
    
    cd $FRONTEND_DIR
    
    # 检查是否有Git仓库
    if [ -d ".git" ]; then
        log_info "拉取最新代码..."
        git pull
    else
        log_warn "未检测到Git仓库，跳过代码更新"
    fi
    
    # 安装依赖
    log_info "安装前端依赖..."
    npm install
    
    # 构建
    log_info "构建前端项目..."
    npm run build
    
    if [ ! -d "dist" ]; then
        log_error "构建失败，dist目录不存在"
        exit 1
    fi
    
    # 重载Nginx
    log_info "重载Nginx配置..."
    if systemctl is-active --quiet nginx; then
        systemctl reload nginx
        log_info "Nginx已重载"
    else
        log_warn "Nginx未运行，正在启动..."
        systemctl start nginx
    fi
}

# 主函数
main() {
    log_info "========================================="
    log_info "  西藏旅游网站部署脚本"
    log_info "========================================="
    
    # 检查必要的命令
    check_command java
    check_command mvn
    check_command npm
    check_command nginx
    
    # 创建日志目录
    mkdir -p $LOG_DIR
    
    # 根据参数决定部署内容
    case "${1:-all}" in
        backend)
            deploy_backend
            ;;
        frontend)
            deploy_frontend
            ;;
        all)
            deploy_backend
            deploy_frontend
            ;;
        *)
            log_error "未知参数: $1"
            echo "使用方法: $0 [backend|frontend|all]"
            exit 1
            ;;
    esac
    
    log_info "========================================="
    log_info "部署完成！"
    log_info "========================================="
}

# 执行主函数
main "$@"






