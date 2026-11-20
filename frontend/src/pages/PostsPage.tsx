import { useState, useEffect, useMemo, useRef } from 'react';
import { Link } from 'react-router-dom';
import FilterSidebar, { FilterTrigger } from '@components/common/FilterSidebar/FilterSidebar';
import { getStoredPosts } from '@services/postService';
import { POST_CATEGORIES, CAMPUS_LOCATIONS } from '@constants/index';
import type { PostFilters, Post } from '@types/index';
import styles from './PostsPage.module.css';

type SortOption = 'newest' | 'oldest' | 'title-asc' | 'title-desc';

export default function PostsPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [filters, setFilters] = useState<PostFilters>({});
  const [drawerOpen, setDrawerOpen] = useState(false);

  // ── 1. Debounced Search ─────────────────────────────────
  const [searchInput, setSearchInput] = useState('');
  const [debouncedKeyword, setDebouncedKeyword] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  // ── 2. Sorting & Pagination ──────────────────────────────
  const [sortBy, setSortBy] = useState<SortOption>('newest');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(6);

  const gridTopRef = useRef<HTMLDivElement>(null);

  // Load posts
  useEffect(() => {
    setPosts(getStoredPosts());
  }, []);

  // Debounce search effect (350ms delay)
  useEffect(() => {
    setIsSearching(true);
    const timer = setTimeout(() => {
      setDebouncedKeyword(searchInput.trim());
      setIsSearching(false);
      setCurrentPage(1); // Reset to page 1 on new search
    }, 350);

    return () => clearTimeout(timer);
  }, [searchInput]);

  // Sync keyword into filters
  useEffect(() => {
    setFilters(prev => ({ ...prev, keyword: debouncedKeyword || undefined }));
  }, [debouncedKeyword]);

  const activeFilterCount = [
    filters.type,
    filters.status,
    filters.category,
    filters.location,
    filters.dateFrom,
    filters.dateTo,
  ].filter(Boolean).length;

  const handleResetFilters = () => {
    setFilters({});
    setSearchInput('');
    setDebouncedKeyword('');
    setCurrentPage(1);
  };

  const handleFilterChange = (newFilters: PostFilters) => {
    setFilters(prev => ({
      ...newFilters,
      keyword: prev.keyword, // Preserve search keyword
    }));
    setCurrentPage(1);
  };

  // ── Filtered & Sorted Posts ──────────────────────────────
  const filteredPosts = useMemo(() => {
    const result = posts.filter(post => {
      // 1. Loại bài đăng
      if (filters.type && post.type !== filters.type) return false;

      // 2. Trạng thái bài đăng
      if (filters.status && post.status !== filters.status) return false;

      // 3. Danh mục
      if (filters.category && post.category !== filters.category) return false;

      // 4. Vị trí
      if (filters.location && post.location !== filters.location) return false;

      // 5. Khoảng thời gian
      if (filters.dateFrom && post.date < filters.dateFrom) return false;
      if (filters.dateTo && post.date > filters.dateTo) return false;

      // 6. Từ khóa tìm kiếm (Tên, Mô tả, Địa điểm)
      if (filters.keyword) {
        const q = filters.keyword.toLowerCase();
        const matchTitle = post.title.toLowerCase().includes(q);
        const matchDesc = post.description.toLowerCase().includes(q);
        const matchLocation = post.location.toLowerCase().includes(q);
        if (!matchTitle && !matchDesc && !matchLocation) return false;
      }

      return true;
    });

    // Sắp xếp
    return [...result].sort((a, b) => {
      if (sortBy === 'newest') {
        return new Date(b.date).getTime() - new Date(a.date).getTime();
      }
      if (sortBy === 'oldest') {
        return new Date(a.date).getTime() - new Date(b.date).getTime();
      }
      if (sortBy === 'title-asc') {
        return a.title.localeCompare(b.title, 'vi');
      }
      if (sortBy === 'title-desc') {
        return b.title.localeCompare(a.title, 'vi');
      }
      return 0;
    });
  }, [posts, filters, sortBy]);

  // ── Pagination Calculation ───────────────────────────────
  const totalItems = filteredPosts.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

  const paginatedPosts = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize;
    return filteredPosts.slice(startIndex, startIndex + pageSize);
  }, [filteredPosts, currentPage, pageSize]);

  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setCurrentPage(newPage);
      gridTopRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  const getLocationLabel = (val: string) => {
    return CAMPUS_LOCATIONS.find(l => l.value === val)?.label || val;
  };

  const getCategoryLabel = (val: string) => {
    return POST_CATEGORIES.find(c => c.value === val)?.label || val;
  };

  const pageNumbers = useMemo(() => {
    const pages: number[] = [];
    for (let i = 1; i <= totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }, [totalPages]);

  return (
    <div className={`container ${styles.pageContainer}`}>
      {/* ── Top Header ────────────────────────────────────── */}
      <div className={styles.pageHeader}>
        <div className={styles.titleArea}>
          <h1 className={styles.pageTitle}>Danh sách bài đăng</h1>
          <p className={styles.pageDesc}>
            Tìm kiếm đồ thất lạc và tin nhặt được đồ trong khuôn viên trường ĐH Giao thông Vận tải TP.HCM
          </p>
        </div>
      </div>

      {/* ── Thanh tìm kiếm từ khóa với Debounce ───────────── */}
      <div className={styles.searchSection} role="search">
        <span className={styles.searchIcon} aria-hidden="true">
          🔍
        </span>
        <input
          type="search"
          className={styles.searchInput}
          placeholder="Tìm theo tên đồ vật, mô tả, vị trí (VD: Ví da, Laptop, Bãi xe A)..."
          value={searchInput}
          onChange={e => setSearchInput(e.target.value)}
          id="posts-search-input"
          autoComplete="off"
        />
        {isSearching && (
          <span className={styles.searchingSpinner}>Đang lọc…</span>
        )}
        {searchInput && (
          <button
            type="button"
            className={styles.clearSearchBtn}
            onClick={() => setSearchInput('')}
            aria-label="Xóa từ khóa tìm kiếm"
          >
            ✕
          </button>
        )}
      </div>

      <div className={styles.layout}>
        {/* ── BỘ LỌC ĐA NĂNG (Sidebar bên trái trên Desktop / Drawer trên Mobile) ── */}
        <FilterSidebar
          filters={filters}
          onFilterChange={handleFilterChange}
          onReset={handleResetFilters}
          isDrawerOpen={drawerOpen}
          onDrawerClose={() => setDrawerOpen(false)}
        />

        {/* ── MAIN CONTENT: Controls + Card-based Grid + Pagination ── */}
        <main className={styles.main} ref={gridTopRef}>
          {/* ── Thanh điều khiển: Số kết quả + Sắp xếp + Nút mở Drawer ── */}
          <div className={styles.controlsBar}>
            <div className={styles.resultsCount}>
              <span>
                Tìm thấy{' '}
                <span className={styles.resultsHighlight}>{totalItems}</span> bài đăng
              </span>
              {debouncedKeyword && (
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                  cho từ khóa "{debouncedKeyword}"
                </span>
              )}
            </div>

            <div className={styles.controlsRight}>
              {/* Dropdown sắp xếp */}
              <div className={styles.sortGroup}>
                <label htmlFor="posts-sort-select" className={styles.sortLabel}>
                  Sắp xếp:
                </label>
                <select
                  id="posts-sort-select"
                  className={styles.sortSelect}
                  value={sortBy}
                  onChange={e => setSortBy(e.target.value as SortOption)}
                >
                  <option value="newest">🕒 Mới nhất</option>
                  <option value="oldest">⏳ Cũ nhất</option>
                  <option value="title-asc">🔤 Tên A → Z</option>
                  <option value="title-desc">🔤 Tên Z → A</option>
                </select>
              </div>

              {/* Nút bật bộ lọc trên di động */}
              <FilterTrigger
                count={activeFilterCount}
                onClick={() => setDrawerOpen(true)}
              />
            </div>
          </div>

          {/* ── Active Filter Tags (nếu có bộ lọc nào được chọn) ── */}
          {activeFilterCount > 0 && (
            <div className={styles.activeTagsRow}>
              <span className={styles.activeTagLabel}>Bộ lọc đang chọn:</span>

              {filters.type && (
                <span className={styles.filterTag}>
                  Loại: {filters.type === 'LOST' ? 'Mất đồ' : 'Nhặt được'}
                  <button
                    type="button"
                    className={styles.removeTagBtn}
                    onClick={() => handleFilterChange({ ...filters, type: undefined })}
                    aria-label="Xóa lọc loại bài"
                  >
                    ✕
                  </button>
                </span>
              )}

              {filters.category && (
                <span className={styles.filterTag}>
                  Danh mục: {getCategoryLabel(filters.category)}
                  <button
                    type="button"
                    className={styles.removeTagBtn}
                    onClick={() => handleFilterChange({ ...filters, category: undefined })}
                    aria-label="Xóa lọc danh mục"
                  >
                    ✕
                  </button>
                </span>
              )}

              {filters.location && (
                <span className={styles.filterTag}>
                  Vị trí: {getLocationLabel(filters.location)}
                  <button
                    type="button"
                    className={styles.removeTagBtn}
                    onClick={() => handleFilterChange({ ...filters, location: undefined })}
                    aria-label="Xóa lọc vị trí"
                  >
                    ✕
                  </button>
                </span>
              )}

              {filters.status && (
                <span className={styles.filterTag}>
                  Trạng thái: {filters.status === 'RESOLVED' ? 'Đã nhận lại' : 'Đang hoạt động'}
                  <button
                    type="button"
                    className={styles.removeTagBtn}
                    onClick={() => handleFilterChange({ ...filters, status: undefined })}
                    aria-label="Xóa lọc trạng thái"
                  >
                    ✕
                  </button>
                </span>
              )}

              <button
                type="button"
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--color-primary-600)',
                  fontSize: '11px',
                  fontWeight: 'bold',
                  cursor: 'pointer',
                }}
                onClick={handleResetFilters}
              >
                Xóa tất cả
              </button>
            </div>
          )}

          {/* ── GIAO DIỆN DANH SÁCH DẠNG THẺ (CARD-BASED VIEW) ── */}
          {paginatedPosts.length === 0 ? (
            <div className={styles.emptyCard}>
              <div className={styles.emptyIcon}>🔍</div>
              <h2 className={styles.emptyTitle}>Không tìm thấy bài đăng nào</h2>
              <p className={styles.emptyDesc}>
                Không có bài đăng nào khớp với từ khóa tìm kiếm hoặc các tiêu chí bộ lọc của bạn. Vui lòng thử tìm từ khóa khác hoặc xóa bớt bộ lọc.
              </p>
              <button
                type="button"
                className={styles.btnResetFilters}
                onClick={handleResetFilters}
              >
                🔄 Xóa tất cả bộ lọc
              </button>
            </div>
          ) : (
            <div className={styles.postsGrid}>
              {paginatedPosts.map(post => {
                const isLost = post.type === 'LOST';
                const isResolved = post.status === 'RESOLVED';
                const hasImages = post.imageUrls && post.imageUrls.length > 0;
                const authorInitial = post.author?.name ? post.author.name.charAt(0).toUpperCase() : 'U';

                return (
                  <Link
                    key={post.id}
                    to={`/posts/${post.id}`}
                    className={styles.postCard}
                    id={`post-card-${post.id}`}
                  >
                    {/* Hình ảnh món đồ */}
                    <div className={styles.cardImgWrap}>
                      {hasImages ? (
                        <img
                          src={post.imageUrls[0]}
                          alt={post.title}
                          className={styles.cardImg}
                          loading="lazy"
                        />
                      ) : (
                        <div className={styles.cardImgPlaceholder}>
                          <span className={styles.placeholderIcon}>📦</span>
                          <span className={styles.placeholderSub}>Không có ảnh</span>
                        </div>
                      )}

                      {/* NHÃN: "Mất đồ" (Màu đỏ) và "Nhặt được" (Màu xanh lá) theo yêu cầu */}
                      <div className={styles.badgeFloatTopLeft}>
                        {isLost ? (
                          <span className={styles.badgeLostRed}>
                            <span>🚨</span>
                            <span>Mất đồ</span>
                          </span>
                        ) : (
                          <span className={styles.badgeFoundGreen}>
                            <span>🎁</span>
                            <span>Nhặt được</span>
                          </span>
                        )}
                      </div>

                      {/* Nhãn Đã nhận lại nếu RESOLVED */}
                      {isResolved && (
                        <div className={styles.badgeResolvedOverlay}>
                          ✓ Đã nhận lại
                        </div>
                      )}
                    </div>

                    {/* Nội dung card */}
                    <div className={styles.cardBody}>
                      <h3 className={styles.cardTitle}>{post.title}</h3>

                      <div className={styles.cardMeta}>
                        <span>📍 {getLocationLabel(post.location)}</span>
                        <span>•</span>
                        <span>📅 {post.date}</span>
                      </div>
                    </div>

                    {/* Footer card: tác giả + danh mục */}
                    <div className={styles.cardFooter}>
                      <div className={styles.cardAuthor}>
                        {post.author?.avatarUrl ? (
                          <img
                            src={post.author.avatarUrl}
                            alt={post.author.name}
                            className={styles.authorAvatarSm}
                          />
                        ) : (
                          <div className={styles.authorInitialsSm}>
                            {authorInitial}
                          </div>
                        )}
                        <span className={styles.authorNameSm}>{post.author?.name || 'Sinh viên'}</span>
                      </div>

                      <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                        {getCategoryLabel(post.category)}
                      </span>
                    </div>
                  </Link>
                );
              })}
            </div>
          )}

          {/* ── PHÂN TRANG (PAGINATION) ────────────────────────── */}
          {totalPages > 1 && (
            <div className={styles.paginationWrapper}>
              <div className={styles.paginationInfo}>
                Trang {currentPage} / {totalPages} (Tổng số {totalItems} bài đăng)
              </div>

              <div className={styles.paginationRow}>
                {/* Nút Trang trước */}
                <button
                  type="button"
                  className={styles.pageBtn}
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage <= 1}
                  aria-label="Trang trước"
                >
                  ‹ Trước
                </button>

                {/* Các nút số trang */}
                {pageNumbers.map(page => (
                  <button
                    key={page}
                    type="button"
                    className={`${styles.pageBtn} ${page === currentPage ? styles.pageBtnActive : ''}`}
                    onClick={() => handlePageChange(page)}
                    aria-label={`Trang ${page}`}
                    aria-current={page === currentPage ? 'page' : undefined}
                  >
                    {page}
                  </button>
                ))}

                {/* Nút Trang sau */}
                <button
                  type="button"
                  className={styles.pageBtn}
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage >= totalPages}
                  aria-label="Trang sau"
                >
                  Sau ›
                </button>

                {/* Số lượng bài mỗi trang */}
                <select
                  className={styles.pageSizeSelect}
                  value={pageSize}
                  onChange={e => {
                    setPageSize(Number(e.target.value));
                    setCurrentPage(1);
                  }}
                  aria-label="Số bài trên mỗi trang"
                >
                  <option value={6}>6 / trang</option>
                  <option value={9}>9 / trang</option>
                  <option value={12}>12 / trang</option>
                </select>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
