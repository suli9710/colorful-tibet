import axios from 'axios'
import api, { endpoints } from '../api'
import { readBrowserStorage } from '../utils/browserStorage'
import { getRecaptchaToken, isRecaptchaV3Enabled } from '../utils/recaptcha'
import { summarizeClientError } from '../utils/errorMonitoring'

export interface ChatMessage {
  id: number
  role: 'guide' | 'user'
  text: string
  action?: 'navigate' | 'generate'
  actionLabel?: string
  pending?: boolean
  fallback?: boolean
  networkFallback?: boolean
  retryText?: string
  limited?: boolean
  retryAfterSeconds?: number
  challengeRequired?: boolean
}

export interface GuideChatHistoryItem {
  role: 'guide' | 'user'
  content: string
}

interface GuideChatApiResponse {
  content?: string
  model?: string
  fallback?: boolean
  action?: string
  actionLabel?: string
  limited?: boolean
  retryAfterSeconds?: number
  challengeRequired?: boolean
}

interface IntentRule {
  patterns: RegExp[]
  response: string
  action?: 'navigate' | 'generate'
  actionLabel?: string
}

export const MAX_GUIDE_CHAT_MESSAGE_CHARS = 280
const MAX_GUIDE_CHAT_RESPONSE_CHARS = 1200

const CONTROL_CHAR_PATTERN = /[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g
const ZERO_WIDTH_PATTERN = /[\u200B-\u200D\uFEFF]/g
const TECHNICAL_OUTPUT_PATTERN = /(?:sk-[A-Za-z0-9_-]{10,}|x-recaptcha-token|recaptcha|authorization\s*[:=]|bearer\s+[A-Za-z0-9._-]{8,}|api[-_\s]*key\s*[:=]|(?:access|refresh)[-_\s]*token\s*[:=]|(?:provider|model)\s*[:=]|stack\s*trace|(?:axios|runtime|nullpointer|illegalargument)?exception|org\.springframework|com\.tibet\.tourism|java\.|at\s+[\w.$]+\([^)]*\))/i
const GUIDE_CHAT_SAFE_FALLBACK_MESSAGE = '小导游暂时无法给出云端回答，先用本地安全建议兜底。你可以继续问西藏路线、景点、季节或高原适应相关问题。'
const GUIDE_CHAT_RATE_LIMIT_MESSAGE = '小导游需要稍微休息一下，避免连续请求过多。请等一会儿再继续问西藏路线、景点或高原适应问题。'
const GUIDE_CHAT_CHALLENGE_MESSAGE = '为了保护服务，小导游需要先完成一次安全校验。请稍后重试，或换个更具体的西藏旅行问题。'

interface SafeGuideChatContent {
  content: string
  sanitized: boolean
}

export function normalizeGuideChatInput(text: string): string {
  const normalized = text
    .replace(CONTROL_CHAR_PATTERN, ' ')
    .replace(ZERO_WIDTH_PATTERN, '')
    .replace(/\s+/g, ' ')
    .trim()
  const chars = Array.from(normalized)
  return chars.length > MAX_GUIDE_CHAT_MESSAGE_CHARS
    ? chars.slice(0, MAX_GUIDE_CHAT_MESSAGE_CHARS).join('')
    : normalized
}

function normalizeGuideChatOutput(text: string): string {
  const normalized = text
    .replace(CONTROL_CHAR_PATTERN, ' ')
    .replace(ZERO_WIDTH_PATTERN, '')
    .replace(/\s+/g, ' ')
    .trim()
  const chars = Array.from(normalized)
  return chars.length > MAX_GUIDE_CHAT_RESPONSE_CHARS
    ? `${chars.slice(0, MAX_GUIDE_CHAT_RESPONSE_CHARS).join('')}...`
    : normalized
}

function safeGuideChatContent(value: unknown, fallback = GUIDE_CHAT_SAFE_FALLBACK_MESSAGE): SafeGuideChatContent {
  if (typeof value !== 'string') {
    return { content: '', sanitized: false }
  }

  const content = normalizeGuideChatOutput(value)
  if (!content) {
    return { content: '', sanitized: false }
  }

  if (TECHNICAL_OUTPUT_PATTERN.test(content)) {
    return { content: fallback, sanitized: true }
  }

  return { content, sanitized: false }
}

