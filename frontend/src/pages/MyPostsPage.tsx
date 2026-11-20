import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { POST_CATEGORIES, CAMPUS_LOCATIONS, ROUTES } from '@constants/index';
import {
  getMyPosts,
  updatePostStatus,
  updatePost,
  deletePost,
} from '@services/postService';
import type { Post, PostType } from '@types/index';
import styles from './MyPostsPage.module.css';

type FilterTab = 'ALL' | 'LOST' | 'FOUND' | 'RESOLVED';

export default function MyPostsPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [activeTab, setActiveTab] = useState<FilterTab>('ALL');

  // Modals state
  const [editingPost, setEditingPost] = useState<Post | null>(null);
  const [deletingPost, setDeletingPost] = useState<Post | null>(null);
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  // Load user posts
  const loadPosts = () => {
    const list = getMyPosts();
    setPosts(list);
  };

  useEffect(() => {
    loadPosts();
  }, []);

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 3000);
  };

  // Toggle resolved status
  const handleToggleResolved = (post: Post) => {
    const newStatus = post.status === 'RESOLVED' ? 'ACTIVE' : 'RESOLVED';
    const updated = updatePostStatus(post.id, newStatus);
    if (updated) {
      loadPosts();
      showToast(
        newStatus === 'RESOLVED'
          ? '🎉 Đã đánh dấu bài viết: ĐÃ NHẬN LẠI!'
          : '🔄 Đã mở lại trạng thái tìm kiếm cho bài viết.'
      );
    }
  };

  // Confirm delete
  const handleConfirmDelete = () => {
    if (!deletingPost) return;
    const success = deletePost(deletingPost.id);
    if (success) {
      loadPosts();
      showToast('🗑️ Đã xóa bài đăng thành công.');
      setDeletingPost(null);
    }
  };

  // Save edit form
  const handleSaveEdit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingPost) return;

    const formData = new FormData(e.currentTarget);
    const updated = updatePost(editingPost.id, {
      title: formData.get('title') as string,
      type: formData.get('type') as PostType,
      category: formData.get('category') as string,
      location: formData.get('location') as string,
      date: formData.get('date') as string,
      description: formData.get('description') as string,
    });

    if (updated) {
      loadPosts();
      showToast('✅ Đã cập nhật bài đăng thành công!');
      setEditingPost(null);
    }
  };

  // Filtered posts
  const filteredPosts = posts.filter(post => {
    if (activeTab === 'ALL') return true;
    if (activeTab === 'RESOLVED') return post.status === 'RESOLVED';
    if (activeTab === 'LOST') return post.type === 'LOST' && post.status !== 'RESOLVED';
    if (activeTab === 'FOUND') return post.type === 'FOUND' && post.status !== 'RESOLVED';
    return true;
  });

  const countAll = posts.length;
  const countLost = posts.filter(p => p.type === 'LOST' && p.status !== 'RESOLVED').length;
  const countFound = posts.filter(p => p.type === 'FOUND' && p.status !== 'RESOLVED').length;
  const countResolved = posts.filter(p => p.status === 'RESOLVED').length;

  return (
    <div className={styles.page}>
      {/* ── Header ─────────────────────────────────────────── */}
      <div className={styles.header}>
        <div className={styles.titleArea}>
          <h1>Bài đăng của tôi</h1>
          <p>Quản lý các tin tìm đồ và nhặt được đồ bạn đã đăng tải</p>
        </div>

        <Link to={ROUTES.POST_NEW} className={styles.btnNewPost} id="btn-create-from-myposts">
          <span>➕</span>
          <span>Đăng tin mới</span>
        </Link>
      </div>

      {/* ── Filter Tabs ─────────────────────────────────────── */}
      <div className={styles.tabsRow} role="tablist">
        <button
          type="button"
          className={`${styles.tabBtn} ${activeTab === 'ALL' ? styles.tabBtnActive : ''}`}
          onClick={() => setActiveTab('ALL')}
        >
          <span>Tất cả</span>
          <span className={styles.tabBadge}>{countAll}</span>
        </button>

        <button
          type="button"
          className={`${styles.tabBtn} ${activeTab === 'LOST' ? styles.tabBtnActive : ''}`}
          onClick={() => setActiveTab('LOST')}
        >
          <span>🚨 Mất đồ</span>
          <span className={styles.tabBadge}>{countLost}</span>
        </button>

        <button
          type="button"
          className={`${styles.tabBtn} ${activeTab === 'FOUND' ? styles.tabBtnActive : ''}`}
          onClick={() => setActiveTab('FOUND')}
        >
          <span>🎁 Nhặt được</span>
          <span className={styles.tabBadge}>{countFound}</span>
        </button>

        <button
          type="button"
          className={`${styles.tabBtn} ${activeTab === 'RESOLVED' ? styles.tabBtnActive : ''}`}
          onClick={() => setActiveTab('RESOLVED')}
        >
          <span>🔵 Đã nhận lại</span>
          <span className={styles.tabBadge}>{countResolved}</span>
        </button>
      </div>

      {/* ── Posts List ──────────────────────────────────────── */}
      {filteredPosts.length === 0 ? (
        <div className={styles.emptyState}>
          <div className={styles.emptyIcon}>📭</div>
          <div className={styles.emptyTitle}>Chưa có bài đăng nào trong mục này</div>
          <p className={styles.emptyDesc}>
            Bạn chưa đăng tin nào hoặc các tin đã được di chuyển sang mục khác.
          </p>
          <Link to={ROUTES.POST_NEW} className={styles.btnNewPost}>
            <span>✏️ Đăng bài ngay</span>
          </Link>
        </div>
      ) : (
        <div className={styles.postsList}>
          {filteredPosts.map(post => {
            const isLost = post.type === 'LOST';
            const isResolved = post.status === 'RESOLVED';
            const categoryLabel = POST_CATEGORIES.find(c => c.value === post.category)?.label || post.category;
            const locationLabel = CAMPUS_LOCATIONS.find(l => l.value === post.location)?.label || post.location;

            return (
              <div key={post.id} className={styles.postCard} id={`my-post-card-${post.id}`}>
                {/* Thumbnail */}
                <div className={styles.postThumb}>
                  {post.imageUrls && post.imageUrls.length > 0 ? (
                    <img
                      src={post.imageUrls[0]}
                      alt={post.title}
                      className={styles.thumbImg}
                    />
                  ) : (
                    <div className={styles.thumbPlaceholder}>📦</div>
                  )}
                </div>

                {/* Content */}
                <div className={styles.postBody}>
                  <div className={styles.postMetaRow}>
                    <span className={isLost ? styles.typeBadgeLost : styles.typeBadgeFound}>
                      {isLost ? '🚨 Mất đồ' : '🎁 Nhặt được'}
                    </span>

                    {isResolved ? (
                      <span className={styles.statusResolvedBadge}>🔵 Đã nhận lại</span>
                    ) : (
                      <span className={styles.statusActiveBadge}>🟢 Đang hoạt động</span>
                    )}

                    <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                      📁 {categoryLabel}
                    </span>
                  </div>

                  <h3 className={styles.postTitle}>
                    <Link to={`/posts/${post.id}`} style={{ color: 'inherit' }}>
                      {post.title}
                    </Link>
                  </h3>

                  <div className={styles.postDetailsSub}>
                    <span>📍 {locationLabel}</span>
                    <span>📅 {post.date}</span>
                    {post.imageUrls && post.imageUrls.length > 0 && (
                      <span>📸 {post.imageUrls.length} ảnh</span>
                    )}
                  </div>
                </div>

                {/* Card Actions */}
                <div className={styles.cardActions}>
                  {/* Nút đánh dấu đã nhận lại */}
                  <button
                    type="button"
                    className={isResolved ? styles.btnActionUndoResolve : styles.btnActionResolve}
                    onClick={() => handleToggleResolved(post)}
                    id={`btn-resolve-${post.id}`}
                    title={isResolved ? 'Chuyển về trạng thái đang tìm' : 'Đánh dấu đã tìm thấy/nhận lại đồ'}
                  >
                    {isResolved ? '🔄 Mở lại tin' : '✅ Đã nhận lại'}
                  </button>

                  <div className={styles.btnRow}>
                    {/* Nút chỉnh sửa */}
                    <button
                      type="button"
                      className={styles.btnActionEdit}
                      onClick={() => setEditingPost(post)}
                      id={`btn-edit-${post.id}`}
                    >
                      ✏️ Sửa
                    </button>

                    {/* Nút xóa */}
                    <button
                      type="button"
                      className={styles.btnActionDelete}
                      onClick={() => setDeletingPost(post)}
                      id={`btn-delete-${post.id}`}
                    >
                      🗑️ Xóa
                    </button>
                  </div>

                  {/* Nút xem chi tiết */}
                  <Link
                    to={`/posts/${post.id}`}
                    className={styles.btnActionView}
                    style={{ width: '100%' }}
                  >
                    Xem chi tiết →
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── MODAL CHỈNH SỬA BÀI VIẾT ───────────────────────── */}
      {editingPost && (
        <div
          className={styles.modalOverlay}
          onClick={() => setEditingPost(null)}
          role="dialog"
          aria-modal="true"
        >
          <div className={styles.modalCard} onClick={e => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2 className={styles.modalTitle}>
                <span>✏️</span>
                <span>Chỉnh sửa bài đăng</span>
              </h2>
              <button
                type="button"
                className={styles.modalCloseBtn}
                onClick={() => setEditingPost(null)}
                aria-label="Đóng"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveEdit}>
              {/* Loại bài đăng */}
              <div className={styles.formGroup}>
                <label className={styles.label}>Loại bài đăng</label>
                <select
                  name="type"
                  className={styles.select}
                  defaultValue={editingPost.type}
                >
                  <option value="LOST">🚨 Mất đồ (Tôi bị mất đồ)</option>
                  <option value="FOUND">🎁 Nhặt được (Tôi nhặt được đồ)</option>
                </select>
              </div>

              {/* Tên đồ vật */}
              <div className={styles.formGroup}>
                <label className={styles.label}>Tên đồ vật *</label>
                <input
                  name="title"
                  type="text"
                  className={styles.input}
                  defaultValue={editingPost.title}
                  required
                />
              </div>

              {/* Danh mục */}
              <div className={styles.formGroup}>
                <label className={styles.label}>Danh mục *</label>
                <select
                  name="category"
                  className={styles.select}
                  defaultValue={editingPost.category}
                  required
                >
                  {POST_CATEGORIES.map(c => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Địa điểm tại UTH */}
              <div className={styles.formGroup}>
                <label className={styles.label}>Vị trí tại UTH *</label>
                <select
                  name="location"
                  className={styles.select}
                  defaultValue={editingPost.location}
                  required
                >
                  {CAMPUS_LOCATIONS.map(l => (
                    <option key={l.value} value={l.value}>
                      {l.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Ngày */}
              <div className={styles.formGroup}>
                <label className={styles.label}>Ngày xảy ra *</label>
                <input
                  name="date"
                  type="date"
                  className={styles.input}
                  defaultValue={editingPost.date}
                  required
                />
              </div>

              {/* Mô tả chi tiết */}
              <div className={styles.formGroup}>
                <label className={styles.label}>Mô tả chi tiết</label>
                <textarea
                  name="description"
                  rows={4}
                  className={styles.textarea}
                  defaultValue={editingPost.description}
                />
              </div>

              {/* Lưu ý bảo mật */}
              <div style={{ fontSize: '11px', color: '#b45309', background: '#fef3c7', padding: '8px 12px', borderRadius: '6px' }}>
                🛡️ <strong>Lưu ý bảo mật:</strong> Không cập nhật các thông tin nhạy cảm như số CCCD, mã thẻ ngân hàng vào nội dung mô tả.
              </div>

              <div className={styles.modalFooter}>
                <button
                  type="button"
                  className={styles.btnCancel}
                  onClick={() => setEditingPost(null)}
                >
                  Hủy
                </button>
                <button type="submit" className={styles.btnSave}>
                  💾 Lưu thay đổi
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── MODAL XÁC NHẬN XÓA BÀI VIẾT ─────────────────────── */}
      {deletingPost && (
        <div
          className={styles.modalOverlay}
          onClick={() => setDeletingPost(null)}
          role="dialog"
          aria-modal="true"
        >
          <div
            className={`${styles.modalCard} ${styles.deleteConfirmCard}`}
            onClick={e => e.stopPropagation()}
          >
            <div className={styles.deleteConfirmIcon}>⚠️</div>
            <h2 className={styles.deleteConfirmTitle}>Xác nhận xóa bài đăng?</h2>
            <p className={styles.deleteConfirmText}>
              Bạn có chắc chắn muốn xóa bài viết{' '}
              <span className={styles.deleteTargetName}>"{deletingPost.title}"</span>?
              <br />
              Hành động này <strong>không thể hoàn tác</strong>. Mọi thông tin liên quan sẽ bị xóa khỏi hệ thống.
            </p>

            <div className={styles.deleteActionsRow}>
              <button
                type="button"
                className={styles.btnCancel}
                onClick={() => setDeletingPost(null)}
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                className={styles.btnConfirmDelete}
                onClick={handleConfirmDelete}
                id="btn-confirm-delete"
              >
                🗑️ Xóa vĩnh viễn
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Toast Notification ─────────────────────────────── */}
      {toastMsg && (
        <div className={styles.toastNotice} role="alert">
          {toastMsg}
        </div>
      )}
    </div>
  );
}
