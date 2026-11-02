// ── Auth endpoints ────────────────────────────────────────
export const AUTH_ENDPOINTS = {
  GOOGLE_LOGIN:  '/auth/google',
  REFRESH_TOKEN: '/auth/refresh',
  LOGOUT:        '/auth/logout',
  ME:            '/auth/me',
} as const;

// ── Post endpoints ────────────────────────────────────────
export const POST_ENDPOINTS = {
  LIST:    '/posts',
  CREATE:  '/posts',
  DETAIL:  (id: string) => `/posts/${id}`,
  UPDATE:  (id: string) => `/posts/${id}`,
  DELETE:  (id: string) => `/posts/${id}`,
  IMAGES:  (id: string) => `/posts/${id}/images`,
} as const;

// ── Claim endpoints ───────────────────────────────────────
export const CLAIM_ENDPOINTS = {
  CREATE:  '/claims',
  MY_LIST: '/claims/me',
  DETAIL:  (id: string) => `/claims/${id}`,
} as const;

// ── Admin endpoints ───────────────────────────────────────
export const ADMIN_ENDPOINTS = {
  DASHBOARD:    '/admin/dashboard',
  POSTS:        '/admin/posts',
  POST_DETAIL:  (id: string) => `/admin/posts/${id}`,
  CLAIMS:       '/admin/claims',
  CLAIM_DETAIL: (id: string) => `/admin/claims/${id}`,
  USERS:        '/admin/users',
  USER_DETAIL:  (id: string) => `/admin/users/${id}`,
} as const;

// ── Upload endpoints ──────────────────────────────────────
export const UPLOAD_ENDPOINTS = {
  IMAGE: '/upload/image',
} as const;
