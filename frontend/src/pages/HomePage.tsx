import { Link } from 'react-router-dom';
import { ROUTES } from '@constants/index';
import styles from './HomePage.module.css';

// ── Mock recent posts (replace with API in Phase 5) ──────
const MOCK_POSTS = [
  {
    id: '1',
    type: 'LOST' as const,
    title: 'Mất ví tiền màu đen',
    description: 'Ví da màu đen, bên trong có CMND, thẻ sinh viên và tiền mặt. Mất tại khu vực căng tin trường.',
    location: 'Căng tin',
    date: '2024-01-15',
    category: 'wallet',
    emoji: '👛',
  },
  {
    id: '2',
    type: 'FOUND' as const,
    title: 'Tìm thấy laptop Dell',
    description: 'Laptop Dell màu bạc, tìm thấy tại thư viện tầng 3. Đang bảo quản tại phòng bảo vệ.',
    location: 'Thư viện',
    date: '2024-01-14',
    category: 'electronics',
    emoji: '💻',
  },
  {
    id: '3',
    type: 'LOST' as const,
    title: 'Mất chìa khóa xe máy',
    description: 'Chìa khóa xe Honda Wave màu đỏ, có móc khóa hình gấu. Mất ở khu bãi xe A.',
    location: 'Bãi xe',
    date: '2024-01-13',
    category: 'keys',
    emoji: '🔑',
  },
  {
    id: '4',
    type: 'FOUND' as const,
    title: 'Tìm thấy balo học sinh',
    description: 'Balo màu xanh navy, nhãn hiệu JanSport. Tìm thấy tại phòng học B205.',
    location: 'Tòa nhà B',
    date: '2024-01-12',
    category: 'bags',
    emoji: '🎒',
  },
  {
    id: '5',
    type: 'LOST' as const,
    title: 'Mất tai nghe Sony',
    description: 'Tai nghe không dây Sony WH-1000XM4 màu đen. Mất tại khu vực sân thể thao.',
    location: 'Sân thể thao',
    date: '2024-01-11',
    category: 'electronics',
    emoji: '🎧',
  },
  {
    id: '6',
    type: 'FOUND' as const,
    title: 'Tìm thấy thẻ sinh viên',
    description: 'Thẻ sinh viên khoa CNTT. Đang giữ tại văn phòng hành chính.',
    location: 'Văn phòng hành chính',
    date: '2024-01-10',
    category: 'documents',
    emoji: '🪪',
  },
];

const FEATURES = [
  { icon: '🔍', title: 'Tìm kiếm nhanh', desc: 'Tìm đồ vật theo tên, danh mục, địa điểm và thời gian một cách nhanh chóng.' },
  { icon: '📮', title: 'Đăng bài dễ dàng', desc: 'Đăng bài về đồ vật bị mất hoặc tìm thấy chỉ trong vài bước đơn giản.' },
  { icon: '📬', title: 'Gửi yêu cầu Claim', desc: 'Gửi yêu cầu nhận lại đồ vật và theo dõi trạng thái xử lý.' },
  { icon: '🔒', title: 'Bảo mật & An toàn', desc: 'Đăng nhập bằng tài khoản Google trường, thông tin được bảo vệ.' },
];

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
}

