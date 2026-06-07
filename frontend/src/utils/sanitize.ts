import DOMPurify, { type Config } from 'dompurify'

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
  ALLOWED_ATTR: ['href', 'title', 'target', 'rel'],
  ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|\/(?!\/)|#)/i,
  FORBID_TAGS: ['svg', 'math', 'style', 'form', 'iframe', 'object', 'embed', 'script'],
  RETURN_TRUSTED_TYPE: false,
}

export function sanitizeHtml(dirty: string): string {
  const clean = DOMPurify.sanitize(dirty, SANITIZE_CONFIG) as string
  return clean.replace(/target="_blank"/g, 'target="_blank" rel="noopener noreferrer"')
}
