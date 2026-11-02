import { Outlet } from 'react-router-dom';
import Header from '@components/common/Header/Header';
import { APP_NAME, ROUTES } from '@constants/index';
import styles from './MainLayout.module.css';

export default function MainLayout() {
  return (
    <div className={styles.layout}>
      <Header />

      <main id="main-content" className={styles.main} role="main">
        <Outlet />
      </main>

      <footer className={styles.footer} role="contentinfo">
        <div className={styles.footerInner}>
          <div className={styles.footerLogo}>{APP_NAME}</div>
          <p className={styles.footerText}>
            Hệ thống quản lý đồ thất lạc – Trường Đại học Giao thông Vận tải TP.HCM
          </p>
          <nav className={styles.footerLinks} aria-label="Footer navigation">
            <a href={ROUTES.POSTS} className={styles.footerLink}>Danh sách bài đăng</a>
            <a href={ROUTES.LOGIN} className={styles.footerLink}>Đăng nhập</a>
          </nav>
          <p className={styles.footerText}>
            © {new Date().getFullYear()} UTH – Lost &amp; Found. All rights reserved.
          </p>
        </div>
      </footer>
    </div>
  );
}
