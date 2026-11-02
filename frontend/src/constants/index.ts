// ── API ──────────────────────────────────────────────────
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';
export const APP_NAME = import.meta.env.VITE_APP_NAME || 'Lost & Found UTH';

// ── Storage keys ─────────────────────────────────────────
export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'lnf_access_token',
  REFRESH_TOKEN: 'lnf_refresh_token',
  USER: 'lnf_user',
} as const;

// ── Routes ───────────────────────────────────────────────
export const ROUTES = {
  HOME:         '/',
  POSTS:        '/posts',
  POST_DETAIL:  '/posts/:id',
  POST_NEW:     '/posts/new',
  LOGIN:        '/login',
  CLAIMS:       '/claims/:postId',
  MY_CLAIMS:    '/my-claims',
  PROFILE:      '/profile',
  ADMIN:        '/admin',
  ADMIN_POSTS:  '/admin/posts',
  ADMIN_CLAIMS: '/admin/claims',
  ADMIN_USERS:  '/admin/users',
  NOT_FOUND:    '*',
} as const;

// ── Post categories ───────────────────────────────────────
export const POST_CATEGORIES = [
  { value: 'electronics',   label: 'Đồ điện tử' },
  { value: 'documents',     label: 'Giấy tờ, tài liệu' },
  { value: 'clothing',      label: 'Quần áo, phụ kiện' },
  { value: 'bags',          label: 'Túi xách, balo' },
  { value: 'keys',          label: 'Chìa khóa' },
  { value: 'wallet',        label: 'Ví tiền' },
  { value: 'jewelry',       label: 'Đồ trang sức' },
  { value: 'books',         label: 'Sách, vở' },
  { value: 'stationery',    label: 'Văn phòng phẩm' },
  { value: 'sports',        label: 'Đồ thể thao' },
  { value: 'other',         label: 'Khác' },
] as const;

// ── Campus locations ──────────────────────────────────────
export const CAMPUS_LOCATIONS = [
  { value: 'library',       label: 'Thư viện' },
  { value: 'canteen',       label: 'Căng tin' },
  { value: 'parking',       label: 'Bãi xe' },
  { value: 'classroom-a',   label: 'Tòa nhà A' },
  { value: 'classroom-b',   label: 'Tòa nhà B' },
  { value: 'classroom-c',   label: 'Tòa nhà C' },
  { value: 'lab',           label: 'Phòng thí nghiệm' },
  { value: 'sports-ground', label: 'Sân thể thao' },
  { value: 'admin-office',  label: 'Văn phòng hành chính' },
  { value: 'entrance',      label: 'Cổng trường' },
  { value: 'other',         label: 'Khác' },
] as const;

// ── Upload constraints ────────────────────────────────────
export const UPLOAD_CONSTRAINTS = {
  MAX_FILES:      3,
  MAX_FILE_SIZE:  5 * 1024 * 1024,  // 5 MB
  ACCEPTED_TYPES: ['image/jpeg', 'image/png', 'image/webp', 'image/gif'],
  ACCEPTED_EXTENSIONS: '.jpg,.jpeg,.png,.webp,.gif',
} as const;

// ── Pagination ────────────────────────────────────────────
export const PAGINATION = {
  DEFAULT_PAGE:  1,
  DEFAULT_LIMIT: 12,
} as const;

// ── Toast durations ───────────────────────────────────────
export const TOAST_DURATION = {
  SHORT:  3000,
  MEDIUM: 5000,
  LONG:   8000,
} as const;
