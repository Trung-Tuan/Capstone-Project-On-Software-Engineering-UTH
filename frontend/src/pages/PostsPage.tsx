import { useState } from 'react';
import FilterSidebar, { FilterTrigger } from '@components/common/FilterSidebar/FilterSidebar';
import type { PostFilters } from '@types/index';
import styles from './PostsPage.module.css';

export default function PostsPage() {
  const [filters, setFilters] = useState<PostFilters>({});
  const [drawerOpen, setDrawerOpen] = useState(false);
  const activeCount = Object.values(filters).filter(Boolean).length;

  const handleReset = () => setFilters({});

  return (
    <div className="container">
      <div className={styles.pageHeader}>
        <div>
          <h1 className={styles.pageTitle}>Danh sách bài đăng</h1>
          <p className={styles.pageDesc}>Tìm kiếm đồ vật bị mất hoặc được tìm thấy trong trường</p>
        </div>
        <FilterTrigger count={activeCount} onClick={() => setDrawerOpen(true)} />
      </div>

      <div className={styles.layout}>
        <FilterSidebar
          filters={filters}
          onFilterChange={setFilters}
          onReset={handleReset}
          isDrawerOpen={drawerOpen}
          onDrawerClose={() => setDrawerOpen(false)}
        />
        <main className={styles.main}>
          {/* Post list will be implemented in Phase 3 */}
          <div className={styles.placeholder}>
            <span>📋</span>
            <p>Danh sách bài đăng sẽ được hiển thị ở đây (Phase 3)</p>
          </div>
        </main>
      </div>
    </div>
  );
}
