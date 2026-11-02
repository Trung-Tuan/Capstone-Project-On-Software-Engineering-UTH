import { Outlet, Navigate, Link, NavLink } from 'react-router-dom';
import { ROUTES } from '@constants/index';
import styles from './AdminLayout.module.css';

// TODO: replace with useAuth() in Phase 2
const isAuthenticated = false;
const isAdmin = false;

const ADMIN_NAV = [
  { to: ROUTES.ADMIN,        label: 'Tổng quan',    icon: '📊', end: true },
  { to: ROUTES.ADMIN_POSTS,  label: 'Bài đăng',     icon: '📋' },
  { to: ROUTES.ADMIN_CLAIMS, label: 'Yêu cầu Claim', icon: '📬' },
  { to: ROUTES.ADMIN_USERS,  label: 'Người dùng',   icon: '👥' },
] as const;

export default function AdminLayout() {
  if (!isAuthenticated) return <Navigate to={ROUTES.LOGIN} replace />;
  if (!isAdmin)          return <Navigate to={ROUTES.HOME}  replace />;

  return (
    <div className={styles.layout}>
      {/* ── Sidebar ── */}
      <aside className={styles.sidebar} aria-label="Admin navigation">
        <div className={styles.sidebarHeader}>
          <Link to={ROUTES.HOME} className={styles.sidebarLogo}>
            <div className={styles.sidebarLogoIcon}>🔍</div>
            <div>
              <div className={styles.sidebarLogoName}>Lost &amp; Found</div>
              <div className={styles.sidebarLogoSub}>Admin Panel</div>
            </div>
          </Link>
        </div>

        <nav className={styles.sidebarNav}>
          {ADMIN_NAV.map(item => (
            <NavLink
              key={item.to}
              to={item.to}
              end={'end' in item ? item.end : false}
              className={({ isActive }) =>
                `${styles.sidebarLink} ${isActive ? styles.active : ''}`
              }
            >
              <span className={styles.sidebarLinkIcon} aria-hidden="true">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className={styles.sidebarFooter}>
          <Link to={ROUTES.HOME} className={styles.sidebarBackLink}>
            <span aria-hidden="true">←</span> Về trang chủ
          </Link>
        </div>
      </aside>

      {/* ── Main ── */}
      <div className={styles.main}>
        <div className={styles.content}>
          <Outlet />
        </div>
      </div>
    </div>
  );
}
