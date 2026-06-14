import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const vue = fs.readFileSync(path.join(__dirname, '../src/views/Heritage.vue'), 'utf8')
const m = vue.match(/const nationalHeritageRaw = `([\s\S]*?)`/)
if (!m) throw new Error('nationalHeritageRaw not found')

const raw = m[1]
const categoryPrefixes = ['一、', '二、', '三、', '四、', '五、', '六、', '七、', '八、']
const categoryKeys = [
  'folkLiterature',
  'traditionalMusic',
  'traditionalDance',
  'traditionalDrama',
  'traditionalSports',
  'traditionalCraft',
  'traditionalMedicine',
  'folkCustom'
]

const lines = raw.split('\n').map((l) => l.trim()).filter(Boolean)
const categories = {}
let currentKey = ''
let id = 1

for (const line of lines) {
  const prefixIdx = categoryPrefixes.findIndex((p) => line.startsWith(p))
  if (prefixIdx !== -1) {
    currentKey = categoryKeys[prefixIdx]
    if (!categories[currentKey]) categories[currentKey] = { items: [] }
    continue
  }
  if (!currentKey || line.startsWith('西藏自治区') || line.startsWith('西藏国家级')) continue

  const sepIndex = line.indexOf('：')
  let name = line
  let description = ''
  if (sepIndex !== -1) {
    name = line.slice(0, sepIndex).trim()
    description = line.slice(sepIndex + 1).trim()
  }
  const firstParenIdx = name.indexOf('（')
  if (firstParenIdx !== -1) name = name.slice(0, firstParenIdx).trim()

  categories[currentKey].items.push({ id: id++, name, description })
}

const representativeZh = [
  {
    key: 'medicine',
    id: 10001,
    name: '藏药',
    description:
      '源自雪域高原的传统医学体系，吸收了藏族本土经验与印度、汉地医学精华，以丸、散、膏、丹等剂型闻名。',
    categoryKey: 'traditionalMedicine',
    significance:
      '体现了藏族人民与高原自然环境长期博弈中形成的健康智慧，是中华传统医学宝库的重要组成部分。',
    imageUrl: '/heritage/藏药.jpg',
    baikeUrl: 'https://baike.baidu.com/item/%E8%97%8F%E5%8C%BB%E8%8D%AF%E6%B5%B4%E6%B3%95'
  },
  {
    key: 'gesar',
    id: 10002,
    name: '格萨尔史诗',
    description: '被誉为”世界上最长的史诗”，通过艺人口耳相传、即兴说唱的方式一代代流传下来。',
    categoryKey: 'folkLiterature',
    significance:
      '记录了藏族社会的历史记忆、英雄理想与价值观，是中华民族口头传统中的璀璨明珠。',
    imageUrl: '/heritage/格萨尔史诗.jpg',
    baikeUrl: 'https://baike.baidu.com/item/%E6%A0%BC%E8%90%A8%E5%B0%94%E7%8E%8B%E4%BC%A0'
  },
  {
    key: 'tibetanOpera',
    id: 10003,
    name: '藏戏',
    description: '被誉为”藏文化的活化石”，集歌舞、说唱、表演于一体，常在寺院法会和民间节日中演出。',
    categoryKey: 'traditionalDrama',
    significance:
      '藏戏综合了宗教仪式、历史故事与民间传说，是研究藏族社会生活与信仰体系的重要窗口。',
    imageUrl: '/heritage/藏戏.jpg',
    baikeUrl: 'https://baike.baidu.com/item/%E8%97%8F%E6%88%8F'
  },
  {
    key: 'thangka',
    id: 10004,
    name: '藏族唐卡',
    description: '以矿物颜料在布、纸或丝绸上绘制的宗教卷轴画，色彩瑰丽、构图严谨，多悬挂于寺院与居室。',
    categoryKey: 'traditionalCraft',
    significance:
      '唐卡承载着藏传佛教教义、历史人物与宇宙观，被视为”可以卷起来带走的宫殿壁画”，是西藏艺术的代表符号之一。',
    imageUrl: '/heritage/唐卡.jpg',
    baikeUrl: 'https://baike.baidu.com/item/%E5%94%90%E5%8D%A1'
  }
]

