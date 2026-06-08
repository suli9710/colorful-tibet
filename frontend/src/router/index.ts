import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { createLoginRedirect } from './authRedirect'

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
            path: '/community/question/:id',
            name: 'question-detail',
            component: () => import('../views/QuestionDetail.vue')
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
            path: '/favorites',
            name: 'favorites',
            component: () => import('../views/Favorites.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/orders',
            name: 'orders',
            component: () => import('../views/OrderCenter.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/hotel-orders',
            name: 'hotel-orders',
            component: () => import('../views/HotelOrders.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'not-found',
            component: () => import('../views/NotFound.vue')
        }
    ]
})

router.beforeEach(async (to) => {
    const auth = useAuthStore()
    const requiresAuth = Boolean(to.meta.requiresAuth) || to.path.startsWith('/admin')
    const isAdminRoute = to.path.startsWith('/admin')
    const isAuthenticated = isAdminRoute
        ? await auth.refreshSession()
        : requiresAuth
          ? await auth.ensureSession()
          : auth.hasValidSession()

    if (isAdminRoute && !isAuthenticated) {
        return createLoginRedirect(to.fullPath)
    }

    if (isAuthenticated && auth.user?.mustChangePassword && to.path !== '/profile') {
        return { path: '/profile', query: { changePassword: '1' } }
    }

    if (isAdminRoute && !auth.isAdmin) {
        return '/'
    }

    if (to.meta.requiresAuth && !isAuthenticated) {
        return createLoginRedirect(to.fullPath)
    }

    return true
})

export default router
