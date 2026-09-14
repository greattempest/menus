import { reactive } from 'vue'

const state = reactive({
  user: JSON.parse(window.localStorage.getItem('menus-user') || 'null'),
  permissions: new Set(JSON.parse(window.localStorage.getItem('menus-permissions') || '[]')),
})

const save = (data) => {
  state.user = data.user
  state.permissions = new Set(data.permissions || [])
  window.localStorage.setItem('menus-user', JSON.stringify(state.user))
  window.localStorage.setItem('menus-permissions', JSON.stringify([...state.permissions]))
}

export const auth = {
  state,
  isLoggedIn: () => Boolean(state.user),
  hasPermission: (permission) => state.permissions.has(permission),
  login: async (username, password) => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username, password }),
    })
    if (!response.ok) {
      let message = '用户名或密码错误'
      try {
        message = (await response.json())?.message || message
      } catch {
        // 后端可能返回纯文本错误
      }
      throw new Error(message)
    }
    const data = await response.json()
    save(data)
  },
  load: async () => {
    try {
      const response = await fetch('/api/auth/me', { credentials: 'include' })
      if (!response.ok) {
        auth.clear()
        return false
      }
      save(await response.json())
      return true
    } catch {
      auth.clear()
      return false
    }
  },
  logout: async () => {
    try {
      await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
    } finally {
      auth.clear()
    }
  },
  clear: () => {
    state.user = null
    state.permissions = new Set()
    window.localStorage.removeItem('menus-user')
    window.localStorage.removeItem('menus-permissions')
  },
}