export function sanitizeGuideChatResponseContent(value: unknown, fallback = GUIDE_CHAT_SAFE_FALLBACK_MESSAGE): string {
  return safeGuideChatContent(value, fallback).content
}

const OFF_TOPIC_RESPONSE = '扎西德勒，我只聊西藏旅行相关内容。我们把话题拉回藏地吧：你想了解拉萨初访、林芝风光、珠峰线路，还是高原适应？'

const greetingPattern = /^(你好|您好|嗨|hi|hello|扎西德勒|你是谁|你能做什么|帮助|help).{0,40}$/i

const tibetTravelPattern = /(西藏|藏地|藏区|拉萨|林芝|日喀则|山南|阿里|那曲|昌都|布达拉|大昭寺|八廓|色拉寺|哲蚌寺|扎什伦布|羊卓雍措|羊湖|纳木错|巴松措|珠峰|冈仁波齐|玛旁雍错|雅鲁藏布|南迦巴瓦|然乌湖|古格|高原|海拔|高反|氧气|进藏|川藏|青藏|滇藏|新藏|边防证|旅行|旅游|路线|行程|攻略|景点|寺庙|雪山|湖泊|美食|藏餐|酥油茶|牦牛|住宿|酒店|交通|预算|季节|天气|拍照|摄影|自驾|火车|飞机|礼仪|习俗|转经|经幡|tibet|lhasa|shigatse|nyingchi|everest)/i

const routeAction = {
  action: 'navigate' as const,
  actionLabel: '去规划路线'
}

const intentRules: IntentRule[] = [
  {
    patterns: [/你好/, /您好/, /嗨/, /hello/i, /hi/i, /你是谁/, /你能做什么/],
    response: '扎西德勒，我是西藏小导游，只回答西藏旅行相关问题。你可以问进藏路线、景点季节、高原适应、寺院礼仪、藏餐住宿或交通预算。'
  },
  {
    patterns: [/规划路线/, /路线/, /行程/, /帮我规划/, /怎么玩/, /安排/, /几天/],
    response: '扎西德勒。第一次进藏建议先在拉萨适应1到2天，再根据天数接林芝、日喀则、羊卓雍措或珠峰方向。告诉我出发月份、天数和预算，我可以继续拆成更稳的路线。',
    ...routeAction
  },
  {
    patterns: [/景点/, /推荐/, /哪里好玩/, /必去/, /有什么地方/],
    response: '人文可以看布达拉宫、大昭寺、八廓街和扎什伦布寺；自然风光可选羊卓雍措、纳木错、巴松措、南迦巴瓦或珠峰。高海拔景点别排得太密。'
  },
  {
    patterns: [/自然/, /风光/, /风景/, /徒步/, /雪山/, /湖泊/],
    response: '偏自然风光可优先考虑羊卓雍措、纳木错、巴松措、南迦巴瓦峰和雅鲁藏布大峡谷。行程上要控制海拔爬升，给身体留适应时间。'
  },
  {
    patterns: [/人文/, /古迹/, /寺庙/, /历史/, /文化/],
    response: '人文路线建议从拉萨开始：布达拉宫、大昭寺、八廓街和色拉寺适合放在前两天。之后再去日喀则看扎什伦布寺，节奏比直接冲高海拔更稳。'
  },
  {
    patterns: [/什么时候去/, /季节/, /几月/, /最佳时间/, /天气/],
    response: '5到10月是多数游客进藏的舒适季节，天气相对稳定。3到4月适合看林芝桃花，冬季人少但高海拔路段要提前核实路况和开放信息。'
  },
  {
    patterns: [/高原反应/, /海拔/, /氧气/, /高反/, /身体/, /适应/],
    response: '高原反应要认真对待。抵达拉萨后先慢下来，少运动、多喝温水、避免饮酒；若头痛、胸闷或呼吸困难明显加重，应及时就医或下降海拔。'
  },
  {
    patterns: [/预算/, /钱/, /费用/, /贵/, /便宜/, /省钱/],
    response: '预算主要看季节、住宿和交通。经济型可选青旅或经济酒店；舒适型适合三四星酒店加拼车或包车；长线和珠峰方向会明显增加交通成本。',
    ...routeAction
  },
  {
    patterns: [/拍照/, /摄影/, /日出/, /日落/, /好看的照片/],
    response: '摄影可重点看药王山拍布达拉宫、羊卓雍措观景台、纳木错星空、南迦巴瓦日照金山和珠峰日落。早晚光线好，但要留足保暖和返程时间。'
  },
  {
    patterns: [/美食/, /吃/, /小吃/, /特色菜/, /藏餐/],
    response: '藏地美食可以从甜茶、酥油茶、糌粑、牦牛肉、藏面和石锅鸡开始。初到高原先清淡一些，等身体适应后再安排更重口的藏餐。'
  },
  {
    patterns: [/礼仪/, /习俗/, /禁忌/, /注意/, /尊重/],
    response: '进寺院要尊重当地习俗：不踩门槛，按现场提示拍照，不随意触摸佛像和经幡，转经通常顺时针。拍摄当地人前最好先征得同意。'
  }
]

