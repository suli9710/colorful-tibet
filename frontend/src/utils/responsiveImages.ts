export interface ResponsiveImageSources {
  avif: string
  webp: string
  fallback: string
}

const optimizedBase = '/heritage/optimized'

const createSources = (stem: string, widths: readonly number[]): ResponsiveImageSources => {
  const srcset = (extension: 'avif' | 'webp') => widths
    .map(width => `${optimizedBase}/${stem}-${width}.${extension} ${width}w`)
    .join(', ')
  const largestWidth = widths[widths.length - 1]

  return {
    avif: srcset('avif'),
    webp: srcset('webp'),
    fallback: `${optimizedBase}/${stem}-${largestWidth}.webp`,
  }
}

const responsiveImages: Readonly<Record<string, ResponsiveImageSources>> = {
  '/heritage/布达拉宫3.jpg': createSources('布达拉宫3', [640, 1280, 1920]),
  '/heritage/纳木错.jpg': createSources('纳木错', [640, 999]),
  '/heritage/藏戏.jpg': createSources('藏戏', [500]),
  '/heritage/雅鲁藏布大峡谷.jpg': createSources('雅鲁藏布大峡谷', [640, 1280]),
}

export const resolveResponsiveImage = (source?: string) => (
  source ? responsiveImages[source] : undefined
)
