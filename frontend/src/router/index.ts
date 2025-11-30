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
        }
    ]
})

router.beforeEach((to, from, next) => {
    const userStr = localStorage.getItem('user')
    const user = userStr ? JSON.parse(userStr) : null
    const isAuthenticated = !!user

    if (to.path.startsWith('/admin') && (!user || user.role !== 'ADMIN')) {
        next('/')
        return
    }

    if (to.meta.requiresAuth && !isAuthenticated) {
        next('/login')
    } else {
        next()
    }
})

export default router
