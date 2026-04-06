export function getAdminToken() {
  return localStorage.getItem('adminToken')
}

export function setAdminToken(token) {
  if (token) localStorage.setItem('adminToken', token)
}

export function clearAdminToken() {
  localStorage.removeItem('adminToken')
}

export function requireAdminAuth(to) {
  const token = getAdminToken()
  if (!token) {
    return '/login'
  }
  return true
}
