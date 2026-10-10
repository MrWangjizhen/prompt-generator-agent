let accessToken = ''
let refreshPromise = null

export function setAccessToken(token) {
  accessToken = token || ''
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = fetch('/api/v1/auth/refresh', {
      method: 'POST',
      credentials: 'include'
    }).then(async response => {
      if (!response.ok) throw new Error('登录已过期')
      const body = await response.json()
      setAccessToken(body.accessToken)
      return body
    }).finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

export async function api(path, options = {}, retry = true) {
  const headers = new Headers(options.headers || {})
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  const response = await fetch(`/api${path}`, { ...options, headers, credentials: 'include' })
  if (response.status === 401 && retry && !path.startsWith('/v1/auth/')) {
    await refreshAccessToken()
    return api(path, options, false)
  }
  if (!response.ok) {
    let message = `请求失败 (${response.status})`
    try { message = (await response.json()).message || message } catch { /* response is not JSON */ }
    throw new Error(message)
  }
  if (response.status === 204) return null
  return response.json()
}

export async function authenticate(mode, credentials) {
  const result = await api(`/v1/auth/${mode}`, { method: 'POST', body: JSON.stringify(credentials) }, false)
  setAccessToken(result.accessToken)
  return result.user
}

export async function restoreSession() {
  const result = await refreshAccessToken()
  return result.user
}

export async function stream(path, payload, onEvent, retry = true) {
  const headers = new Headers({ 'Content-Type': 'application/json', 'Accept': 'text/event-stream' })
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  const response = await fetch(`/api${path}`, {
    method: 'POST', headers, credentials: 'include', body: JSON.stringify(payload)
  })
  if (response.status === 401 && retry) {
    await refreshAccessToken()
    return stream(path, payload, onEvent, false)
  }
  if (!response.ok) throw new Error(`流式请求失败 (${response.status})`)
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
    const frames = buffer.split(/\r?\n\r?\n/)
    buffer = frames.pop() || ''
    for (const frame of frames) {
      let event = 'message'
      const data = []
      for (const line of frame.split(/\r?\n/)) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        if (line.startsWith('data:')) data.push(line.slice(5).trim())
      }
      if (data.length) {
        const raw = data.join('\n')
        let parsed = raw
        try { parsed = JSON.parse(raw) } catch { /* keep text */ }
        await onEvent(event, parsed)
      }
    }
    if (done) break
  }
}

export async function downloadPrompt(sessionId, format) {
  const headers = new Headers()
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  const response = await fetch(`/api/v1/prompt-sessions/${sessionId}/export?format=${format}`, {
    headers, credentials: 'include'
  })
  if (!response.ok) throw new Error('导出失败')
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `prompt.${format === 'json' ? 'json' : 'md'}`
  anchor.click()
  URL.revokeObjectURL(url)
}

export function parseSseFrame(frame) {
  let event = 'message'
  const data = []
  for (const line of frame.split(/\r?\n/)) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    if (line.startsWith('data:')) data.push(line.slice(5).trim())
  }
  return { event, data: data.join('\n') }
}
