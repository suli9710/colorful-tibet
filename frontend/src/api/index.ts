import axios from 'axios'

// 支持环境变量配置API地址，方便部署到不同环境
// 
// 使用方式：
// 1. 本地开发（默认）：使用 /api（通过 Vite proxy 转发到 http://localhost:8080）
// 2. 远程部署/FRP：设置环境变量 VITE_API_BASE_URL=http://1.15.29.168:6000/api
//    或者在启动时设置：VITE_API_BASE_URL=http://1.15.29.168:6000/api npm run dev
// 3. 生产环境：通过环境变量 VITE_API_BASE_URL 配置（如：https://your-backend.com/api）
//
// 优先级：环境变量 > 默认值（/api）
const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

// 在控制台输出当前使用的API地址（仅开发环境）
if (import.meta.env.DEV) {
    console.log('🌐 API Base URL:', apiBaseURL)
}

const api = axios.create({
    baseURL: apiBaseURL,
    timeout: 120000, // 120秒超时，适应AI生成行程的时间需求
    headers: {
        'Content-Type': 'application/json'
    }
})

// Request interceptor for adding auth token and locale
api.interceptors.request.use(config => {
    const userStr = localStorage.getItem('user')
    if (userStr) {
        try {
            const user = JSON.parse(userStr)
            if (user.token) {
                config.headers.Authorization = `Bearer ${user.token}`
                
                // 调试日志：仅在开发环境或管理API请求时输出
                if (import.meta.env.DEV || config.url?.includes('/admin/')) {
                    console.log('🔐 [API Request] 添加Authorization头:', {
                        url: config.url,
                        hasToken: !!user.token,
                        tokenPrefix: user.token?.substring(0, 20) + '...',
                        username: user.username,
                        role: user.role
                    })
                }
            } else {
                console.warn('⚠️ [API Request] 用户信息存在但token缺失:', config.url)
            }
        } catch (e) {
            console.error('❌ [API Request] 解析用户信息失败:', e)
        }
    } else {
        // 对于需要认证的API，如果没有用户信息，记录警告
        if (config.url?.includes('/admin/') || config.url?.includes('/auth/me')) {
            console.warn('⚠️ [API Request] 未找到用户信息，请求可能失败:', config.url)
        }
    }
    
    // 添加语言参数到GET请求
    const locale = localStorage.getItem('locale') || 'zh'
    if (config.method === 'get' || config.method === 'GET') {
        if (!config.params) {
            config.params = {}
        }
        config.params.locale = locale
    }
    
    // 如果是FormData，不设置Content-Type，让浏览器自动设置（包含boundary）
    if (config.data instanceof FormData) {
        delete config.headers['Content-Type']
    }
    return config
})

// Response interceptor for handling 401 errors
api.interceptors.response.use(
    response => response,
    error => {
        if (error.response && error.response.status === 401) {
            // 检查当前路径
            const currentPath = window.location.pathname
            
            // 在管理页面，不要立即清除用户信息，让具体的错误处理函数来处理
            // 这样可以区分是真正的token过期还是其他原因（如权限问题）
            if (currentPath.startsWith('/admin')) {
                // 只记录警告，不立即清除用户信息
                // 让具体的API调用错误处理函数来决定如何处理
                console.warn('⚠️ [API Response] 401错误在管理页面，可能是token过期或权限问题')
            }
            // 在个人中心页面，如果401，说明token无效，清除用户信息并跳转
            else if (currentPath === '/profile') {
                localStorage.removeItem('user')
                window.location.href = '/login'
            }
            // 其他页面，自动跳转登录
            else if (currentPath !== '/login') {
                localStorage.removeItem('user')
                window.location.href = '/login'
            }
        }
        return Promise.reject(error)
    }
)

export const endpoints = {
    auth: {
        login: '/auth/login',
        register: '/auth/register',
        me: '/auth/me',
        meStats: '/auth/me/stats'
    },
    spots: {
        list: '/spots',
        detail: (id: number) => `/spots/${id}`,
        search: '/spots/search',
        recommendations: '/spots/recommendations',
        recommendationsDebug: '/spots/recommendations/debug'
    },
    news: {
        list: '/news'
    },
    heritage: {
        list: '/heritage'
    },
    admin: {
        stats: '/admin/stats',
        users: '/admin/users',
        updateRole: (id: number) => `/admin/users/${id}/role`,
        decryptPassword: (id: number) => `/admin/users/${id}/decrypt-password`,
        spots: '/admin/spots',
        updateSpot: (id: number) => `/admin/spots/${id}`,
        news: '/admin/news',
        createNews: '/admin/news',
        updateNews: (id: number) => `/admin/news/${id}`,
        deleteNews: (id: number) => `/admin/news/${id}`
    },
    bookings: {
        create: '/bookings',
        my: '/bookings/my',
        cancel: (id: number) => `/bookings/${id}/cancel`
    },
    comments: {
        list: (spotId: number) => `/comments/spot/${spotId}`,
        create: '/comments',
        uploadImage: '/comments/upload-image'
    }
}

export default api
