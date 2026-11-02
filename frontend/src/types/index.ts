// ── Auth types ───────────────────────────────────────────
export type UserRole = 'USER' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  name: string;
  avatarUrl?: string;
  role: UserRole;
  createdAt: string;
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

// ── Post types ───────────────────────────────────────────
export type PostType = 'LOST' | 'FOUND';
export type PostStatus = 'ACTIVE' | 'RESOLVED' | 'CLOSED';

export interface Post {
  id: string;
  type: PostType;
  title: string;
  description: string;
  category: string;
  location: string;
  date: string;
  imageUrls: string[];
  status: PostStatus;
  authorId: string;
  author: Pick<User, 'id' | 'name' | 'avatarUrl'>;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePostDto {
  type: PostType;
  title: string;
  description: string;
  category: string;
  location: string;
  date: string;
}

export interface PostFilters {
  type?: PostType;
  category?: string;
  location?: string;
  keyword?: string;
  dateFrom?: string;
  dateTo?: string;
  status?: PostStatus;
  page?: number;
  limit?: number;
}

// ── Claim types ──────────────────────────────────────────
export type ClaimStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface Claim {
  id: string;
  postId: string;
  post?: Pick<Post, 'id' | 'title' | 'type'>;
  claimantId: string;
  claimant?: Pick<User, 'id' | 'name' | 'avatarUrl'>;
  message: string;
  contactInfo: string;
  status: ClaimStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateClaimDto {
  postId: string;
  message: string;
  contactInfo: string;
}

// ── API types ────────────────────────────────────────────
export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface ApiError {
  message: string;
  status: number;
  errors?: Record<string, string[]>;
}

// ── UI types ─────────────────────────────────────────────
export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
}

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
}
