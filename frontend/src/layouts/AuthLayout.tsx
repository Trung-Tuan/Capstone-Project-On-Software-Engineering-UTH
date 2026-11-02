import { Outlet, Navigate } from 'react-router-dom';
import { ROUTES } from '@constants/index';
import styles from './AuthLayout.module.css';

// TODO: replace with useAuth() in Phase 2
const isAuthenticated = false;

export default function AuthLayout() {
  // Redirect to home if already logged in
  if (isAuthenticated) {
    return <Navigate to={ROUTES.HOME} replace />;
  }

  return (
    <div className={styles.layout}>
      <div className={styles.background} aria-hidden="true">
        <div className={styles.orb1} />
        <div className={styles.orb2} />
        <div className={styles.grid} />
      </div>

      <div className={styles.card}>
        <div className={styles.logo}>
          <div className={styles.logoIcon}>🔍</div>
          <div className={styles.logoText}>
            <span className={styles.logoName}>Lost &amp; Found</span>
            <span className={styles.logoSub}>UTH – Đồ thất lạc</span>
          </div>
        </div>
        <Outlet />
      </div>
    </div>
  );
}
