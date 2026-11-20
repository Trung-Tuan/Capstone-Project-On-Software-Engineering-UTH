import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { POST_CATEGORIES, CAMPUS_LOCATIONS, ROUTES } from '@constants/index';
import { getPostById, updatePostStatus, CURRENT_USER_ID } from '@services/postService';
import type { Post } from '@types/index';
import styles from './PostDetailPage.module.css';

export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [post, setPost] = useState<Post | null>(null);
  const [activeSlide, setActiveSlide] = useState(0);
  const [isLightboxOpen, setIsLightboxOpen] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (id) {
      const found = getPostById(id);
      if (found) {
        setPost(found);
      }
    }
  }, [id]);

  if (!post) {
    return (
      <div className="container" style={{ padding: 'var(--space-16) 0', textAlign: 'center' }}>
        <h2 style={{ fontSize: 'var(--font-size-2xl)', marginBottom: 'var(--space-3)' }}>
          Không tìm thấy bài đăng
        </h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: 'var(--space-6)' }}>
          Bài đăng này có thể đã bị xóa hoặc đường dẫn không chính xác.
        </p>
        <Link to={ROUTES.POSTS} className={styles.backBtn}>
          ← Quay lại danh sách bài đăng
        </Link>
      </div>
    );
  }

  const images = post.imageUrls || [];
  const hasImages = images.length > 0;
  const isAuthor = post.authorId === CURRENT_USER_ID;
  const isResolved = post.status === 'RESOLVED';
  const isLost = post.type === 'LOST';

  const categoryLabel = POST_CATEGORIES.find(c => c.value === post.category)?.label || post.category;
  const locationLabel = CAMPUS_LOCATIONS.find(l => l.value === post.location)?.label || post.location;

  const nextSlide = () => {
    setActiveSlide(prev => (prev + 1) % images.length);
  };

  const prevSlide = () => {
    setActiveSlide(prev => (prev - 1 + images.length) % images.length);
  };

  const handleShare = () => {
    navigator.clipboard.writeText(window.location.href);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleToggleResolved = () => {
    const newStatus = isResolved ? 'ACTIVE' : 'RESOLVED';
    const updated = updatePostStatus(post.id, newStatus);
    if (updated) {
      setPost(updated);
    }
  };

  const formatDate = (dateStr: string) => {
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className={styles.page}>
      {/* ── Top Navigation / Actions ───────────────────────── */}
      <div className={styles.topNav}>
        <button
          type="button"
          className={styles.backBtn}
          onClick={() => navigate(-1)}
          aria-label="Quay lại"
        >
          ← Quay lại
        </button>

        <div className={styles.topActions}>
          <button
            type="button"
            className={styles.shareBtn}
            onClick={handleShare}
            aria-label="Chia sẻ bài viết"
          >
            <span>{copied ? '✅ Đã chép link' : '🔗 Chia sẻ'}</span>
          </button>

          {isAuthor && (
            <Link
              to={ROUTES.MY_POSTS}
              className={styles.btnEditAuthor}
              id="btn-manage-my-post"
            >
              ✏️ Quản lý bài này
            </Link>
          )}
        </div>
      </div>

      {/* ── Thông báo bài đã nhận lại ─────────────────────── */}
      {isResolved && (
        <div className={styles.resolvedBanner} role="status">
          <span className={styles.resolvedIcon}>🎉</span>
          <div>
            <div className={styles.resolvedTitle}>ĐỒ VẬT ĐÃ ĐƯỢC NHẬN LẠI THÀNH CÔNG!</div>
            <div className={styles.resolvedDesc}>
              Chủ nhân đã xác nhận nhận lại đồ vật an toàn qua hệ thống Lost &amp; Found UTH.
            </div>
          </div>
        </div>
      )}

      {/* ── Grid chi tiết ─────────────────────────────────── */}
      <div className={styles.detailGrid}>
        {/* ── Cột trái: SLIDE ẢNH ───────────────────────────── */}
        <div className={styles.sliderCard}>
          <div
            className={styles.sliderWrapper}
            onClick={() => hasImages && setIsLightboxOpen(true)}
            title={hasImages ? 'Nhấn để phóng to ảnh' : undefined}
          >
            {hasImages ? (
              <>
                <img
                  src={images[activeSlide]}
                  alt={`${post.title} - ảnh ${activeSlide + 1}`}
                  className={styles.mainSlideImg}
                />

                {/* Badge đếm số ảnh */}
                <div className={styles.counterBadge}>
                  📸 {activeSlide + 1} / {images.length}
                </div>

                <div className={styles.zoomHintBadge}>
                  <span>🔍</span> Phóng to
                </div>

                {/* Nút chuyển ảnh Trước / Sau */}
                {images.length > 1 && (
                  <>
                    <button
                      type="button"
                      className={`${styles.sliderNavBtn} ${styles.prevBtn}`}
                      onClick={e => {
                        e.stopPropagation();
                        prevSlide();
                      }}
                      aria-label="Ảnh trước"
                    >
                      ‹
                    </button>
                    <button
                      type="button"
                      className={`${styles.sliderNavBtn} ${styles.nextBtn}`}
                      onClick={e => {
                        e.stopPropagation();
                        nextSlide();
                      }}
                      aria-label="Ảnh kế tiếp"
                    >
                      ›
                    </button>

                    {/* Dấu chấm chỉ số */}
                    <div className={styles.dotsRow}>
                      {images.map((_, idx) => (
                        <button
                          key={idx}
                          type="button"
                          className={`${styles.dot} ${idx === activeSlide ? styles.dotActive : ''}`}
                          onClick={e => {
                            e.stopPropagation();
                            setActiveSlide(idx);
                          }}
                          aria-label={`Tới ảnh ${idx + 1}`}
                        />
                      ))}
                    </div>
                  </>
                )}
              </>
            ) : (
              <div className={styles.slidePlaceholder}>
                <span>📦</span>
                <span className={styles.slidePlaceholderText}>Bài đăng không đính kèm ảnh</span>
              </div>
            )}
          </div>

          {/* Dải thumbnail xem nhanh */}
          {images.length > 1 && (
            <div className={styles.thumbnailsStrip}>
              {images.map((img, idx) => (
                <button
                  key={idx}
                  type="button"
                  className={`${styles.thumbBtn} ${idx === activeSlide ? styles.thumbBtnActive : ''}`}
                  onClick={() => setActiveSlide(idx)}
                >
                  <img src={img} alt={`Thumbnail ${idx + 1}`} className={styles.thumbImg} />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* ── Cột phải: THÔNG TIN CHI TIẾT ──────────────────── */}
        <div className={styles.infoColumn}>
          <div className={styles.titleCard}>
            {/* Hàng nhãn */}
            <div className={styles.badgesRow}>
              <span className={isLost ? styles.typeBadgeLost : styles.typeBadgeFound}>
                {isLost ? '🚨 Mất đồ' : '🎁 Nhặt được'}
              </span>

              <span className={isResolved ? styles.statusBadgeResolved : styles.statusBadgeActive}>
                {isResolved ? '🔵 Đã nhận lại' : (isLost ? '🟢 Đang tìm kiếm' : '🟢 Chờ người nhận')}
              </span>

              <span className={styles.categoryTag}>
                📁 {categoryLabel}
              </span>
            </div>

            <h1 className={styles.postTitle}>{post.title}</h1>

            {/* Vị trí & Thời gian */}
            <div className={styles.metaBox}>
              <div className={styles.metaItem}>
                <span className={styles.metaItemIcon}>📍</span>
                <div>
                  <div className={styles.metaItemLabel}>Vị trí tại UTH</div>
                  <div className={styles.metaItemValue}>{locationLabel}</div>
                </div>
              </div>

              <div className={styles.metaItem}>
                <span className={styles.metaItemIcon}>📅</span>
                <div>
                  <div className={styles.metaItemLabel}>Thời gian xảy ra</div>
                  <div className={styles.metaItemValue}>{formatDate(post.date)}</div>
                </div>
              </div>

              <div className={styles.metaItem}>
                <span className={styles.metaItemIcon}>🕒</span>
                <div>
                  <div className={styles.metaItemLabel}>Ngày đăng tin</div>
                  <div className={styles.metaItemValue}>{formatDate(post.createdAt)}</div>
                </div>
              </div>
            </div>

            {/* Thông tin người đăng */}
            <div className={styles.authorBox}>
              <div className={styles.authorLeft}>
                {post.author?.avatarUrl ? (
                  <img
                    src={post.author.avatarUrl}
                    alt={post.author.name}
                    className={styles.authorAvatar}
                  />
                ) : (
                  <div className={styles.authorAvatarFallback}>
                    {post.author?.name ? post.author.name.charAt(0).toUpperCase() : 'U'}
                  </div>
                )}
                <div>
                  <div className={styles.authorName}>{post.author?.name || 'Người dùng UTH'}</div>
                  <div className={styles.authorRole}>Sinh viên UTH</div>
                </div>
              </div>
              <span className={styles.authorTag}>
                {isAuthor ? 'Bài của bạn' : 'Tác giả'}
              </span>
            </div>

            {/* Hàng nút hành động */}
            <div className={styles.actionsBar}>
              {isAuthor ? (
                <div className={styles.authorActionGroup}>
                  <button
                    type="button"
                    className={isResolved ? styles.btnReopen : styles.btnMarkResolved}
                    onClick={handleToggleResolved}
                    id="btn-detail-toggle-resolved"
                  >
                    {isResolved ? '🔄 Mở lại tin' : '✅ Đánh dấu ĐÃ NHẬN LẠI'}
                  </button>
                </div>
              ) : (
                <Link
                  to={`/claims/${post.id}`}
                  className={styles.btnClaim}
                  id="btn-detail-claim"
                >
                  <span>📬</span>
                  <span>Gửi yêu cầu nhận lại đồ (Claim)</span>
                </Link>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ── Mô tả chi tiết ─────────────────────────────────── */}
      <div className={styles.descCard} style={{ marginTop: 'var(--space-6)' }}>
        <h2 className={styles.descTitle}>
          <span>📝</span>
          <span>Mô tả chi tiết đồ vật</span>
        </h2>
        <div className={styles.descContent}>
          {post.description}
        </div>

        {/* Khuyến cáo an toàn thông tin cá nhân */}
        <div className={styles.securityNote}>
          <span>🛡️</span>
          <span>
            <strong>Bảo vệ an toàn thông tin:</strong> Nếu đồ vật là giấy tờ hoặc ví tiền, vui lòng kiểm tra kỹ giấy tờ tùy thân hoặc các đặc điểm nhận dạng riêng biệt khi giao nhận để đảm bảo giao đúng chính chủ. Không chụp rõ các số căn cước, số thẻ ngân hàng.
          </span>
        </div>
      </div>

      {/* ── Modal phóng to toàn màn hình (Lightbox) ─────────── */}
      {isLightboxOpen && hasImages && (
        <div
          className={styles.lightboxOverlay}
          onClick={() => setIsLightboxOpen(false)}
          role="dialog"
          aria-modal="true"
        >
          <button
            type="button"
            className={styles.lightboxClose}
            onClick={() => setIsLightboxOpen(false)}
            aria-label="Đóng phóng to"
          >
            ✕
          </button>
          <img
            src={images[activeSlide]}
            alt={post.title}
            className={styles.lightboxImg}
            onClick={e => e.stopPropagation()}
          />
        </div>
      )}
    </div>
  );
}
