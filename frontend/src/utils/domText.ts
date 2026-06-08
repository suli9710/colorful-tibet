export const createTextPopupContent = (title: string, body: string) => {
  const content = document.createElement('div')
  const titleElement = document.createElement('strong')
  titleElement.textContent = title
  content.append(titleElement, document.createElement('br'), document.createTextNode(body))
  return content
}

export interface TextPopupLine {
  text: string
  color?: string
}

export const createTextCardPopupContent = (title: string, lines: TextPopupLine[]) => {
  const content = document.createElement('div')
  content.style.padding = '6px 2px'
  content.style.minWidth = '190px'
  content.style.maxWidth = '240px'

  const titleElement = document.createElement('h3')
  titleElement.textContent = title
  titleElement.style.margin = '0 0 8px 0'
  titleElement.style.fontSize = '15px'
  titleElement.style.fontWeight = '700'
  titleElement.style.color = '#1c1917'
  content.append(titleElement)

  lines.forEach((line, index) => {
    const paragraph = document.createElement('p')
    paragraph.textContent = line.text
    paragraph.style.margin = index === 0 ? '4px 0' : '6px 0 0 0'
    paragraph.style.fontSize = '12px'
    paragraph.style.color = line.color || '#57534e'
    paragraph.style.lineHeight = '1.55'
    content.append(paragraph)
  })

  return content
}
