/** 轻量 Markdown：加粗、列表、换行 */
export function renderMarkdownLite(text: string): string {
  if (!text) return ''
  const escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  let html = escaped.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
  if (html.includes('<li>')) {
    html = html.replace(/(<li>[\s\S]*?<\/li>)+/g, (m) => `<ul>${m}</ul>`)
  }
  html = html.replace(/\n/g, '<br/>')
  return html
}
