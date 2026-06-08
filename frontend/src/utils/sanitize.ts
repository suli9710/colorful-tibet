import DOMPurify, { type Config } from 'dompurify'
import { marked } from 'marked'

const SAFE_BLANK_LINK_REL = 'noopener noreferrer'
const SAFE_LINK_HREF_REGEXP = /^(?:(?:https?|mailto|tel):|\/(?!\/)|#|\?|\.{1,2}\/|[^\s:/\\?#]+(?:[/?#]|$))/i
const SAFE_IMAGE_SRC_REGEXP = /^(?:(?:https?):|\/(?!\/)|\.{1,2}\/|[^\s:/\\?#]+(?:[/?#]|$))/i
const ABSOLUTE_HTTP_URL_REGEXP = /^https?:\/\//i

const SANITIZE_CONFIG: Config = {
  ALLOWED_TAGS: [
    'a',
    'blockquote',
    'br',
    'code',
    'del',
    'em',
    'h1',
    'h2',
    'h3',
    'h4',
    'h5',
    'h6',
    'hr',
    'img',
    'li',
    'ol',
    'p',
    'pre',
    'span',
    'strong',
    'table',
    'tbody',
    'td',
    'th',
    'thead',
    'tr',
    'ul'
  ],
  ALLOWED_ATTR: ['align', 'alt', 'height', 'href', 'loading', 'rel', 'src', 'target', 'title', 'width'],
  ALLOWED_URI_REGEXP: SAFE_LINK_HREF_REGEXP,
  FORBID_TAGS: ['svg', 'math', 'style', 'form', 'iframe', 'object', 'embed', 'script'],
  RETURN_TRUSTED_TYPE: false,
}

if (typeof DOMPurify.addHook === 'function') {
  DOMPurify.addHook('afterSanitizeAttributes', node => {
    const element = node as Element
    const tagName = element.nodeName.toLowerCase()

    if (tagName === 'a') {
      const href = element.getAttribute('href')?.trim()
      const target = element.getAttribute('target')?.trim().toLowerCase()

      if (!href || !SAFE_LINK_HREF_REGEXP.test(href)) {
        element.removeAttribute('href')
        element.removeAttribute('target')
        element.removeAttribute('rel')
        return
      }

      element.setAttribute('href', href)

      if (target === '_blank' || ABSOLUTE_HTTP_URL_REGEXP.test(href)) {
        element.setAttribute('target', '_blank')
        element.setAttribute('rel', SAFE_BLANK_LINK_REL)
      } else {
        element.removeAttribute('target')
        element.removeAttribute('rel')
      }
      return
    }

    if (tagName === 'img') {
      const source = element.getAttribute('src')?.trim()
      if (!source || !SAFE_IMAGE_SRC_REGEXP.test(source)) {
        element.removeAttribute('src')
      }
    }

    element.removeAttribute('rel')
    element.removeAttribute('target')
  })
}

export function sanitizeHtml(dirty: string | null | undefined): string {
  return DOMPurify.sanitize(dirty ?? '', SANITIZE_CONFIG) as string
}

export function renderMarkdownToSafeHtml(markdown: string | null | undefined): string {
  const source = markdown ?? ''
  if (!source) return ''

  return sanitizeHtml(marked(source, { async: false }))
}
