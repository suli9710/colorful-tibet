import axios from 'axios'

const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

if (import.meta.env.DEV) {
  console.log('🌐 API Base URL:', apiBaseURL)
}

const api = axios.create({
  baseURL: apiBaseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(config => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      if (user.token) {
        config.headers.Authorization = `Bearer ${user.token}`
      }
    } catch (e) {
      console.error('❌ [API Request] 解析用户信息失败:', e)
    }
  }

  const locale = localStorage.getItem('locale') || 'zh'
  if (config.method === 'get' || config.method === 'GET') {
    if (!config.params) {
      config.params = {}
    }
    config.params.locale = locale
  }

  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      const method = String(error.config?.method || '').toLowerCase()
      const requestUrl = String(error.config?.url || '')
      const isBookingCreate = method === 'post' && requestUrl.includes('/bookings')

      // 酒店下单接口 401 时不做全局强制跳转，交给业务页面自行处理提示
      if (!isBookingCreate) {
        const currentPath = window.location.pathname
        if (currentPath !== '/login') {
          localStorage.removeItem('user')
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  }
)

export interface HotelRoom {
  id: number
  name: string
  price: number
  desc: string
}

export interface HotelItem {
  id: number
  region: string
  name: string
  city: string
  address: string
  stars: number
  rating: number
  reviewCount: number
  priceMin: number
  available: boolean
  tags: string[]
  coverImage: string
  description: string
  amenities: string[]
  rooms: HotelRoom[]
}

export const hotelRegions = ['拉萨', '林芝', '日喀则', '阿里'] as const

export const hotels: HotelItem[] = [
  {
    id: 1,
    region: '拉萨',
    name: '拉萨瑞吉度假酒店',
    city: '拉萨',
    address: '城关区',
    stars: 5,
    rating: 4.9,
    reviewCount: 128,
    priceMin: 899,
    available: true,
    tags: ['高端', '景观', '早餐'],
    coverImage: 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=1200&q=80',
    description: '靠近市中心的高品质酒店，适合希望兼顾舒适与出行便利的游客。',
    amenities: ['免费Wi-Fi', '早餐', '停车场', '健身房'],
    rooms: [
      { id: 1, name: '观景大床房', price: 699, desc: '1张大床 · 可住2人 · 含窗景' },
      { id: 2, name: '豪华双床房', price: 799, desc: '2张单床 · 可住2人 · 含早餐' },
      { id: 3, name: '家庭套房', price: 1099, desc: '1大床+1沙发床 · 可住3-4人' },
    ],
  },
  {
    id: 2,
    region: '拉萨',
    name: '拉萨香格里拉大酒店',
    city: '拉萨',
    address: '堆龙德庆区',
    stars: 5,
    rating: 4.8,
    reviewCount: 96,
    priceMin: 828,
    available: true,
    tags: ['商务', '观景', '舒适'],
    coverImage: 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=1200&q=80',
    description: '适合商务与度假兼顾的高品质住宿，设施完善，视野开阔。',
    amenities: ['免费Wi-Fi', '早餐', '会议室', '接送服务'],
    rooms: [
      { id: 1, name: '高级大床房', price: 688, desc: '1张大床 · 可住2人 · 含早餐' },
      { id: 2, name: '景观双床房', price: 728, desc: '2张单床 · 可住2人 · 城市景观' },
      { id: 3, name: '行政套房', price: 1288, desc: '更大空间 · 含行政礼遇' },
    ],
  },
  {
    id: 3,
    region: '拉萨',
    name: '拉萨雪域明珠酒店',
    city: '拉萨',
    address: '城关区',
    stars: 4,
    rating: 4.7,
    reviewCount: 74,
    priceMin: 568,
    available: true,
    tags: ['亲子', '便捷', '舒适'],
    coverImage: 'https://images.unsplash.com/photo-1445019980597-93fa8acb246c?w=1200&q=80',
    description: '适合家庭出行和短途停留，周边交通便利，性价比较高。',
    amenities: ['免费Wi-Fi', '早餐', '停车场'],
    rooms: [
      { id: 1, name: '标准大床房', price: 488, desc: '1张大床 · 可住2人' },
      { id: 2, name: '标准双床房', price: 528, desc: '2张单床 · 可住2人' },
      { id: 3, name: '家庭亲子房', price: 688, desc: '适合家庭入住 · 空间更大' },
    ],
  },
  {
    id: 4,
    region: '拉萨',
    name: '拉萨措美朵精品酒店',
    city: '拉萨',
    address: '柳梧新区',
    stars: 4,
    rating: 4.6,
    reviewCount: 58,
    priceMin: 488,
    available: false,
    tags: ['精品', '静谧', '设计感'],
    coverImage: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&q=80',
    description: '设计感较强的精品酒店，适合追求安静和氛围感的旅行者。',
    amenities: ['免费Wi-Fi', '早餐', '洗衣服务'],
    rooms: [
      { id: 1, name: '精品大床房', price: 458, desc: '1张大床 · 可住2人' },
      { id: 2, name: '静谧双床房', price: 498, desc: '2张单床 · 可住2人' },
      { id: 3, name: '轻奢套房', price: 788, desc: '舒适空间 · 含独立会客区' },
    ],
  },
  {
    id: 5,
    region: '林芝',
    name: '林芝云栖山居',
    city: '林芝',
    address: '巴宜区',
    stars: 4,
    rating: 4.8,
    reviewCount: 86,
    priceMin: 599,
    available: true,
    tags: ['亲子', '自然', '民宿感'],
    coverImage: 'https://images.unsplash.com/photo-1445019980597-93fa8acb246c?w=1200&q=80',
    description: '面向雪山与森林景观，适合深度自然旅行和慢节奏度假。',
    amenities: ['免费Wi-Fi', '早餐', '观景台', '停车场'],
    rooms: [
      { id: 1, name: '山景大床房', price: 599, desc: '1张大床 · 可住2人 · 山景' },
      { id: 2, name: '森林双床房', price: 639, desc: '2张单床 · 可住2人 · 近森林' },
      { id: 3, name: '亲子套房', price: 888, desc: '适合家庭入住 · 含儿童空间' },
    ],
  },
  {
    id: 6,
    region: '林芝',
    name: '林芝桃花源酒店',
    city: '林芝',
    address: '工布江达县',
    stars: 5,
    rating: 4.9,
    reviewCount: 67,
    priceMin: 788,
    available: true,
    tags: ['高端', '度假', '景观'],
    coverImage: 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=1200&q=80',
    description: '以自然度假体验为主，适合赏景、休闲和拍照打卡。',
    amenities: ['免费Wi-Fi', '早餐', '泳池', '停车场'],
    rooms: [
      { id: 1, name: '景观大床房', price: 788, desc: '1张大床 · 可住2人' },
      { id: 2, name: '山谷双床房', price: 828, desc: '2张单床 · 可住2人' },
      { id: 3, name: '山谷套房', price: 1188, desc: '更大景观面 · 含早餐' },
    ],
  },
  {
    id: 7,
    region: '林芝',
    name: '林芝雅鲁藏布酒店',
    city: '林芝',
    address: '米林市',
    stars: 4,
    rating: 4.7,
    reviewCount: 44,
    priceMin: 528,
    available: true,
    tags: ['观景', '静谧', '舒适'],
    coverImage: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&q=80',
    description: '靠近景区通道，适合游览雅鲁藏布大峡谷线路。',
    amenities: ['免费Wi-Fi', '早餐', '停车场'],
    rooms: [
      { id: 1, name: '舒适大床房', price: 528, desc: '1张大床 · 可住2人' },
      { id: 2, name: '舒适双床房', price: 568, desc: '2张单床 · 可住2人' },
      { id: 3, name: '观景家庭房', price: 788, desc: '适合家庭入住 · 含观景窗' },
    ],
  },
  {
    id: 8,
    region: '林芝',
    name: '林芝松茸花园酒店',
    city: '林芝',
    address: '波密县',
    stars: 3,
    rating: 4.5,
    reviewCount: 38,
    priceMin: 398,
    available: false,
    tags: ['经济', '便捷', '花园'],
    coverImage: 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=1200&q=80',
    description: '适合预算型出行，提供稳定舒适的基础住宿体验。',
    amenities: ['免费Wi-Fi', '早餐', '花园'],
    rooms: [
      { id: 1, name: '标准大床房', price: 398, desc: '1张大床 · 可住2人' },
      { id: 2, name: '标准双床房', price: 428, desc: '2张单床 · 可住2人' },
      { id: 3, name: '家庭三人房', price: 568, desc: '适合三人同行' },
    ],
  },
  {
    id: 9,
    region: '日喀则',
    name: '日喀则扎什伦布客栈',
    city: '日喀则',
    address: '桑珠孜区',
    stars: 3,
    rating: 4.6,
    reviewCount: 54,
    priceMin: 268,
    available: false,
    tags: ['经济', '文化', '便捷'],
    coverImage: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&q=80',
    description: '轻量级住宿选择，适合预算友好型出行和短暂停留。',
    amenities: ['免费Wi-Fi', '早餐'],
    rooms: [
      { id: 1, name: '经济大床房', price: 268, desc: '1张大床 · 可住2人' },
      { id: 2, name: '经济双床房', price: 298, desc: '2张单床 · 可住2人' },
      { id: 3, name: '三人间', price: 358, desc: '适合多人同行' },
    ],
  },
  {
    id: 10,
    region: '日喀则',
    name: '日喀则珠峰国际酒店',
    city: '日喀则',
    address: '定日县',
    stars: 4,
    rating: 4.7,
    reviewCount: 62,
    priceMin: 468,
    available: true,
    tags: ['珠峰', '中转', '舒适'],
    coverImage: 'https://images.unsplash.com/photo-1445019980597-93fa8acb246c?w=1200&q=80',
    description: '适合珠峰线路中转与高原旅途休整，舒适度较高。',
    amenities: ['免费Wi-Fi', '早餐', '停车场'],
    rooms: [
      { id: 1, name: '舒适大床房', price: 468, desc: '1张大床 · 可住2人' },
      { id: 2, name: '舒适双床房', price: 498, desc: '2张单床 · 可住2人' },
      { id: 3, name: '家庭套房', price: 688, desc: '适合家庭入住' },
    ],
  },
  {
    id: 11,
    region: '日喀则',
    name: '日喀则吉隆边贸酒店',
    city: '日喀则',
    address: '吉隆县',
    stars: 4,
    rating: 4.6,
    reviewCount: 41,
    priceMin: 438,
    available: true,
    tags: ['边贸', '便捷', '静谧'],
    coverImage: 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=1200&q=80',
    description: '适合边贸通行与自驾补给的中档酒店。',
    amenities: ['免费Wi-Fi', '早餐', '停车场'],
    rooms: [
      { id: 1, name: '商务大床房', price: 438, desc: '1张大床 · 可住2人' },
      { id: 2, name: '商务双床房', price: 468, desc: '2张单床 · 可住2人' },
      { id: 3, name: '行政套房', price: 688, desc: '更大空间 · 适合商务住宿' },
    ],
  },
  {
    id: 12,
    region: '日喀则',
    name: '日喀则白朗精品酒店',
    city: '日喀则',
    address: '白朗县',
    stars: 3,
    rating: 4.5,
    reviewCount: 33,
    priceMin: 328,
    available: true,
    tags: ['精品', '平价', '清爽'],
    coverImage: 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=1200&q=80',
    description: '价格亲民，适合普通旅行和短期停留。',
    amenities: ['免费Wi-Fi', '早餐'],
    rooms: [
      { id: 1, name: '标准大床房', price: 328, desc: '1张大床 · 可住2人' },
      { id: 2, name: '标准双床房', price: 358, desc: '2张单床 · 可住2人' },
      { id: 3, name: '三人间', price: 428, desc: '适合三人同行' },
    ],
  },
  {
    id: 13,
    region: '阿里',
    name: '冈仁波齐云端酒店',
    city: '阿里',
    address: '普兰县',
    stars: 4,
    rating: 4.7,
    reviewCount: 39,
    priceMin: 499,
    available: true,
    tags: ['朝圣', '静谧', '观景'],
    coverImage: 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=1200&q=80',
    description: '远离喧嚣，适合高原深度游与朝圣线路中转。',
    amenities: ['免费Wi-Fi', '早餐', '停车场'],
    rooms: [
      { id: 1, name: '观景大床房', price: 499, desc: '1张大床 · 可住2人' },
      { id: 2, name: '观景双床房', price: 528, desc: '2张单床 · 可住2人' },
      { id: 3, name: '家庭套房', price: 758, desc: '适合家庭入住' },
    ],
  },
  {
    id: 14,
    region: '阿里',
    name: '阿里神山观景酒店',
    city: '阿里',
    address: '札达县',
    stars: 5,
    rating: 4.8,
    reviewCount: 27,
    priceMin: 888,
    available: true,
    tags: ['高端', '观景', '静谧'],
    coverImage: 'https://images.unsplash.com/photo-1445019980597-93fa8acb246c?w=1200&q=80',
    description: '面向神山风景，适合高端观景型住宿。',
    amenities: ['免费Wi-Fi', '早餐', '观景平台'],
    rooms: [
      { id: 1, name: '神山大床房', price: 888, desc: '1张大床 · 可住2人' },
      { id: 2, name: '神山双床房', price: 928, desc: '2张单床 · 可住2人' },
      { id: 3, name: '神山套房', price: 1388, desc: '更大观景面 · 含早餐' },
    ],
  },
  {
    id: 15,
    region: '阿里',
    name: '阿里玛旁雍措度假酒店',
    city: '阿里',
    address: '普兰县',
    stars: 4,
    rating: 4.7,
    reviewCount: 35,
    priceMin: 598,
    available: true,
    tags: ['湖景', '度假', '宁静'],
    coverImage: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1200&q=80',
    description: '湖边风景优美，适合摄影与慢旅行。',
    amenities: ['免费Wi-Fi', '早餐', '停车场'],
    rooms: [
      { id: 1, name: '湖景大床房', price: 598, desc: '1张大床 · 可住2人' },
      { id: 2, name: '湖景双床房', price: 638, desc: '2张单床 · 可住2人' },
      { id: 3, name: '湖景套房', price: 888, desc: '适合家庭与情侣入住' },
    ],
  },
  {
    id: 16,
    region: '阿里',
    name: '阿里古格王朝酒店',
    city: '阿里',
    address: '札达县',
    stars: 3,
    rating: 4.5,
    reviewCount: 24,
    priceMin: 388,
    available: false,
    tags: ['文化', '便捷', '平价'],
    coverImage: 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=1200&q=80',
    description: '适合探访古格王朝遗址的基础住宿。',
    amenities: ['免费Wi-Fi', '早餐'],
    rooms: [
      { id: 1, name: '标准大床房', price: 388, desc: '1张大床 · 可住2人' },
      { id: 2, name: '标准双床房', price: 418, desc: '2张单床 · 可住2人' },
      { id: 3, name: '三人间', price: 498, desc: '适合多人同行' },
    ],
  },
]

export const hotelsByRegion = hotelRegions.reduce<Record<string, HotelItem[]>>((acc, region) => {
  acc[region] = hotels.filter((hotel) => hotel.region === region)
  return acc
}, {})

export const getHotelById = (id: number) => hotels.find((hotel) => hotel.id === id)
export const getRoomById = (hotelId: number, roomId: number) => getHotelById(hotelId)?.rooms.find((room) => room.id === roomId)

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
    deleteUser: (id: number) => `/admin/users/${id}`,
    decryptPassword: (id: number) => `/admin/users/${id}/decrypt-password`,
    auditLogs: '/admin/audit-logs/list',
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
  hotelBookings: {
    create: '/hotel-bookings',
    my: '/hotel-bookings/my',
    all: '/hotel-bookings',
    updateStatus: (id: number) => `/hotel-bookings/${id}/status`,
    cancel: (id: number) => `/hotel-bookings/${id}`
  },
  comments: {
    list: (spotId: number) => `/comments/spot/${spotId}`,
    create: '/comments',
    uploadImage: '/comments/upload-image'
  }
}

export default api
