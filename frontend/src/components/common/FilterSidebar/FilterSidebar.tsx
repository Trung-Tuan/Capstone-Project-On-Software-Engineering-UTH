import { useState, useEffect, useCallback } from 'react';
import { POST_CATEGORIES, CAMPUS_LOCATIONS } from '@constants/index';
import type { PostFilters, PostType } from '@types/index';
import styles from './FilterSidebar.module.css';

// ── Props ─────────────────────────────────────────────────
interface FilterSidebarProps {
  filters: PostFilters;
  onFilterChange: (filters: PostFilters) => void;
  onReset: () => void;
  /** Mobile drawer mode: controlled externally */
  isDrawerOpen?: boolean;
  onDrawerClose?: () => void;
}

// ── Component ─────────────────────────────────────────────
export default function FilterSidebar({
  filters,
  onFilterChange,
  onReset,
  isDrawerOpen = false,
  onDrawerClose,
}: FilterSidebarProps) {
  const [local, setLocal] = useState<PostFilters>(filters);

  // Sync when parent filters change (e.g. URL driven)
  useEffect(() => { setLocal(filters); }, [filters]);

  const activeCount = [
    local.type, local.category, local.location, local.keyword,
    local.dateFrom, local.dateTo,
  ].filter(Boolean).length;

  const update = useCallback(<K extends keyof PostFilters>(key: K, val: PostFilters[K]) => {
    setLocal(prev => ({ ...prev, [key]: val || undefined }));
  }, []);

  const handleTypeToggle = (type: PostType | undefined) => {
    setLocal(prev => ({ ...prev, type: prev.type === type ? undefined : type }));
  };

  const handleApply = () => {
    onFilterChange(local);
    onDrawerClose?.();
  };

  const handleReset = () => {
    setLocal({});
    onReset();
    onDrawerClose?.();
  };

  // ── Inner content (shared between sidebar and drawer) ──
  const content = (
    <>
      {/* Type filter */}
      <div className={styles.card}>
        <div className={styles.cardTitle}>Loại bài đăng</div>
        <div className={styles.typeGrid}>
          <button
            className={`${styles.typeBtn} ${!local.type ? styles.active : ''}`}
            onClick={() => handleTypeToggle(undefined)}
            id="filter-type-all"
          >
            <span className={styles.typeBtnIcon}>📋</span>
            Tất cả
          </button>
          <button
            className={`${styles.typeBtn} ${local.type === 'LOST' ? styles.lostActive : ''}`}
            onClick={() => handleTypeToggle('LOST')}
            id="filter-type-lost"
          >
            <span className={styles.typeBtnIcon}>😢</span>
            Đã mất
          </button>
          <button
            className={`${styles.typeBtn} ${local.type === 'FOUND' ? styles.foundActive : ''}`}
            onClick={() => handleTypeToggle('FOUND')}
            id="filter-type-found"
          >
            <span className={styles.typeBtnIcon}>🎉</span>
            Tìm thấy
          </button>
        </div>
      </div>

      {/* Keyword search */}
      <div className={styles.card}>
        <div className={styles.cardTitle}>Tìm kiếm</div>
        <div className={styles.searchWrap}>
          <span className={styles.searchIcon} aria-hidden="true">🔍</span>
          <input
            id="filter-keyword"
            type="search"
            className={styles.searchInput}
            placeholder="Tên đồ vật, mô tả…"
            value={local.keyword ?? ''}
            onChange={e => update('keyword', e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleApply(); }}
          />
        </div>
      </div>

      {/* Category */}
      <div className={styles.card}>
        <div className={styles.cardTitle}>Danh mục</div>
        <select
          id="filter-category"
          className={styles.select}
          value={local.category ?? ''}
          onChange={e => update('category', e.target.value)}
        >
          <option value="">Tất cả danh mục</option>
          {POST_CATEGORIES.map(cat => (
            <option key={cat.value} value={cat.value}>{cat.label}</option>
          ))}
        </select>
      </div>

      {/* Location */}
      <div className={styles.card}>
        <div className={styles.cardTitle}>Địa điểm</div>
        <select
          id="filter-location"
          className={styles.select}
          value={local.location ?? ''}
          onChange={e => update('location', e.target.value)}
        >
          <option value="">Tất cả địa điểm</option>
          {CAMPUS_LOCATIONS.map(loc => (
            <option key={loc.value} value={loc.value}>{loc.label}</option>
          ))}
        </select>
      </div>

      {/* Date range */}
      <div className={styles.card}>
        <div className={styles.cardTitle}>Thời gian</div>
        <div className={styles.dateGrid}>
          <div>
            <label htmlFor="filter-date-from" className={styles.dateLabel}>Từ ngày</label>
            <input
              id="filter-date-from"
              type="date"
              className={styles.dateInput}
              value={local.dateFrom ?? ''}
              onChange={e => update('dateFrom', e.target.value)}
              max={local.dateTo}
            />
          </div>
          <div>
            <label htmlFor="filter-date-to" className={styles.dateLabel}>Đến ngày</label>
            <input
              id="filter-date-to"
              type="date"
              className={styles.dateInput}
              value={local.dateTo ?? ''}
              onChange={e => update('dateTo', e.target.value)}
              min={local.dateFrom}
            />
          </div>
        </div>
      </div>

      {/* Apply / Reset */}
      <div className={styles.btnRow}>
        <button
          className={styles.btnApply}
          onClick={handleApply}
          id="filter-apply-btn"
        >
          Áp dụng
          {activeCount > 0 && (
            <span className={styles.filterBadge}>{activeCount}</span>
          )}
        </button>
        <button
          className={styles.btnReset}
          onClick={handleReset}
          id="filter-reset-btn"
          disabled={activeCount === 0}
        >
          Xóa bộ lọc
        </button>
      </div>
    </>
  );

  return (
    <>
      {/* ── Desktop sidebar ── */}
      <aside className={styles.sidebar} aria-label="Bộ lọc bài đăng">
        {content}
      </aside>

      {/* ── Mobile drawer ── */}
      {isDrawerOpen && (
        <div
          className={`${styles.drawerOverlay} ${styles.open}`}
          onClick={onDrawerClose}
          aria-hidden="true"
        />
      )}
      <div
        className={`${styles.drawer} ${isDrawerOpen ? styles.open : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label="Bộ lọc bài đăng"
        id="filter-drawer"
      >
        <div className={styles.drawerHeader}>
          <span className={styles.drawerTitle}>
            Bộ lọc
            {activeCount > 0 && (
              <span className={styles.filterBadge}>{activeCount}</span>
            )}
          </span>
          <button
            className={styles.drawerClose}
            onClick={onDrawerClose}
            aria-label="Đóng bộ lọc"
            id="filter-drawer-close"
          >
            ✕
          </button>
        </div>
        <div className={styles.drawerBody}>{content}</div>
      </div>
    </>
  );
}

// ── FilterTrigger (button to open drawer on mobile) ───────
export function FilterTrigger({
  count,
  onClick,
}: {
  count: number;
  onClick: () => void;
}) {
  return (
    <button
      className={styles.filterTrigger}
      onClick={onClick}
      id="filter-drawer-trigger"
      aria-controls="filter-drawer"
      aria-expanded={false}
    >
      <span aria-hidden="true">⚙️</span>
      Bộ lọc
      {count > 0 && <span className={styles.filterBadge}>{count}</span>}
    </button>
  );
}
