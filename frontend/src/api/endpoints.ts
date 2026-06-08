import { authEndpoints } from './modules/auth'
import { adminHeritageEndpoints, heritageEndpoints } from './modules/heritage'

export const endpoints = {
  auth: authEndpoints,
  guide: {
    chat: '/guide/chat'
  },
  routes: {
    generate: '/routes/generate',
    generateStream: '/routes/generate/stream',
    generateJob: '/routes/generate/jobs',
    generateJobDetail: (jobId: string) => `/routes/generate/jobs/${encodeURIComponent(jobId)}`,
    generateJobStream: (jobId: string) => `/routes/generate/jobs/${encodeURIComponent(jobId)}/stream`,
    share: '/routes/share',
    shared: '/routes/shared',
    sharedDetail: (id: number) => `/routes/shared/${id}`,
    sharedLike: (id: number) => `/routes/shared/${id}/like`,
    sharedLikeStatus: (id: number) => `/routes/shared/${id}/like-status`,
    sharedComments: (id: number) => `/routes/shared/${id}/comments`,
    deleteSharedComment: (routeId: number, commentId: number) => `/routes/shared/${routeId}/comments/${commentId}`,
    myRoutes: '/routes/my-routes',
    aiLatest: '/routes/ai/latest',
    aiSaved: '/routes/ai/saved',
    saveAiRoute: (id: number) => `/routes/ai/${id}/save`
  },
  community: {
    questions: '/community/questions',
    questionDetail: (id: number | string) => `/community/questions/${id}`,
    questionAnswers: (id: number | string) => `/community/questions/${id}/answers`,
    questionLike: (id: number | string) => `/community/questions/${id}/like`,
    questionLikeStatus: (id: number | string) => `/community/questions/${id}/like-status`,
    createQuestionAnswer: (id: number | string) => `/community/questions/${id}/answers`,
    acceptQuestionAnswer: (questionId: number | string, answerId: number | string) =>
      `/community/questions/${questionId}/answers/${answerId}/accept`,
    deleteQuestion: (id: number | string) => `/community/questions/${id}`
  },
  itineraries: {
    generate: '/itineraries/generate',
    my: '/itineraries/my',
    detail: (id: number) => `/itineraries/${id}`,
    quote: (id: number) => `/itineraries/${id}/quote`,
    createVersion: (id: number) => `/itineraries/${id}/versions`
  },
  orders: {
    create: '/orders',
    my: '/orders/my',
    detail: (id: number) => `/orders/${id}`,
    cancel: (id: number) => `/orders/${id}/cancel`,
    delete: (id: number) => `/orders/${id}`
  },
  tibetSpecialty: {
    travelKit: (itineraryId: number) => `/tibet-specialty/itineraries/${itineraryId}/travel-kit`,
    highlandAssessment: '/tibet-specialty/highland-assessment',
    cultureTips: '/tibet-specialty/culture-tips',
    phrasebook: '/tibet-specialty/phrasebook',
    sustainableOptions: '/tibet-specialty/sustainable-options'
  },
  spots: {
    list: '/spots',
    heatmap: '/spots/heatmap',
    detail: (id: number) => `/spots/${id}`,
    search: '/spots/search',
    recommendations: '/spots/recommendations',
    recommendationsMe: '/spots/recommendations/me',
    recommendationsDebug: '/spots/recommendations/debug'
  },
  news: {
    list: '/news'
  },
  heritage: heritageEndpoints,
  adminHeritage: adminHeritageEndpoints,
  admin: {
    stats: '/admin/stats',
    securityPosture: '/admin/security-posture',
    users: '/admin/users',
    updateRole: (id: number) => `/admin/users/${id}/role`,
    deleteUser: (id: number) => `/admin/users/${id}`,
    unlockUser: (id: number) => `/admin/users/${id}/unlock`,
    spots: '/admin/spots',
    updateSpot: (id: number) => `/admin/spots/${id}`,
    news: '/admin/news',
    createNews: '/admin/news',
    updateNews: (id: number) => `/admin/news/${id}`,
    deleteNews: (id: number) => `/admin/news/${id}`,
    uploadImage: '/admin/upload-image'
  },
  carousels: {
    list: '/carousels',
    adminList: '/admin/carousels',
    adminCreate: '/admin/carousels',
    adminUpdate: (id: number) => `/admin/carousels/${id}`,
    adminDelete: (id: number) => `/admin/carousels/${id}`
  },
  prices: {
    fetch: (spotId: number) => `/prices/fetch/${spotId}`,
    update: (spotId: number) => `/prices/update/${spotId}`,
    batchUpdate: '/prices/batch-update',
    batchUpdateJob: '/prices/batch-update/jobs',
    batchUpdateJobStatus: (jobId: string) => `/prices/batch-update/jobs/${jobId}`
  },
  favorites: {
    list: '/favorites',
    add: (routeId: number) => `/favorites/${routeId}`,
    remove: (routeId: number) => `/favorites/${routeId}`,
    status: (routeId: number) => `/favorites/${routeId}/status`
  },
  bookings: {
    create: '/bookings',
    my: '/bookings/my',
    cancel: (id: number) => `/bookings/${id}/cancel`,
    delete: (id: number) => `/bookings/${id}`
  },
  hotels: {
    list: '/hotel-bookings/hotels',
    detail: (id: number) => `/hotel-bookings/hotels/${id}`,
    roomTypes: (hotelId: number) => `/hotel-bookings/room-types/${hotelId}`
  },
  hotelBookings: {
    create: '/hotel-bookings',
    my: '/hotel-bookings/my',
    all: '/hotel-bookings',
    roomTypes: (hotelId: number) => `/hotel-bookings/room-types/${hotelId}`,
    updateStatus: (id: number) => `/hotel-bookings/${id}/status`,
    cancel: (id: number) => `/hotel-bookings/${id}`,
    delete: (id: number) => `/hotel-bookings/${id}/permanent`
  },
  adminRoutes: {
    list: '/admin/routes',
    create: '/admin/routes',
    update: (id: number) => `/admin/routes/${id}`,
    delete: (id: number) => `/admin/routes/${id}`
  },
  adminCommunity: {
    routes: '/admin/community/routes',
    updateRoute: (id: number) => `/admin/community/routes/${id}`,
    deleteRoute: (id: number) => `/admin/community/routes/${id}`,
    questions: '/admin/community/questions',
    updateQuestion: (id: number) => `/admin/community/questions/${id}`,
    deleteQuestion: (id: number) => `/admin/community/questions/${id}`,
    comments: '/admin/community/comments',
    updateComment: (id: number) => `/admin/community/comments/${id}`,
    deleteComment: (id: number) => `/admin/community/comments/${id}`,
    spotComments: '/admin/community/spot-comments',
    updateSpotComment: (id: number) => `/admin/community/spot-comments/${id}`,
    deleteSpotComment: (id: number) => `/admin/community/spot-comments/${id}`,
    answers: '/admin/community/answers',
    updateAnswer: (id: number) => `/admin/community/answers/${id}`,
    deleteAnswer: (id: number) => `/admin/community/answers/${id}`
  },
  adminHotels: {
    list: '/admin/hotels',
    create: '/admin/hotels',
    update: (id: number) => `/admin/hotels/${id}`,
    delete: (id: number) => `/admin/hotels/${id}`,
    roomTypes: (hotelId: number) => `/admin/hotels/${hotelId}/room-types`,
    updateRoomType: (id: number) => `/admin/room-types/${id}`,
    deleteRoomType: (id: number) => `/admin/room-types/${id}`
  },
  comments: {
    list: (spotId: number) => `/comments/spot/${spotId}`,
    create: '/comments',
    delete: (id: number) => `/comments/${id}`,
    uploadImage: '/comments/upload-image'
  }
}
