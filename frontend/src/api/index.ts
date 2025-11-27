import axios from 'axios'

const api = axios.create({
    baseURL: '/api',
    timeout: 120000, // 120秒超时，适应AI生成行程的时间需求
    headers: {
        'Content-Type': 'application/json'
    }
})

// Request interceptor for adding auth token
api.interceptors.request.use(config => {
    const userStr = localStorage.getItem('user')
    if (userStr) {
        const user = JSON.parse(userStr)
        if (user.token) {
            config.headers.Authorization = `Bearer ${user.token}`
        }
    }
    return config
})

export const endpoints = {
    auth: {
        login: '/auth/login',
        register: '/auth/register'
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
        spots: '/admin/spots',
        updateSpot: (id: number) => `/admin/spots/${id}`
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
