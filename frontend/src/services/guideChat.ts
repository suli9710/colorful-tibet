export interface ChatMessage {
  id: number
  role: 'guide' | 'user'
  text: string
  action?: 'navigate' | 'generate'
  actionLabel?: string
}

interface IntentRule {
  patterns: RegExp[]
  response: string
  action?: 'navigate' | 'generate'
  actionLabel?: string
}

const intentRules: IntentRule[] = [
  {
    patterns: [/你好/, /嗨/, /hello/i, /hi/i, /你是谁/, /叫什么/],
    response: '你好呀！我是你的西藏旅行小导游，可以帮你规划路线、推荐景点、解答旅行疑问～想去哪里玩呢？'
  },
  {
    patterns: [/规划路线/, /路线/, /行程/, /帮我规划/, /怎么玩/, /安排/],
    response: '交给我吧！告诉我你想玩几天、预算多少、喜欢什么类型的风景，我帮你生成一条完美的西藏路线！',
    action: 'navigate',
    actionLabel: '去规划路线'
  },
  {
    patterns: [/景点/, /推荐/, /哪里好玩/, /必去/, /有什么地方/],
    response: '西藏必去的景点很多！布达拉宫雄伟壮观，纳木错湖水湛蓝，珠峰大本营震撼人心，羊卓雍措如蓝宝石般美丽，大昭寺香火缭绕……每个地方都值得一去！你想看自然风光还是人文古迹呢？'
  },
  {
    patterns: [/自然/, /风光/, /风景/, /徒步/, /雪山/, /湖泊/],
    response: '喜欢自然风光的话，一定不能错过纳木错和羊卓雍措这两大圣湖，还有雅鲁藏布大峡谷的壮丽、南迦巴瓦峰的日照金山、林芝的桃花沟……我可以帮你规划一条偏自然风光的路线！'
  },
  {
    patterns: [/人文/, /古迹/, /寺庙/, /历史/, /文化/],
    response: '西藏的人文底蕴深厚！布达拉宫、大昭寺、色拉寺的辩经、扎什伦布寺、古格王朝遗址……每座寺庙都有自己的故事。选择"文化探索"偏好，我会帮你串联起这些人文瑰宝！'
  },
  {
    patterns: [/什么时候去/, /季节/, /几月/, /最佳时间/, /天气/],
    response: '5-10月是西藏旅游的黄金季节，气候温和，氧气充足，景色最美。想看桃花的话3-4月去林芝，想体验藏历新年可以冬季前往。不过冬天部分高海拔路段会封闭哦～'
  },
  {
    patterns: [/高原反应/, /海拔/, /氧气/, /高反/, /身体/, /适应/],
    response: '高原反应是很多人担心的问题～建议到达拉萨后先休息1-2天适应，多喝温水、少剧烈运动、保持心情放松。我们生成的每条路线都会科学安排海拔适应节奏，避免连续高海拔住宿！'
  },
  {
    patterns: [/预算/, /钱/, /费用/, /贵/, /便宜/, /省钱/],
    response: '西藏旅行丰俭由人！经济型大概3000-5000元/人（青旅+拼车），舒适型5000-8000元/人，豪华型8000元以上。我们支持经济、舒适、豪华三种预算档位，选择"经济"档我会帮你精打细算！',
    action: 'navigate',
    actionLabel: '去规划路线'
  },
  {
    patterns: [/拍照/, /摄影/, /日出/, /日落/, /好看的照片/],
    response: '摄影爱好者来对地方了！珠峰日出、纳木错星空、羊卓雍措的蒂芙尼蓝、古格王朝的黄昏……选择"摄影之旅"偏好，我会帮你安排最佳拍摄时间和机位！'
  },
  {
    patterns: [/美食/, /吃/, /小吃/, /特色菜/, /藏餐/],
    response: '一定要尝尝酥油茶、甜茶、青稞酒、糌粑、牦牛肉干、藏面、藏包子、酸奶！拉萨八廓街附近的藏餐馆都很地道，光明港琼甜茶馆是本地人最爱去的地方～'
  },
  {
    patterns: [/礼仪/, /习俗/, /禁忌/, /注意/, /尊重/],
    response: '藏族同胞非常热情好客！进寺庙记得脱帽、不踩门槛、顺时针转经、不手指佛像、不摸小孩的头。拍照前先征得同意。我们的路线结果里附带详细的文化礼仪指南哦～'
  },
  {
    patterns: [/住宿/, /住/, /酒店/, /宾馆/, /客栈/],
    response: '拉萨有从青旅到五星级酒店的各种选择，林芝和日喀则也有很多舒适的住处。在路线规划完成后，你还可以直接预订我们推荐的酒店，省心又方便！'
  },
  {
    patterns: [/交通/, /怎么去/, /火车/, /飞机/, /自驾/],
    response: '进藏可以选择飞机（拉萨贡嘎机场）、火车（青藏铁路，一路风景绝美）、或者自驾（青藏线/川藏线/滇藏线）。西藏内部推荐包车或拼车，因为景点之间距离较远，公共交通不太方便。'
  },
  {
    patterns: [/几天/, /玩多久/, /天数/, /时间够/],
    response: '拉萨市区2-3天，拉萨+日喀则+珠峰线7-9天，林芝线3-4天，阿里大环线12-15天，全藏深度游16天以上。一般来说，西藏玩7-10天比较合适！你想安排几天的行程呢？',
    action: 'navigate',
    actionLabel: '去规划路线'
  }
]

const fallbackResponses = [
  '这个问题问得好！不过我还不太了解呢～不如让我帮你规划一条路线，在实际旅行中慢慢探索吧！',
  '哎呀，这个问题把我难住了！但是我可以帮你生成一条超棒的西藏路线哦～',
  '好问题！虽然我不确定答案，但我可以帮你规划路线，沿途你一定能找到答案！',
  '哈哈，你的好奇心真强！要不我们先规划一条路线，边玩边发现？'
]

let nextMessageId = 1
let lastFallbackIndex = -1

function matchIntent(input: string): IntentRule | null {
  const normalized = input.trim()
  for (const rule of intentRules) {
    for (const pattern of rule.patterns) {
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

export function processUserMessage(text: string): ChatMessage[] {
  const userMsg: ChatMessage = {
    id: nextMessageId++,
    role: 'user',
    text: text.trim()
  }

  const intent = matchIntent(text)
  const guideMsg: ChatMessage = {
    id: nextMessageId++,
    role: 'guide',
    text: intent ? intent.response : getFallbackResponse(),
    ...(intent?.action ? { action: intent.action, actionLabel: intent.actionLabel } : {})
  }

  return [userMsg, guideMsg]
}

export function getGreeting(): ChatMessage {
  return {
    id: nextMessageId++,
    role: 'guide',
    text: '你好呀！我是你的西藏旅行小导游～想去哪里玩呢？可以问我景点推荐、旅行贴士，或者让我帮你规划一条完美的路线！'
  }
}

export const quickReplies = [
  { label: '推荐景点', text: '有什么必去的景点推荐？' },
  { label: '规划路线', text: '帮我规划一条路线' },
  { label: '最佳季节', text: '什么时候去西藏最好？' },
  { label: '高原反应', text: '高原反应严重吗？要注意什么？' },
  { label: '大概预算', text: '去一趟西藏大概要花多少钱？' },
  { label: '藏地美食', text: '西藏有什么好吃的？' },
  { label: '拍照攻略', text: '西藏哪里拍照最好看？' },
  { label: '文化礼仪', text: '去西藏要注意什么礼仪习俗？' }
]