export default function HomePage() {
  return (
    <div>
      {/* ── Hero ── */}
      <section className={styles.hero} aria-labelledby="hero-title">
        <div className={styles.heroBackground} aria-hidden="true">
          <div className={styles.heroOrb1} />
          <div className={styles.heroOrb2} />
          <div className={styles.heroGrid} />
        </div>
        <div className={styles.heroContent}>
          <div className={styles.heroEyebrow}>
            <span>🏫</span> Trường Đại học Giao thông Vận tải TP.HCM
          </div>
          <h1 id="hero-title" className={styles.heroTitle}>
            Hệ thống quản lý{' '}
            <span className={styles.heroHighlight}>đồ thất lạc</span>{' '}
            trong trường
          </h1>
          <p className={styles.heroDesc}>
            Nền tảng kết nối sinh viên và nhân viên UTH – giúp bạn tìm lại
            đồ vật bị mất hoặc trao lại đồ cho người cần.
          </p>
          <div className={styles.heroCtas}>
            <Link to={ROUTES.POSTS} className={styles.ctaPrimary} id="hero-browse-btn">
              <span aria-hidden="true">📋</span> Xem danh sách
            </Link>
            <Link to={ROUTES.LOGIN} className={styles.ctaSecondary} id="hero-post-btn">
              <span aria-hidden="true">✏️</span> Đăng bài ngay
            </Link>
          </div>
        </div>
      </section>

      {/* ── Stats ── */}
      <div className={styles.statsBar} aria-label="Thống kê hệ thống">
        <div className={styles.statsInner}>
          {[
            { number: '1,240+', label: 'Bài đăng' },
            { number: '890+',   label: 'Đồ vật tìm lại' },
            { number: '3,500+', label: 'Người dùng' },
          ].map(stat => (
            <div key={stat.label} className={styles.statItem}>
              <span className={styles.statNumber}>{stat.number}</span>
              <span className={styles.statLabel}>{stat.label}</span>
            </div>
          ))}
        </div>
      </div>

      {/* ── Recent Posts ── */}
      <section className={styles.section} aria-labelledby="recent-posts-title">
        <div className="container">
          <div className={styles.sectionHeader}>
            <h2 id="recent-posts-title" className={styles.sectionTitle}>
              Bài đăng mới nhất
            </h2>
            <Link to={ROUTES.POSTS} className={styles.sectionLink}>
              Xem tất cả <span aria-hidden="true">→</span>
            </Link>
          </div>

          <div className={styles.postsGrid}>
            {MOCK_POSTS.map(post => (
              <Link
                key={post.id}
                to={`/posts/${post.id}`}
                className={styles.postCard}
                id={`home-post-card-${post.id}`}
              >
                <div className={styles.postCardImagePlaceholder} aria-hidden="true">
                  {post.emoji}
                </div>
                <div className={styles.postCardBody}>
                  <div className={styles.postCardMeta}>
                    <span
                      className={`badge ${post.type === 'LOST' ? 'badge-lost' : 'badge-found'}`}
                    >
                      {post.type === 'LOST' ? '😢 Đã mất' : '🎉 Tìm thấy'}
                    </span>
                  </div>
                  <h3 className={styles.postCardTitle}>{post.title}</h3>
                  <p className={styles.postCardDesc}>{post.description}</p>
                  <div className={styles.postCardFooter}>
                    <span className={styles.postCardLocation}>
                      📍 {post.location}
                    </span>
                    <span className={styles.postCardDate}>
                      🕐 {formatDate(post.date)}
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* ── Features ── */}
      <section className={styles.features} aria-labelledby="features-title">
        <div className="container">
          <div className={styles.sectionHeader} style={{ justifyContent: 'center', textAlign: 'center' }}>
            <h2 id="features-title" className={styles.sectionTitle}>
              Tại sao dùng Lost &amp; Found UTH?
            </h2>
          </div>
          <div className={styles.featuresGrid}>
            {FEATURES.map(f => (
              <div key={f.title} className={styles.featureCard}>
                <span className={styles.featureIcon} aria-hidden="true">{f.icon}</span>
                <h3 className={styles.featureTitle}>{f.title}</h3>
                <p className={styles.featureDesc}>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA ── */}
      <section className={styles.ctaSection} aria-labelledby="cta-title">
        <div className={styles.ctaSectionGlow} aria-hidden="true" />
        <div className={`${styles.ctaSectionContent} container`}>
          <h2 id="cta-title" className={styles.ctaSectionTitle}>
            Bạn vừa mất đồ?
          </h2>
          <p className={styles.ctaSectionDesc}>
            Đăng bài ngay để cộng đồng UTH giúp bạn tìm lại!
          </p>
          <div className={styles.heroCtas}>
            <Link to={ROUTES.LOGIN} className={styles.ctaPrimary} id="cta-post-btn">
              <span aria-hidden="true">✏️</span> Đăng bài ngay
            </Link>
            <Link to={ROUTES.POSTS} className={styles.ctaSecondary} id="cta-browse-btn">
              <span aria-hidden="true">🔍</span> Tìm kiếm
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
