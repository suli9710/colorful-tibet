import privacyBo from './privacy.bo.md?raw'
import privacyZh from './privacy.zh.md?raw'
import termsBo from './terms.bo.md?raw'
import termsZh from './terms.zh.md?raw'

export type LegalDocumentKind = 'privacy' | 'terms'

type LegalLocale = 'bo' | 'zh'

const legalDocuments: Record<LegalDocumentKind, Record<LegalLocale, string>> = {
  privacy: {
    bo: privacyBo,
    zh: privacyZh
  },
  terms: {
    bo: termsBo,
    zh: termsZh
  }
}

const resolveLegalLocale = (locale: string): LegalLocale =>
  locale.toLowerCase().startsWith('bo') ? 'bo' : 'zh'

export const getLegalMarkdown = (kind: LegalDocumentKind, locale: string) => {
  const legalLocale = resolveLegalLocale(locale)
  return legalDocuments[kind][legalLocale] || legalDocuments[kind].zh
}