const fallbackResponses = [
  '我会把建议控制在西藏旅行范围内。你可以补充出发月份、旅行天数、同行人数和偏好，我再按海拔适应、车程和景点顺序给你更贴近实际的安排。',
  '先给你一个稳妥方向：第一次进藏不宜把行程排太满，建议先拉萨适应，再根据天数选择林芝、日喀则、羊湖或珠峰方向。',
  '这个要结合季节和体力看。你把计划出发时间、天数和预算告诉我，我可以继续帮你细化成更可执行的西藏行程。',
  '西藏旅行最关键的是节奏、海拔和车程。把你最想看的景色告诉我，我会帮你在自然风光、人文寺院和休息时间之间取舍。'
]

let nextMessageId = 1
let lastFallbackIndex = -1

function isTibetTravelTopic(input: string): boolean {
  const normalized = normalizeGuideChatInput(input)
  return greetingPattern.test(normalized) || tibetTravelPattern.test(normalized)
}

function matchIntent(input: string): IntentRule | null {
  const normalized = normalizeGuideChatInput(input)
  for (const rule of intentRules) {
    for (const pattern of rule.patterns) {
      pattern.lastIndex = 0
      if (pattern.test(normalized)) {
        return rule
      }
    }
  }
  return null
}

function getFallbackResponse(): string {
  const available = fallbackResponses.length
  let index: number
  do {
    index = Math.floor(Math.random() * available)
  } while (index === lastFallbackIndex && available > 1)
  lastFallbackIndex = index
  return fallbackResponses[index]
}

function toGuideMessage(text: string, options: Partial<ChatMessage> = {}): ChatMessage {
  return {
    id: nextMessageId++,
    role: 'guide',
    text,
    ...options
  }
}

function localGuideMessage(text: string): ChatMessage {
  if (!isTibetTravelTopic(text)) {
    return toGuideMessage(OFF_TOPIC_RESPONSE, { fallback: true })
  }

  const intent = matchIntent(text)
  return toGuideMessage(intent ? intent.response : getFallbackResponse(), {
    fallback: true,
    ...(intent?.action ? { action: intent.action, actionLabel: intent.actionLabel } : {})
  })
}

function normalizeAction(action?: string): 'navigate' | 'generate' | undefined {
  return action === 'navigate' || action === 'generate' ? action : undefined
}

export function coerceRetryAfter(value: unknown, fallbackSeconds = 45): number {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return Math.max(1, Math.min(Math.ceil(value), 3600))
  }
  if (typeof value === 'string') {
    const parsed = Number.parseInt(value, 10)
    if (Number.isFinite(parsed)) {
      return Math.max(1, Math.min(parsed, 3600))
    }
  }
  return fallbackSeconds
}

export function resolveGuideChatLocale(storage?: Pick<Storage, 'getItem'> | null): 'zh' | 'bo' {
  try {
    const locale = storage === undefined
      ? readBrowserStorage('localStorage', 'locale', 'zh')
      : storage?.getItem('locale')
    return locale === 'bo' ? 'bo' : 'zh'
  } catch {
    return 'zh'
  }
}

export function summarizeGuideChatError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status
    return status ? `guide chat request failed with status ${status}` : 'guide chat request failed'
  }
  return error instanceof Error && error.name ? error.name : 'unexpected guide chat error'
}

function limitedGuideMessage(data?: GuideChatApiResponse, retryHeader?: unknown): ChatMessage {
  const retryAfterSeconds = coerceRetryAfter(data?.retryAfterSeconds ?? retryHeader, 45)
  const challengeRequired = Boolean(data?.challengeRequired)
  return toGuideMessage(
    challengeRequired ? GUIDE_CHAT_CHALLENGE_MESSAGE : GUIDE_CHAT_RATE_LIMIT_MESSAGE,
    {
      fallback: true,
      limited: true,
      retryAfterSeconds,
      challengeRequired
    }
  )
}