const experienceSpots = [
  {
    name: '拉萨非遗体验中心（八廓街）',
    city: '拉萨',
    address: '拉萨市城关区八廓街步行街附近',
    lat: 29.653,
    lng: 91.117,
    tag: '藏',
    brief: '非遗集合体验空间，可预约藏戏、唐卡、藏香等项目体验',
    highlight: '一站式打卡多种非遗项目'
  },
  {
    name: '罗布林卡唐卡工坊',
    city: '拉萨',
    address: '拉萨市城关区罗布林卡景区周边传统手工街区',
    lat: 29.642,
    lng: 91.071,
    tag: '画',
    brief: '专注藏族唐卡绘制与展示的工作室，支持短时体验与深度课程',
    highlight: '亲手绘制一幅简易唐卡或吉祥纹样'
  },
  {
    name: '日喀则藏戏传习中心',
    city: '日喀则',
    address: '日喀则市桑珠孜区传统文化街区内',
    lat: 29.268,
    lng: 88.882,
    tag: '戏',
    brief: '定期排练和展演藏戏的传习点，游客可预约观摩与互动体验',
    highlight: '近距离观看一场完整的藏戏表演'
  },
  {
    name: '林芝藏药文化体验馆',
    city: '林芝',
    address: '林芝市巴宜区林芝镇附近康养文化街区',
    lat: 29.654,
    lng: 94.362,
    tag: '药',
    brief: '结合藏药展示、讲解与简易调养体验的综合空间',
    highlight: '了解常见藏药材与传统养生方式'
  }
]

const imageAliases = {
  格萨尔史诗: '/heritage/格萨尔史诗.jpg',
  格萨尔: '/heritage/格萨尔史诗.jpg',
  藏戏: '/heritage/藏戏.jpg',
  藏族唐卡: '/heritage/唐卡.jpg',
  唐卡: '/heritage/唐卡.jpg',
  藏医药浴法: '/heritage/藏药.jpg',
  藏药: '/heritage/藏药.jpg',
  拉萨囊玛: '/heritage/拉萨囊玛.jpg',
  拉萨朗玛: '/heritage/拉萨囊玛.jpg',
  囊玛: '/heritage/拉萨囊玛.jpg',
  那曲山歌: '/heritage/那曲山歌.jpeg',
  藏族山歌: '/heritage/那曲山歌.jpeg',
  藏北民歌: '/heritage/那曲山歌.jpeg',
  热巴舞: '/heritage/热巴舞.jpg',
  锅庄舞: '/heritage/锅庄舞.jpg',
  弦子舞: '/heritage/弦子舞.jpg',
  门巴戏: '/heritage/门巴戏.jpg',
  藏族传统马术: '/heritage/藏族传统马术.jpg',
  马术: '/heritage/藏族传统马术.jpg',
  藏香制作技艺: '/heritage/藏香制作技艺.jpg',
  藏香: '/heritage/藏香制作技艺.jpg',
  藏刀锻制技艺: '/heritage/藏刀锻制技艺.jpg',
  藏刀: '/heritage/藏刀锻制技艺.jpg',
  '藏族邦典/卡垫织造技艺': '/heritage/藏族邦典卡垫织造技艺.jpg',
  藏族邦典卡垫织造技艺: '/heritage/藏族邦典卡垫织造技艺.jpg',
  邦典: '/heritage/藏族邦典卡垫织造技艺.jpg',
  卡垫: '/heritage/藏族邦典卡垫织造技艺.jpg',
  藏族雕版印刷技艺: '/heritage/藏族雕版印刷技艺.jpg',
  雕版印刷: '/heritage/藏族雕版印刷技艺.jpg',
  藏族造纸技艺: '/heritage/藏族造纸技艺.jpg',
  藏纸: '/heritage/藏族造纸技艺.jpg',
  雪顿节: '/heritage/雪顿节.jpg',
  望果节: '/heritage/望果节.jpg',
  藏族金属锻造技艺: '/heritage/藏族金属锻造技艺.jpg',
  金属锻造: '/heritage/藏族金属锻造技艺.jpg',
  墨脱石锅制作技艺: '/heritage/墨脱石锅制作技艺.jpg',
  墨脱石锅: '/heritage/墨脱石锅制作技艺.jpg',
  羌姆: '/heritage/羌姆.jpg',
  'གེ་སར': '/heritage/格萨尔史诗.jpg',
  'བོད་ཟློས་གར': '/heritage/藏戏.jpg',
  'ཐང་ཀ': '/heritage/唐卡.jpg',
  'བོད་སྨན': '/heritage/藏药.jpg'
}

const zhContent = {
  heritageContent: {
    national: {
      noDescription: '（暂无补充说明，后续可在后台完善这一条目的详细介绍。）',
      categories
    },
    representative: Object.fromEntries(representativeZh.map((item) => [item.key, item])),
    experienceSpots,
    imageAliases,
    aliases: {
      thangkaBackendName: '唐卡',
      thangkaDisplayName: '藏族唐卡'
    },
    eventMonthSuffix: '月'
  }
}

const outPath = path.join(__dirname, '../src/i18n/locales/heritage-content.zh.json')
fs.writeFileSync(outPath, JSON.stringify(zhContent, null, 2) + '\n', 'utf8')
console.log(`Wrote ${outPath} with ${id - 1} national items`)
