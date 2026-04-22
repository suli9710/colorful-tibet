import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            component: () => import('../views/Home.vue')
        },
        {
            path: '/login',
            component: () => import('../views/Login.vue')
        },
        {
            path: '/register',
            component: () => import('../views/Register.vue')
        },
        {
            path: '/spots',
            component: () => import('../views/ScenicSpots.vue')
        },
        {
            path: '/spots/:id',
            component: () => import('../views/ScenicSpotDetail.vue')
        },
        {
            path: '/heritage',
            component: () => import('../views/Heritage.vue')
        },
        {
            path: '/news',
            component: () => import('../views/News.vue')
        },
        {
            path: '/admin',
            component: () => import('../views/AdminDashboard.vue')
        },
        {
            path: '/route-planner',
            name: 'route-planner',
            component: () => import('../views/RoutePlanner.vue')
        },
        {
            path: '/route', // Alias for backward compatibility
            redirect: '/route-planner'
        },
        {
            path: '/community',
            name: 'community',
            component: () => import('../views/RouteCommunity.vue')
        },
        {
            path: '/community/:id',
            name: 'route-detail',
            component: () => import('../views/RouteDetail.vue')
        },
        {
            path: '/create-route',
            name: 'create-route',
            component: () => import('../views/CreateRoute.vue')
        },
        {
            path: '/profile',
            component: () => import('../views/UserProfile.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/privacy',
            name: 'privacy',
            component: () => import('../views/PrivacyPolicy.vue')
        },
        {
            path: '/terms',
            name: 'terms',
            component: () => import('../views/TermsOfService.vue')
        },
        {
            path: '/hotels',
            name: 'hotels',
            component: () => import('../views/HotelList.vue')
        },
        {
            path: '/hotels/:id',
            name: 'hotel-detail',
            component: () => import('../views/HotelDetail.vue')
        },
        {
            path: '/hotel-booking/:id',
            name: 'hotel-booking',
            component: () => import('../views/HotelBooking.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/hotel-orders',
            name: 'hotel-orders',
            component: () => import('../views/HotelOrders.vue'),
            meta: { requiresAuth: true }
        }
    ]
})

router.beforeEach((to) => {
    const userStr = localStorage.getItem('user')
    const user = userStr ? JSON.parse(userStr) : null
    const isAuthenticated = !!user

    if (to.path.startsWith('/admin') && (!user || user.role !== 'ADMIN')) {
        return '/'
    }

    if (to.meta.requiresAuth && !isAuthenticated) {
        return '/login'
    }

    return true
})

export default router