export function createUserMessage(text: string): ChatMessage {
  return {
    id: nextMessageId++,
    role: 'user',
    text: normalizeGuideChatInput(text)
  }
}

export function createPendingGuideMessage(): ChatMessage {
  return toGuideMessage('小导游正在整理藏地建议...', { pending: true })
}

export function getLocalGuideReply(text: string): ChatMessage {
  return localGuideMessage(normalizeGuideChatInput(text))
}

export function createNetworkFallbackGuideMessage(text: string): ChatMessage {
  const retryText = normalizeGuideChatInput(text)
  return {
    ...localGuideMessage(retryText),
    networkFallback: true,
    retryText
  }
}

export function processUserMessage(text: string): ChatMessage[] {
  const normalized = normalizeGuideChatInput(text)
  return [createUserMessage(normalized), localGuideMessage(normalized)]
}

export async function requestGuideChat(
  text: string,
  history: GuideChatHistoryItem[],
  recaptchaToken = '',
  retriedChallenge = false
): Promise<ChatMessage> {
  const trimmed = normalizeGuideChatInput(text)
  if (!trimmed) {
    return localGuideMessage('')
  }
  const locale = resolveGuideChatLocale()

  try {
    const response = await api.post<GuideChatApiResponse>(endpoints.guide.chat, {
      message: trimmed,
      history: history
        .slice(-8)
        .map(item => ({
          role: item.role,
          content: normalizeGuideChatInput(item.content)
        }))
        .filter(item => item.content),
      locale
    }, recaptchaToken
      ? { headers: { 'X-Recaptcha-Token': recaptchaToken } }
      : undefined)

    const data = response.data
    const contentResult = safeGuideChatContent(data.content)
    const content = contentResult.content
    if (!content) {
      return localGuideMessage(trimmed)
    }

    const action = normalizeAction(data.action)
    const actionLabelResult = data.actionLabel
      ? safeGuideChatContent(data.actionLabel, '')
      : { content: '', sanitized: false }
    const limited = Boolean(data.limited)
    return toGuideMessage(content, {
      fallback: Boolean(data.fallback) || contentResult.sanitized || actionLabelResult.sanitized,
      limited,
      retryAfterSeconds: limited ? coerceRetryAfter(data.retryAfterSeconds, 45) : undefined,
      challengeRequired: Boolean(data.challengeRequired),
      ...(action ? { action, actionLabel: actionLabelResult.content || '去规划路线' } : {})
    })
  } catch (error) {
    if (axios.isAxiosError<GuideChatApiResponse>(error) && error.response?.status === 428) {
      if (!retriedChallenge && isRecaptchaV3Enabled()) {
        try {
          const token = await getRecaptchaToken('guide_chat')
          if (token) {
            return requestGuideChat(trimmed, history, token, true)
          }
        } catch (recaptchaError) {
          if (import.meta.env.DEV) {
            console.warn('Guide chat reCAPTCHA failed:', summarizeClientError(recaptchaError))
          }
        }
      }
      return limitedGuideMessage(error.response.data, error.response.headers?.['retry-after'])
    }
    if (axios.isAxiosError<GuideChatApiResponse>(error) && error.response?.status === 429) {
      return limitedGuideMessage(error.response.data, error.response.headers?.['retry-after'])
    }
    throw error
  }
}

export function getGreeting(): ChatMessage {
  return toGuideMessage('扎西德勒，我是西藏小导游。这里我只聊西藏旅行：路线、景点、季节、高原适应、寺院礼仪、藏餐住宿和交通预算，都可以问我。')
}

export const quickReplies = [
  { label: '景点推荐', text: '西藏有什么必去的景点推荐？' },
  { label: '规划路线', text: '帮我规划一条第一次进藏的路线' },
  { label: '最佳季节', text: '什么时候去西藏最合适？' },
  { label: '高原适应', text: '去西藏高原反应要注意什么？' },
  { label: '大概预算', text: '去一趟西藏大概要花多少钱？' },
  { label: '藏地美食', text: '西藏有什么适合游客尝试的美食？' },
  { label: '拍照攻略', text: '西藏哪里拍照最好看？' },
  { label: '文化礼仪', text: '去西藏寺院和当地社区要注意什么礼仪？' }
]
