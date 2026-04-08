const apiBase = '/admin/api'

function toJsonOrEmpty(res) {
  return res.json().catch(() => ({}))
}

export async function apiFetch(path, { method = 'GET', body } = {}) {
  const token = localStorage.getItem('adminToken')
  const headers = {}

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (token) {
    headers['X-Admin-Token'] = token
  }

  const res = await fetch(`${apiBase}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  })

  const data = await toJsonOrEmpty(res)
  return { res, data }
}

export async function login(username, password) {
  const res = await fetch(`${apiBase}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })
  const data = await toJsonOrEmpty(res)
  return { res, data }
}

