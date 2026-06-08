export interface NewsImageItem {
  id?: number | string | null
  imageUrl?: string | null
  title?: string | null
}

export type FailedNewsImageMap = Record<string, boolean>

export const NEWS_DEFAULT_IMAGE = '/images/news/default-news.jpg'
export const NEWS_FALLBACK_IMAGE = '/images/spots/布达拉宫.jpg'

export const getNewsImageKey = (item: NewsImageItem) =>
  String(item.id || item.imageUrl || item.title || 'news-image')

export const resolveNewsImage = (
  item: NewsImageItem,
  failedImages: FailedNewsImageMap,
) => failedImages[getNewsImageKey(item)]
  ? NEWS_FALLBACK_IMAGE
  : (item.imageUrl || NEWS_DEFAULT_IMAGE)

export const markNewsImageFailed = (
  item: NewsImageItem,
  failedImages: FailedNewsImageMap,
) => {
  failedImages[getNewsImageKey(item)] = true
}
