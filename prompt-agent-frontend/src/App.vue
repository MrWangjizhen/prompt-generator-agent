<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, authenticate, downloadPrompt, restoreSession, setAccessToken, stream } from './api.js'

const user = ref(null)
const authMode = ref('login')
const credentials = ref({ username: '', password: '' })
const authBusy = ref(false)
const sessions = ref([])
const current = ref(null)
const taskType = ref('TEXT')
const message = ref('')
const messages = ref([])
const streamingCandidates = ref({})
const busy = ref(false)
const notice = ref('')
const error = ref('')
const adminFile = ref(null)
const adminDocuments = ref([])

const isAdmin = computed(() => user.value?.roles?.includes('ROLE_ADMIN'))
const canConfirm = computed(() => current.value?.status === 'AWAITING_CONFIRMATION')
const streamingCandidateList = computed(() => Object.values(streamingCandidates.value)
  .sort((left, right) => left.candidateIndex - right.candidateIndex))

onMounted(async () => {
  try {
    user.value = await restoreSession()
    await loadSessions()
    if (isAdmin.value) await loadDocuments()
  } catch { /* first visit */ }
})

async function submitAuth() {
  authBusy.value = true; error.value = ''
  try {
    user.value = await authenticate(authMode.value, credentials.value)
    credentials.value.password = ''
    await loadSessions()
    if (isAdmin.value) await loadDocuments()
  } catch (e) { error.value = e.message } finally { authBusy.value = false }
}

async function logout() {
  await api('/v1/auth/logout', { method: 'POST' })
  setAccessToken(''); user.value = null; sessions.value = []; current.value = null
  messages.value = []; streamingCandidates.value = {}
}

async function loadSessions() {
  sessions.value = await api('/v1/prompt-sessions')
}

async function createSession() {
  current.value = await api('/v1/prompt-sessions', {
    method: 'POST', body: JSON.stringify({ taskType: taskType.value, memoryConsent: user.value.memoryConsent })
  })
  restoreMessages(current.value)
  streamingCandidates.value = {}
  sessions.value.unshift(current.value)
}

async function openSession(item) {
  current.value = await api(`/v1/prompt-sessions/${item.id}`)
  restoreMessages(current.value)
  streamingCandidates.value = {}
}

async function sendTurn() {
  const value = message.value.trim()
  if (!value || !current.value || busy.value) return
  messages.value.push({ role: 'user', text: value }); message.value = ''; busy.value = true; error.value = ''
  try {
    await stream(`/v1/prompt-sessions/${current.value.id}/turns/stream`, {
      message: value, expectedVersion: current.value.version
    }, async (event, data) => {
      if (event === 'question' || event === 'confirmation_required') messages.value.push({ role: 'assistant', text: data.message })
      if (event === 'done') { current.value = data; replaceSession(data) }
      if (event === 'error') throw new Error(data.message)
    })
  } catch (e) { error.value = e.message } finally { busy.value = false }
}

async function confirmRequirements() {
  busy.value = true; error.value = ''; notice.value = '正在生成三个版本…'; streamingCandidates.value = {}
  try {
    await stream(`/v1/prompt-sessions/${current.value.id}/confirm/stream`, {
      expectedVersion: current.value.version
    }, async (event, data) => {
      if (event === 'generation_started') current.value = { ...current.value, status: 'GENERATING' }
      if (event === 'generation_delta') applyGenerationDelta(data)
      if (event === 'candidate') acceptStreamingCandidate(data)
      if (event === 'done') { current.value = data; replaceSession(data); streamingCandidates.value = {} }
      if (event === 'error') throw new Error(data.message)
    })
    notice.value = '三个版本已生成，请选择最符合需要的一个。'
  } catch (e) { error.value = e.message } finally { busy.value = false }
}

async function selectCandidate(candidate) {
  current.value = await api(`/v1/prompt-sessions/${current.value.id}/selection`, {
    method: 'POST', body: JSON.stringify({ candidateId: candidate.id, expectedVersion: current.value.version })
  })
  replaceSession(current.value)
}

