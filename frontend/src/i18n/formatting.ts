export const toIntlLocale = (locale: string) => locale === 'bo' ? 'bo-CN' : 'zh-CN'

export const toFiniteAmount = (value?: number | string | null) => {
  const amount = Number(value ?? 0)
  return Number.isFinite(amount) ? amount : 0
}
