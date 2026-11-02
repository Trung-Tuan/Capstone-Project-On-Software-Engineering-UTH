import { Link } from 'react-router-dom';
import { ROUTES } from '@constants/index';
import styles from './NotFoundPage.module.css';

export default function NotFoundPage() {
  return (
    <div className={styles.wrapper}>
      <div className={styles.orb} aria-hidden="true" />
      <div className={styles.content}>
        <div className={styles.code} aria-hidden="true">404</div>
        <h1 className={styles.title}>Trang không tìm thấy</h1>
        <p className={styles.desc}>
          Trang bạn đang tìm kiếm không tồn tại hoặc đã bị xóa.
        </p>
        <div className={styles.actions}>
          <Link to={ROUTES.HOME} className={styles.btnPrimary} id="404-home-btn">
            🏠 Về trang chủ
          </Link>
          <Link to={ROUTES.POSTS} className={styles.btnSecondary} id="404-posts-btn">
            📋 Xem bài đăng
          </Link>
        </div>
      </div>
    </div>
  );
}
