import { useState, useEffect, useRef, useCallback } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { ROUTES } from '@constants/index';
import styles from './Header.module.css';

// ── Temporary auth stub (replace with useAuth hook in Phase 2) ──
interface MockUser {
  name: string;
  email: string;
  avatarUrl?: string;
  role: 'USER' | 'ADMIN';
}
const MOCK_USER: MockUser | null = null; // set to null = not logged in

// ── Nav items ─────────────────────────────────────────────
const NAV_ITEMS = [
  { to: ROUTES.POSTS,    label: 'Danh sách',  icon: '📋' },
  { to: ROUTES.MY_CLAIMS, label: 'Yêu cầu',  icon: '📬' },
] as const;

export default function Header() {
  const [scrolled,    setScrolled]   = useState(false);
  const [mobileOpen,  setMobileOpen] = useState(false);
  const [dropdownOpen, setDropdown]  = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const navigate    = useNavigate();

  // Close dropdown when clicking outside
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdown(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // Track scroll for header shadow
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  // Lock body scroll when mobile menu is open
  useEffect(() => {
    document.body.style.overflow = mobileOpen ? 'hidden' : '';
    return () => { document.body.style.overflow = ''; };
  }, [mobileOpen]);

  const closeMobile = useCallback(() => setMobileOpen(false), []);

  const handleLogout = () => {
    setDropdown(false);
    // TODO: call authService.logout() in Phase 2
    navigate(ROUTES.LOGIN);
  };

  const user = MOCK_USER;
  const initials = user?.name
    ? user.name.split(' ').map(w => w[0]).slice(-2).join('').toUpperCase()
    : '';

  return (
    <>
      <header className={`${styles.header} ${scrolled ? styles.scrolled : ''}`} role="banner">
        <div className={styles.inner}>
          {/* ── Logo ── */}
          <Link to={ROUTES.HOME} className={styles.logo} aria-label="Lost & Found UTH - Trang chủ">
            <div className={styles.logoIcon} aria-hidden="true">🔍</div>
            <div className={styles.logoText}>
              <span className={styles.logoName}>Lost & Found</span>
              <span className={styles.logoSub}>UTH – Đồ thất lạc</span>
            </div>
          </Link>

          {/* ── Desktop nav ── */}
          <nav className={styles.nav} aria-label="Điều hướng chính">
            {NAV_ITEMS.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `${styles.navLink} ${isActive ? styles.active : ''}`
                }
              >
                <span aria-hidden="true">{item.icon}</span>
                {item.label}
              </NavLink>
            ))}
          </nav>

          {/* ── Right section ── */}
          <div className={styles.right}>
            {user ? (
              <>
                <Link
                  to={ROUTES.POST_NEW}
                  className={styles.btnCreate}
                  id="header-create-post-btn"
                >
                  <span aria-hidden="true">✏️</span>
                  <span>Đăng bài</span>
                </Link>

                {/* Avatar + Dropdown */}
                <div className={styles.dropdownWrapper} ref={dropdownRef}>
                  <button
                    className={styles.avatarBtn}
                    onClick={() => setDropdown(v => !v)}
                    aria-expanded={dropdownOpen}
                    aria-haspopup="menu"
                    aria-label={`Tài khoản của ${user.name}`}
                    id="header-user-menu-btn"
                  >
                    {user.avatarUrl ? (
                      <img
                        src={user.avatarUrl}
                        alt={user.name}
                        className={styles.avatarImg}
                      />
                    ) : (
                      <div className={styles.avatarFallback} aria-hidden="true">
                        {initials}
                      </div>
                    )}
                  </button>

                  {dropdownOpen && (
                    <div
                      className={styles.dropdown}
                      role="menu"
                      aria-labelledby="header-user-menu-btn"
                    >
                      <div className={styles.dropdownHeader}>
                        <div className={styles.dropdownName}>{user.name}</div>
                        <div className={styles.dropdownEmail}>{user.email}</div>
                        <span className={`${styles.dropdownRole} ${user.role === 'ADMIN' ? styles.admin : ''}`}>
                          {user.role === 'ADMIN' ? '⚡ Admin' : '👤 Người dùng'}
                        </span>
                      </div>

                      <div className={styles.dropdownMenu}>
                        <Link
                          to={ROUTES.PROFILE}
                          className={styles.dropdownItem}
                          role="menuitem"
                          onClick={() => setDropdown(false)}
                          id="header-profile-link"
                        >
                          <span aria-hidden="true">👤</span> Hồ sơ cá nhân
                        </Link>
                        <Link
                          to={ROUTES.MY_CLAIMS}
                          className={styles.dropdownItem}
                          role="menuitem"
                          onClick={() => setDropdown(false)}
                          id="header-my-claims-link"
                        >
                          <span aria-hidden="true">📬</span> Yêu cầu của tôi
                        </Link>

                        {user.role === 'ADMIN' && (
                          <>
                            <div className={styles.dropdownDivider} />
                            <Link
                              to={ROUTES.ADMIN}
                              className={styles.dropdownItem}
                              role="menuitem"
                              onClick={() => setDropdown(false)}
                              id="header-admin-link"
                            >
                              <span aria-hidden="true">⚙️</span> Quản trị viên
                            </Link>
                          </>
                        )}

                        <div className={styles.dropdownDivider} />
                        <button
                          className={`${styles.dropdownItem} ${styles.danger}`}
                          role="menuitem"
                          onClick={handleLogout}
                          id="header-logout-btn"
                        >
                          <span aria-hidden="true">🚪</span> Đăng xuất
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <Link
                to={ROUTES.LOGIN}
                className={styles.btnLogin}
                id="header-login-btn"
              >
                <span aria-hidden="true">🔑</span> Đăng nhập
              </Link>
            )}

            {/* Hamburger */}
            <button
              className={`${styles.hamburger} ${mobileOpen ? styles.open : ''}`}
              onClick={() => setMobileOpen(v => !v)}
              aria-expanded={mobileOpen}
              aria-controls="mobile-nav"
              aria-label={mobileOpen ? 'Đóng menu' : 'Mở menu'}
              id="header-hamburger-btn"
            >
              <span />
              <span />
              <span />
            </button>
          </div>
        </div>
      </header>

      {/* ── Mobile Nav Overlay ── */}
      <nav
        id="mobile-nav"
        className={`${styles.mobileNav} ${mobileOpen ? styles.open : ''}`}
        aria-label="Menu di động"
        aria-hidden={!mobileOpen}
      >
        {NAV_ITEMS.map(item => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `${styles.mobileNavLink} ${isActive ? styles.active : ''}`
            }
            onClick={closeMobile}
          >
            <span aria-hidden="true">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}

        {user ? (
          <>
            <div className={styles.mobileNavDivider} />
            <NavLink
              to={ROUTES.POST_NEW}
              className={styles.mobileNavLink}
              onClick={closeMobile}
            >
              <span aria-hidden="true">✏️</span> Đăng bài mới
            </NavLink>
            <NavLink
              to={ROUTES.PROFILE}
              className={styles.mobileNavLink}
              onClick={closeMobile}
            >
              <span aria-hidden="true">👤</span> Hồ sơ cá nhân
            </NavLink>
            {user.role === 'ADMIN' && (
              <NavLink
                to={ROUTES.ADMIN}
                className={styles.mobileNavLink}
                onClick={closeMobile}
              >
                <span aria-hidden="true">⚙️</span> Quản trị viên
              </NavLink>
            )}
            <div className={styles.mobileNavDivider} />
            <button
              className={`${styles.mobileNavLink} ${styles.danger}`}
              onClick={() => { closeMobile(); handleLogout(); }}
            >
              <span aria-hidden="true">🚪</span> Đăng xuất
            </button>
          </>
        ) : (
          <>
            <div className={styles.mobileNavDivider} />
            <NavLink
              to={ROUTES.LOGIN}
              className={styles.mobileNavLink}
              onClick={closeMobile}
            >
              <span aria-hidden="true">🔑</span> Đăng nhập
            </NavLink>
          </>
        )}
      </nav>
    </>
  );
}