async function copy(text) {
  await navigator.clipboard.writeText(text); notice.value = '已复制到剪贴板'
}

async function toggleMemory() {
  const enabled = !user.value.memoryConsent
  await api('/v1/memory/consent', { method: 'POST', body: JSON.stringify({ enabled }) })
  user.value = { ...user.value, memoryConsent: enabled }
  notice.value = enabled ? '长期记忆已开启' : '长期记忆已关闭'
}

async function clearMemory() {
  if (!confirm('确定清除全部长期记忆吗？此操作不可撤销。')) return
  await api('/v1/memory', { method: 'DELETE' })
  user.value = { ...user.value, memoryConsent: false }; notice.value = '长期记忆已清除'
}

async function uploadKnowledge() {
  if (!adminFile.value) return
  const form = new FormData(); form.append('file', adminFile.value)
  await api('/v1/admin/knowledge', { method: 'POST', body: form })
  adminFile.value = null; await loadDocuments(); notice.value = '文档已上传并创建索引任务'
}

async function loadDocuments() { adminDocuments.value = await api('/v1/admin/knowledge') }

function replaceSession(value) {
  const index = sessions.value.findIndex(item => item.id === value.id)
  if (index >= 0) sessions.value[index] = value
}

function restoreMessages(session) {
  messages.value = (session.turns || []).map(turn => ({ role: turn.role, text: turn.content }))
}

function applyGenerationDelta(delta) {
  const previous = streamingCandidates.value[delta.variant] || {
    candidateIndex: delta.candidateIndex,
    variant: delta.variant,
    attempt: delta.attempt,
    content: '',
    completed: false
  }
  const content = delta.reset ? '' : previous.content + (delta.delta || '')
  streamingCandidates.value = {
    ...streamingCandidates.value,
    [delta.variant]: { ...previous, candidateIndex: delta.candidateIndex, attempt: delta.attempt, content, completed: false }
  }
}

function acceptStreamingCandidate(candidate) {
  const previous = streamingCandidates.value[candidate.variant] || { candidateIndex: 99, content: '' }
  streamingCandidates.value = {
    ...streamingCandidates.value,
    [candidate.variant]: { ...previous, ...candidate, content: candidate.content, completed: true }
  }
}
</script>

