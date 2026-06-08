export interface NewsImageItem {
  id?: number | string | null
  imageUrl?: string | null
  title?: string | null
  category?: string | null
}

export type FailedNewsImageMap = Record<string, boolean>

export const NEWS_DEFAULT_IMAGE = '/heritage/布达拉宫3.jpg'
export const NEWS_FALLBACK_IMAGE = '/heritage/大昭寺.jpg'

const NEWS_CATEGORY_IMAGE_MAP: Record<string, string> = {
  POLICY: '/heritage/布达拉宫3.jpg',
  EVENT: '/heritage/雪顿节.jpg',
  NOTICE: '/heritage/大昭寺.jpg',
}

export const getNewsImageKey = (item: NewsImageItem) =>
  String(item.id || item.imageUrl || item.title || 'news-image')

export const resolveNewsImage = (
  item: NewsImageItem,
  failedImages: FailedNewsImageMap,
) => {
  if (failedImages[getNewsImageKey(item)]) {
    return NEWS_FALLBACK_IMAGE
  }

  return item.imageUrl || NEWS_CATEGORY_IMAGE_MAP[item.category || ''] || NEWS_DEFAULT_IMAGE
}

export const markNewsImageFailed = (
  item: NewsImageItem,
  failedImages: FailedNewsImageMap,
) => {
  failedImages[getNewsImageKey(item)] = true
}
