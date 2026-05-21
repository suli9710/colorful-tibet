import { marked } from 'marked'

interface RenderRequest {
  id: number
  markdown: string
}

interface RenderResponse {
  id: number
  html: string
}

self.onmessage = (event: MessageEvent<RenderRequest>) => {
  const { id, markdown } = event.data
  const html = markdown ? String(marked.parse(markdown)) : ''
  const response: RenderResponse = { id, html }
  self.postMessage(response)
}
