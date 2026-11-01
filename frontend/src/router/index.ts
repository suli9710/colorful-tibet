import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import i18n from '../i18n'
import { createLoginRedirect } from './authRedirect'
import {
    ROUTE_TITLE_KEYS,
    installDocumentTitleSync,
    routeScrollBehavior
} from './routeMetadata'

const router = createRouter({
    history: createWebHistory(),
    scrollBehavior: routeScrollBehavior,
    routes: [
        {
            path: '/',
            component: () => import('../views/Home.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.home }
        },
        {
            path: '/login',
            component: () => import('../views/Login.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.login }
        },
        {
            path: '/register',
            component: () => import('../views/Register.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.register }
        },
        {
            path: '/spots',
            component: () => import('../views/ScenicSpots.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.scenicSpots }
        },
        {
            path: '/spots/:id',
            component: () => import('../views/ScenicSpotDetail.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.scenicSpotDetail }
        },
        {
            path: '/heritage',
            component: () => import('../views/Heritage.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.heritage }
        },
        {
            path: '/news',
            component: () => import('../views/News.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.news }
        },
        {
            path: '/admin',
            component: () => import('../views/AdminDashboard.vue'),
            meta: {
                requiresAuth: true,
                roles: ['ADMIN'],
                titleKey: ROUTE_TITLE_KEYS.admin
            }
        },
        {
            path: '/route-planner',
            name: 'route-planner',
            component: () => import('../views/RoutePlanner.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.routePlanner }
        },
        {
            path: '/route', // Alias for backward compatibility
            redirect: '/route-planner',
            meta: { titleKey: ROUTE_TITLE_KEYS.routePlanner }
        },
        {
            path: '/community',
            name: 'community',
            component: () => import('../views/RouteCommunity.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.community }
        },
        {
            path: '/community/question/:id',
            name: 'question-detail',
            component: () => import('../views/QuestionDetail.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.questionDetail }
        },
        {
            path: '/community/:id',
            name: 'route-detail',
            component: () => import('../views/RouteDetail.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.routeDetail }
        },
        {
            path: '/create-route',
            name: 'create-route',
            component: () => import('../views/CreateRoute.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.createRoute }
        },
        {
            path: '/profile',
            component: () => import('../views/UserProfile.vue'),
            meta: { requiresAuth: true, titleKey: ROUTE_TITLE_KEYS.profile }
        },
        {
            path: '/privacy',
            name: 'privacy',
            component: () => import('../views/PrivacyPolicy.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.privacy }
        },
        {
            path: '/terms',
            name: 'terms',
            component: () => import('../views/TermsOfService.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.terms }
        },
        {
            path: '/hotels',
            name: 'hotels',
            component: () => import('../views/HotelList.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.hotels }
        },
        {
            path: '/hotels/:id',
            name: 'hotel-detail',
            component: () => import('../views/HotelDetail.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.hotelDetail }
        },
        {
            path: '/hotel-booking/:id',
            name: 'hotel-booking',
            component: () => import('../views/HotelBooking.vue'),
            meta: { requiresAuth: true, titleKey: ROUTE_TITLE_KEYS.hotelBooking }
        },
        {
            path: '/favorites',
            name: 'favorites',
            component: () => import('../views/Favorites.vue'),
            meta: { requiresAuth: true, titleKey: ROUTE_TITLE_KEYS.favorites }
        },
        {
            path: '/orders',
            name: 'orders',
            component: () => import('../views/OrderCenter.vue'),
            meta: { requiresAuth: true, titleKey: ROUTE_TITLE_KEYS.orders }
        },
        {
            path: '/hotel-orders',
            name: 'hotel-orders',
            component: () => import('../views/HotelOrders.vue'),
            meta: { requiresAuth: true, titleKey: ROUTE_TITLE_KEYS.hotelOrders }
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'not-found',
            component: () => import('../views/NotFound.vue'),
            meta: { titleKey: ROUTE_TITLE_KEYS.notFound }
        }
    ]
})

router.beforeEach(async (to) => {
    const auth = useAuthStore()
    const requiredRoles = to.meta.roles ?? []
    const requiresFreshSession = requiredRoles.length > 0
    const requiresAuth = Boolean(to.meta.requiresAuth) || requiresFreshSession
    const isAuthenticated = requiresFreshSession
        ? await auth.refreshSession()
        : requiresAuth
          ? await auth.ensureSession()
          : auth.hasValidSession()

    if (requiresAuth && !isAuthenticated) {
        return createLoginRedirect(to.fullPath)
    }

    if (isAuthenticated && auth.user?.mustChangePassword && to.path !== '/profile') {
        return { path: '/profile', query: { changePassword: '1' } }
    }

    if (requiredRoles.includes('ADMIN') && !auth.isAdmin) {
        return '/'
    }

    return true
})

installDocumentTitleSync(router, {
    locale: i18n.global.locale,
    translate: key => i18n.global.t(key)
})

export default router
