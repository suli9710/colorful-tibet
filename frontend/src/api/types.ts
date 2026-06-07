export interface AiRouteGenerateRequest {
  days: number
  budget: string
  preference: string
  locale?: string
}

export interface AiRouteGenerateResponse {
  content: string
  model: string
  budget: string
  preference: string
  days: number
  prompt: string
}

export interface AiRouteRecordResponse {
  id: number
  jobId?: string | null
  title: string
  content: string
  days: number
  budget: string
  preference: string
  locale?: string | null
  status: 'RUNNING' | 'COMPLETED' | 'FAILED'
  manuallySaved: boolean
  errorMessage?: string | null
  createdAt: string
  updatedAt: string
}

export interface HeritageItem {
  id: number
  name: string
  nameTibetan?: string
  description?: string
  descriptionTibetan?: string
  category?: string
  imageUrl?: string
  videoUrl?: string
  originStory?: string
  significance?: string
  baikeUrl?: string
  region?: string
  protectionLevel?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  createdAt?: string
}

export interface HeritageCommentItem {
  id: number
  content: string
  imageUrl?: string
  rating?: number
  userId: number
  username: string
  nickname?: string
  avatar?: string
  createdAt: string
}

export interface HeritageInheritorItem {
  id: number
  name: string
  nameTibetan?: string
  avatarUrl?: string
  level?: string
  bio?: string
  bioTibetan?: string
  story?: string
  region?: string
  heritageItemId: number
  createdAt?: string
}

export interface HeritageEventItem {
  id: number
  title: string
  titleTibetan?: string
  description?: string
  descriptionTibetan?: string
  eventDate?: string
  endDate?: string
  location?: string
  imageUrl?: string
  contactInfo?: string
  heritageItemId?: number
  createdAt?: string
}

export type SecurityPostureStatus = 'READY' | 'DEGRADED' | 'BLOCKED'
export type SecurityFindingStatus = 'PASS' | 'WARN' | 'FAIL' | 'INFO'
export type DependencyHealthStatus = 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN' | 'DISABLED'

export interface SecurityPostureResponse {
  status: SecurityPostureStatus
  score: number
  generatedAt: string
  environment: {
    activeProfiles: string[]
    strictSecretsRequired: boolean
  }
  exposure: {
    publicDocsEnabled: boolean
    publicMetricsEnabled: boolean
    actuatorHealthPublic: boolean
    actuatorInfoAdminOnly: boolean
    corsConfigured: boolean
    trustedProxyHeadersEnabled: boolean
  }
  authentication: {
    jwtConfigured: boolean
    jwtIssuerConfigured: boolean
    jwtAudienceConfigured: boolean
    jwtExpirationMs: number
    cookieSecure: boolean
    cookieSameSite: string
    csrfConfigured: boolean
    superAdminTotpConfigured: boolean
    tokenRevocationRedisEnabled: boolean
  }
  protections: {
    rateLimitEnabled: boolean
    rateLimitRedisEnabled: boolean
    bruteForceEnabled: boolean
    bruteForceRedisEnabled: boolean
    antibotEnabled: boolean
    recaptchaConfigured: boolean
  }
  dataProtection: {
    piiKeysConfigured: boolean
    piiActiveKeyConfigured: boolean
    legacyPiiKeyConfigured: boolean
    piiMigrationEnabled: boolean
  }
  dependencies: {
    database: DependencyHealthStatus
    redis: DependencyHealthStatus
    scrapling: DependencyHealthStatus
    aiProviderConfigured: boolean
    paymentCallbackSecretConfigured: boolean
  }
  findings: Array<{
    id: string
    severity: 'HIGH' | 'MEDIUM' | 'LOW'
    status: SecurityFindingStatus
    message: string
  }>
}

export type {
  components as OpenApiComponents,
  operations as OpenApiOperations,
  paths as OpenApiPaths
} from './generated/schema'