<template>
  <main v-if="!user" class="auth-shell">
    <section class="auth-card">
      <div class="brand-mark">P</div>
      <p class="eyebrow">PROMPT GENERATOR AGENT</p>
      <h1>把模糊想法，问成真正可用的提示词。</h1>
      <p class="muted">AI 会逐步澄清目标、上下文、约束与验收标准，再给你三个可直接使用的版本。</p>
      <form @submit.prevent="submitAuth" class="auth-form">
        <label>用户名<input v-model.trim="credentials.username" autocomplete="username" required minlength="3" /></label>
        <label>密码<input v-model="credentials.password" type="password" autocomplete="current-password" required minlength="10" /></label>
        <p v-if="error" class="error">{{ error }}</p>
        <button class="primary" :disabled="authBusy">{{ authBusy ? '请稍候…' : authMode === 'login' ? '登录' : '创建账号' }}</button>
      </form>
      <button class="link" @click="authMode = authMode === 'login' ? 'register' : 'login'">
        {{ authMode === 'login' ? '没有账号？立即注册' : '已有账号？返回登录' }}
      </button>
    </section>
  </main>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div><p class="eyebrow">PROMPT FORGE</p><h2>需求工作台</h2></div>
      <div class="new-session">
        <select v-model="taskType" aria-label="任务类型">
          <option v-for="type in ['TEXT','CODE','ANALYSIS','IMAGE','AGENT']" :key="type">{{ type }}</option>
        </select>
        <button class="primary" @click="createSession">＋ 新需求</button>
      </div>
      <nav class="session-list">
        <button v-for="item in sessions" :key="item.id" :class="{ active: current?.id === item.id }" @click="openSession(item)">
          <span>{{ item.taskType }}</span><small>{{ item.status }}</small>
        </button>
      </nav>
      <div class="account">
        <strong>{{ user.username }}</strong>
        <button class="link" @click="toggleMemory">记忆：{{ user.memoryConsent ? '开启' : '关闭' }}</button>
        <button v-if="user.memoryConsent" class="link danger" @click="clearMemory">清除记忆</button>
        <button class="link" @click="logout">退出</button>
      </div>
    </aside>

    <section class="workspace">
      <header>
        <div><p class="eyebrow">GUIDED DISCOVERY</p><h1>{{ current ? `${current.taskType} 提示词` : '开始一次需求访谈' }}</h1></div>
        <span v-if="current" class="status">{{ current.status }}</span>
      </header>

      <div v-if="!current" class="empty-state">
        <h2>选择任务类型并创建新需求</h2>
        <p>从一句不完整的想法开始即可。Agent 会一次只问一个最关键的问题。</p>
      </div>

      <template v-else>
        <div class="conversation">
          <article v-for="(item, index) in messages" :key="index" :class="['message', item.role]">{{ item.text }}</article>
        </div>
        <section v-if="current.requirements" class="requirements">
          <h3>当前需求摘要</h3>
          <div class="chips"><span v-for="(value, key) in current.requirements.common" :key="key"><b>{{ key }}</b> {{ value }}</span></div>
        </section>
        <div class="composer">
          <textarea v-model="message" rows="3" placeholder="补充需求、纠正信息，或说“这项我不确定，先跳过”…" @keydown.ctrl.enter="sendTurn" />
          <button class="primary" :disabled="busy || !message.trim()" @click="sendTurn">{{ busy ? '处理中…' : '发送' }}</button>
          <button v-if="canConfirm" class="confirm" :disabled="busy" @click="confirmRequirements">确认并生成三个版本</button>
        </div>

        <section v-if="streamingCandidateList.length" class="candidates streaming" aria-live="polite">
          <article v-for="candidate in streamingCandidateList" :key="candidate.variant" class="candidate">
            <div class="candidate-head">
              <div><p class="eyebrow">{{ candidate.variant }}</p><h3>{{ candidate.completed ? '生成完成' : `正在生成 · 第 ${candidate.attempt} 次尝试` }}</h3></div>
              <span>{{ candidate.completed ? '已校验' : '流式输出' }}</span>
            </div>
            <pre>{{ candidate.content || '正在等待模型返回…' }}<span v-if="!candidate.completed" class="cursor">▋</span></pre>
          </article>
        </section>

        <section v-if="current.candidates?.length" class="candidates">
          <article v-for="candidate in current.candidates" :key="candidate.id" :class="['candidate', { selected: current.selectedCandidateId === candidate.id }]">
            <div class="candidate-head"><div><p class="eyebrow">{{ candidate.variant }}</p><h3>{{ candidate.title }}</h3></div><span>{{ candidate.tradeoff }}</span></div>
            <pre>{{ candidate.content }}</pre>
            <div class="actions"><button @click="copy(candidate.content)">复制</button><button @click="selectCandidate(candidate)">选择此版本</button></div>
          </article>
          <div v-if="current.selectedCandidateId" class="export-actions">
            <button @click="downloadPrompt(current.id, 'markdown')">导出 Markdown</button>
            <button @click="downloadPrompt(current.id, 'json')">导出 JSON</button>
          </div>
        </section>
      </template>

      <section v-if="isAdmin" class="admin-panel">
        <div><p class="eyebrow">ADMIN</p><h2>知识库</h2><p class="muted">支持 Markdown、TXT、PDF，单文件不超过 20 MB。</p></div>
        <div class="upload"><input type="file" accept=".md,.markdown,.txt,.pdf" @change="adminFile = $event.target.files[0]" /><button @click="uploadKnowledge">上传并索引</button></div>
        <ul><li v-for="doc in adminDocuments" :key="doc.id"><span>{{ doc.filename }}</span><b>{{ doc.status }}</b></li></ul>
      </section>
      <p v-if="notice" class="notice">{{ notice }}</p><p v-if="error" class="error floating">{{ error }}</p>
    </section>
  </div>
</template>
