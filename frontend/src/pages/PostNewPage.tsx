import {
  useState,
  useRef,
  useCallback,
  useId,
  type ChangeEvent,
  type FormEvent,
} from 'react';
import { useNavigate } from 'react-router-dom';
import { POST_CATEGORIES, CAMPUS_LOCATIONS, UPLOAD_CONSTRAINTS, ROUTES } from '@constants/index';
import type { PostType } from '@types/index';
import styles from './PostNewPage.module.css';

// ── Category emoji map ────────────────────────────────────
const CATEGORY_META: Record<string, { emoji: string; suggestions: string[] }> = {
  documents: {
    emoji: '📄',
    suggestions: ['Thẻ sinh viên UTH', 'Căn cước công dân (CCCD)', 'Giấy phép lái xe (GPLX)', 'Thẻ xe buýt / Thẻ ngân hàng', 'Bìa hồ sơ học tập'],
  },
  wallet: {
    emoji: '👛',
    suggestions: ['Ví da nam màu đen', 'Ví nữ cầm tay mini', 'Ví gập màu nâu', 'Ví vải có dây kéo'],
  },
  electronics: {
    emoji: '💻',
    suggestions: ['Tai nghe không dây', 'Củ sạc / Cáp sạc iPhone', 'Sạc dự phòng', 'Chuột máy tính không dây', 'Điện thoại di động'],
  },
  keys: {
    emoji: '🔑',
    suggestions: ['Chìa khóa xe máy Honda', 'Chìa khóa xe Yamaha', 'Chùm chìa khóa phòng', 'Smartkey xe máy'],
  },
  bags: {
    emoji: '🎒',
    suggestions: ['Balo đựng laptop màu đen', 'Túi tote vải canvas', 'Túi đeo chéo mini'],
  },
  clothing: {
    emoji: '👕',
    suggestions: ['Áo khoác dù', 'Áo khoác đồng phục UTH', 'Mũ bảo hiểm 1/2', 'Kính cận gọng đen'],
  },
  jewelry: {
    emoji: '💍',
    suggestions: ['Nhẫn bạc', 'Vòng tay kim loại', 'Đồng hồ đeo tay', 'Dây chuyền'],
  },
  books: {
    emoji: '📚',
    suggestions: ['Giáo trình đại cương', 'Sổ tay ghi chú', 'Sách tiếng Anh', 'Tập bài giảng'],
  },
  stationery: {
    emoji: '✏️',
    suggestions: ['Hộp bút chì màu xanh', 'Máy tính Casio fx-580VNX', 'Bút ký cao cấp'],
  },
  sports: {
    emoji: '⚽',
    suggestions: ['Vợt cầu lông', 'Bóng rổ', 'Bình nước thể thao', 'Giày thể thao'],
  },
  other: {
    emoji: '📦',
    suggestions: ['Cây dù đi mưa', 'Bình giữ nhiệt Lock&Lock', 'Kính râm'],
  },
};

const LOCATION_META: Record<string, string> = {
  library: 'Thư viện',
  canteen: 'Căng tin',
  parking: 'Bãi giữ xe',
  'classroom-a': 'Tòa A',
  'classroom-b': 'Tòa B',
  'classroom-c': 'Tòa C',
  lab: 'Phòng Lab',
  'sports-ground': 'Sân thể thao',
  'admin-office': 'Phòng Đào tạo / Hành chính',
  entrance: 'Cổng trường',
  other: 'Vị trí khác',
};

// Danh mục nhạy cảm cần bật cảnh báo bảo mật
const SENSITIVE_CATEGORIES = new Set(['documents', 'wallet']);

interface ImagePreview {
  id: string;
  file: File;
  url: string;
}

interface FormData {
  type: PostType;
  category: string;
  title: string;
  description: string;
  location: string;
  date: string;
}

function getFormattedDate(offsetDays = 0) {
  const d = new Date();
  d.setDate(d.getDate() - offsetDays);
  return d.toISOString().split('T')[0];
}

export default function PostNewPage() {
  const navigate = useNavigate();
  const galleryInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const titleInputRef = useRef<HTMLInputElement>(null);

  const titleId = useId();
  const descId = useId();

  const todayStr = getFormattedDate(0);
  const yesterdayStr = getFormattedDate(1);

  const [form, setForm] = useState<FormData>({
    type: 'LOST',
    category: '',
    title: '',
    description: '',
    location: '',
    date: todayStr,
  });

  const [images, setImages] = useState<ImagePreview[]>([]);
  const [imageError, setImageError] = useState('');
  const [errors, setErrors] = useState<Partial<Record<keyof FormData, string>>>({});
  const [hasConfirmedPrivacy, setHasConfirmedPrivacy] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  // Cảnh báo xuất hiện khi chọn Giấy tờ, Ví HOẶC khi tải ảnh lên
  const showPrivacyWarning = SENSITIVE_CATEGORIES.has(form.category) || images.length > 0;

  // Xử lý chọn loại tin
  const handleTypeSelect = (type: PostType) => {
    setForm(prev => ({ ...prev, type }));
  };

  // Xử lý chọn danh mục
  const handleCategorySelect = (category: string) => {
    setForm(prev => ({ ...prev, category }));
    setErrors(prev => ({ ...prev, category: '' }));
  };

  // Xử lý chọn nhanh gợi ý tiêu đề (tiết kiệm thời gian gõ phím trên mobile)
  const handleSuggestionClick = (suggestedTitle: string) => {
    setForm(prev => ({ ...prev, title: suggestedTitle }));
    setErrors(prev => ({ ...prev, title: '' }));
    titleInputRef.current?.focus();
  };

  // Xử lý chọn địa điểm
  const handleLocationSelect = (locValue: string) => {
    setForm(prev => ({ ...prev, location: locValue }));
    setErrors(prev => ({ ...prev, location: '' }));
  };

  // Xử lý chọn ngày bằng preset (Hôm nay / Hôm qua)
  const handleDatePreset = (dateVal: string) => {
    setForm(prev => ({ ...prev, date: dateVal }));
    setErrors(prev => ({ ...prev, date: '' }));
  };

  const handleInputChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
    setErrors(prev => ({ ...prev, [name]: '' }));
  };

  // Tải ảnh (Camera hoặc Thư viện)
  const handleFiles = useCallback((files: FileList | null) => {
    if (!files) return;
    setImageError('');
    const remaining = UPLOAD_CONSTRAINTS.MAX_FILES - images.length;
    if (remaining <= 0) {
      setImageError(`Tối đa ${UPLOAD_CONSTRAINTS.MAX_FILES} hình ảnh.`);
      return;
    }

    const accepted: ImagePreview[] = [];
    Array.from(files).slice(0, remaining).forEach(file => {
      if (!UPLOAD_CONSTRAINTS.ACCEPTED_TYPES.includes(file.type as never)) {
        setImageError('Vui lòng chọn ảnh định dạng JPG, PNG hoặc WebP.');
        return;
      }
      if (file.size > UPLOAD_CONSTRAINTS.MAX_FILE_SIZE) {
        setImageError('Dung lượng mỗi ảnh tối đa 5 MB.');
        return;
      }
      accepted.push({
        id: `${Date.now()}-${Math.random()}`,
        file,
        url: URL.createObjectURL(file),
      });
    });

    setImages(prev => [...prev, ...accepted]);
  }, [images.length]);

  const removeImage = (id: string) => {
    setImages(prev => {
      const target = prev.find(img => img.id === id);
      if (target) URL.revokeObjectURL(target.url);
      return prev.filter(img => img.id !== id);
    });
  };

  // Xác thực nhanh
  const validate = (): boolean => {
    const newErrors: Partial<Record<keyof FormData, string>> = {};
    if (!form.category) newErrors.category = 'Vui lòng chọn 1 danh mục đồ vật.';
    if (!form.title.trim()) {
      newErrors.title = 'Vui lòng nhập tên đồ vật.';
    } else if (form.title.trim().length < 3) {
      newErrors.title = 'Tên đồ vật tối thiểu 3 ký tự.';
    }
    if (!form.location) newErrors.location = 'Vui lòng chọn địa điểm trong trường.';
    if (!form.date) newErrors.date = 'Vui lòng chọn thời gian.';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!validate()) {
      window.scrollTo({ top: 120, behavior: 'smooth' });
      return;
    }

    setSubmitting(true);
    // Giả lập gửi bài đăng (Phase 4 kết nối API)
    await new Promise(resolve => setTimeout(resolve, 800));
    setSubmitting(false);
    setSubmitted(true);
  };

  // Màn hình thành công
  if (submitted) {
    return (
      <div className={styles.page}>
        <div className={styles.successOverlay}>
          <div className={styles.successIcon}>🎉</div>
          <h1 className={styles.successTitle}>Đăng tin thành công!</h1>
          <p className={styles.successText}>
            Bài đăng <strong>"{form.title}"</strong> đã được tạo thành công và đang hiển thị để cộng đồng sinh viên UTH hỗ trợ.
          </p>
          <div className={styles.successActions}>
            <button
              type="button"
              className={styles.btnPrimaryAction}
              onClick={() => navigate(ROUTES.POSTS)}
            >
              📋 Xem danh sách bài đăng
            </button>
            <button
              type="button"
              className={styles.btnSecondaryAction}
              onClick={() => {
                setSubmitted(false);
                setForm({
                  type: 'LOST',
                  category: '',
                  title: '',
                  description: '',
                  location: '',
                  date: todayStr,
                });
                setImages([]);
                setHasConfirmedPrivacy(false);
              }}
            >
              ✏️ Đăng tin khác
            </button>
          </div>
        </div>
      </div>
    );
  }

  const isLost = form.type === 'LOST';
  const categorySuggestions = form.category ? CATEGORY_META[form.category]?.suggestions || [] : [];

  return (
    <div className={styles.page}>
      {/* ── Top Header ────────────────────────────────────── */}
      <div className={styles.topHeader}>
        <button
          type="button"
          className={styles.backBtn}
          onClick={() => navigate(-1)}
          aria-label="Quay lại"
        >
          ←
        </button>
        <div className={styles.headerText}>
          <h1 className={styles.pageTitle}>Đăng tin nhanh</h1>
          <div className={styles.pageSub}>
            <span>Hỗ trợ tìm kiếm UTH</span>
            <span className={styles.timeTag}>⚡ Hoàn thành &lt; 2 phút</span>
          </div>
        </div>
      </div>

      <form className={styles.formBody} onSubmit={handleSubmit} noValidate>
        {/* ── BƯỚC 1: Chọn loại tin ──────────────────────── */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>
            <span>1. Bạn muốn đăng tin gì?</span>
          </div>
          <div className={styles.typeToggle}>
            <button
              type="button"
              className={`${styles.typeBtn} ${isLost ? styles.typeBtnLostActive : ''}`}
              onClick={() => handleTypeSelect('LOST')}
              id="btn-select-lost"
            >
              <span className={styles.typeBtnEmoji}>🚨</span>
              <span className={styles.typeBtnLabel}>Mất đồ</span>
              <span className={styles.typeBtnSub}>Tôi cần tìm lại đồ</span>
            </button>

            <button
              type="button"
              className={`${styles.typeBtn} ${!isLost ? styles.typeBtnFoundActive : ''}`}
              onClick={() => handleTypeSelect('FOUND')}
              id="btn-select-found"
            >
              <span className={styles.typeBtnEmoji}>🎁</span>
              <span className={styles.typeBtnLabel}>Nhặt được</span>
              <span className={styles.typeBtnSub}>Tôi nhặt được đồ</span>
            </button>
          </div>
        </div>

        {/* ── BƯỚC 2: Chọn danh mục ───────────────────────── */}
        <div className={`${styles.section} ${errors.category ? styles.sectionError : ''}`}>
          <div className={styles.sectionTitle}>
            <span>2. Danh mục món đồ *</span>
          </div>
          <div className={styles.categoryGrid}>
            {POST_CATEGORIES.map(cat => {
              const meta = CATEGORY_META[cat.value];
              const isSelected = form.category === cat.value;
              return (
                <button
                  key={cat.value}
                  type="button"
                  className={`${styles.catBtn} ${isSelected ? styles.catBtnActive : ''}`}
                  onClick={() => handleCategorySelect(cat.value)}
                  id={`cat-btn-${cat.value}`}
                >
                  <span className={styles.catBtnEmoji}>{meta?.emoji || '📦'}</span>
                  <span className={styles.catBtnLabel}>{cat.label}</span>
                </button>
              );
            })}
          </div>
          {errors.category && <span className={styles.errorText}>⚠️ {errors.category}</span>}
        </div>

        {/* ── BƯỚC 3: Tên đồ vật + Gợi ý nhanh ───────────── */}
        <div className={`${styles.section} ${errors.title ? styles.sectionError : ''}`}>
          <div className={styles.sectionTitle}>
            <span>3. Tên đồ vật *</span>
          </div>
          <div className={styles.titleInputWrap}>
            <input
              ref={titleInputRef}
              id={titleId}
              name="title"
              type="text"
              className={`${styles.titleInput} ${errors.title ? styles.inputError : ''}`}
              placeholder="VD: Ví da nam màu đen, Thẻ sinh viên, Laptop Dell..."
              value={form.title}
              onChange={handleInputChange}
              maxLength={100}
            />
            <span className={styles.charCount}>{form.title.length}/100</span>
          </div>
          {errors.title && <span className={styles.errorText}>⚠️ {errors.title}</span>}

          {/* Gợi ý tên đồ vật theo danh mục (tiết kiệm thời gian trên mobile) */}
          {categorySuggestions.length > 0 && (
            <div>
              <div className={styles.suggestionsLabel}>⚡ Gợi ý chọn nhanh (chạm để điền):</div>
              <div className={styles.suggestionsRow}>
                {categorySuggestions.map(sug => (
                  <button
                    key={sug}
                    type="button"
                    className={styles.suggestionChip}
                    onClick={() => handleSuggestionClick(sug)}
                  >
                    + {sug}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* ── BƯỚC 4: Địa điểm & Thời gian ────────────────── */}
        <div className={`${styles.section} ${errors.location || errors.date ? styles.sectionError : ''}`}>
          <div className={styles.sectionTitle}>
            <span>4. Địa điểm &amp; Thời gian *</span>
          </div>

          <div style={{ marginBottom: 'var(--space-3)' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)' }}>
              VỊ TRÍ TẠI UTH:
            </span>
          </div>
          <div className={styles.locationGrid}>
            {CAMPUS_LOCATIONS.map(loc => {
              const isSelected = form.location === loc.value;
              return (
                <button
                  key={loc.value}
                  type="button"
                  className={`${styles.locationChip} ${isSelected ? styles.locationChipActive : ''}`}
                  onClick={() => handleLocationSelect(loc.value)}
                >
                  {isSelected ? '✓ ' : ''}{loc.label}
                </button>
              );
            })}
          </div>
          {errors.location && <span className={styles.errorText}>⚠️ {errors.location}</span>}

          <div style={{ marginTop: 'var(--space-4)', marginBottom: 'var(--space-2)' }}>
            <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)' }}>
              THỜI GIAN XẢY RA:
            </span>
          </div>
          <div className={styles.dateRow}>
            <button
              type="button"
              className={`${styles.datePresetBtn} ${form.date === todayStr ? styles.datePresetActive : ''}`}
              onClick={() => handleDatePreset(todayStr)}
            >
              📅 Hôm nay
            </button>
            <button
              type="button"
              className={`${styles.datePresetBtn} ${form.date === yesterdayStr ? styles.datePresetActive : ''}`}
              onClick={() => handleDatePreset(yesterdayStr)}
            >
              📅 Hôm qua
            </button>
          </div>
          <input
            name="date"
            type="date"
            className={styles.dateInput}
            value={form.date}
            onChange={handleInputChange}
            max={todayStr}
          />
          {errors.date && <span className={styles.errorText}>⚠️ {errors.date}</span>}
        </div>

        {/* ── BƯỚC 5: Tải ảnh đồ vật (Tùy chọn) ────────────── */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>
            <span>5. Hình ảnh đồ vật (Tùy chọn - Tối đa 3 ảnh)</span>
          </div>

          {/* Hidden inputs cho camera vs gallery */}
          <input
            ref={galleryInputRef}
            type="file"
            accept={UPLOAD_CONSTRAINTS.ACCEPTED_EXTENSIONS}
            multiple
            style={{ display: 'none' }}
            onChange={e => handleFiles(e.target.files)}
          />
          <input
            ref={cameraInputRef}
            type="file"
            accept="image/*"
            capture="environment"
            style={{ display: 'none' }}
            onChange={e => handleFiles(e.target.files)}
          />

          <div className={styles.uploadBtnRow}>
            <button
              type="button"
              className={styles.uploadBtn}
              onClick={() => cameraInputRef.current?.click()}
              id="btn-upload-camera"
            >
              <span className={styles.uploadBtnIcon}>📸</span>
              <span>Chụp ảnh ngay</span>
            </button>

            <button
              type="button"
              className={styles.uploadBtn}
              onClick={() => galleryInputRef.current?.click()}
              id="btn-upload-gallery"
            >
              <span className={styles.uploadBtnIcon}>🖼️</span>
              <span>Chọn từ album</span>
            </button>
          </div>

          {/* Previews */}
          {images.length > 0 && (
            <div className={styles.imagePreviewsRow}>
              {images.map(img => (
                <div key={img.id} className={styles.imagePreviewItem}>
                  <img src={img.url} alt="Preview" className={styles.previewImg} />
                  <button
                    type="button"
                    className={styles.removeImgBtn}
                    onClick={() => removeImage(img.id)}
                    aria-label="Xóa ảnh"
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
          )}

          {imageError && <div className={styles.imageError}>⚠️ {imageError}</div>}
          <div className={styles.imageHint}>
            💡 Mẹo: Chụp rõ các góc hoặc đặc điểm nhận diện đặc trưng (màu sắc, vết trầy, sticker...)
          </div>
        </div>

        {/* ── BƯỚC 6: Mô tả chi tiết (Tùy chọn) ────────────── */}
        <div className={styles.section}>
          <div className={styles.sectionTitle}>
            <span>6. Mô tả thêm đặc điểm nhận dạng</span>
          </div>
          <textarea
            id={descId}
            name="description"
            rows={3}
            className={styles.descTextarea}
            placeholder="Mô tả cụ thể màu sắc, vết trầy xước, ốp lưng, móc khóa... để dễ xác minh chính chủ"
            value={form.description}
            onChange={handleInputChange}
          />
        </div>

        {/* ── CẢNH BÁO AN TOÀN THÔNG TIN CÁ NHÂN ───────────── */}
        {showPrivacyWarning && (
          <div className={styles.privacyWarning} role="alert">
            <div className={styles.privacyHeader}>
              <span className={styles.privacyIcon}>🛡️</span>
              <div>
                <div className={styles.privacyTitle}>
                  CẢNH BÁO BẢO MẬT THÔNG TIN CÁ NHÂN (CCCD &amp; NGÂN HÀNG)
                </div>
                <div className={styles.privacyText}>
                  Bạn đang chọn danh mục nhạy cảm (Giấy tờ / Ví tiền) hoặc tải hình ảnh lên hệ thống:
                </div>
              </div>
            </div>

            <ul className={styles.privacyRules}>
              <li>
                <span>🚫</span>
                <span><strong>Tuyệt đối KHÔNG</strong> chụp rõ 12 số CCCD/CMND, ngày tháng năm sinh hoặc số thẻ tín dụng/ATM.</span>
              </li>
              <li>
                <span>✂️</span>
                <span>Vui lòng che/làm mờ 6 số cuối CCCD hoặc dùng ngón tay che mã bảo mật CVV trước khi chụp ảnh.</span>
              </li>
              <li>
                <span>🔐</span>
                <span>Hệ thống UTH cam kết bảo vệ danh tính và bảo mật thông tin tài sản cho toàn thể sinh viên.</span>
              </li>
            </ul>

            <label className={styles.privacyCheckRow}>
              <input
                type="checkbox"
                checked={hasConfirmedPrivacy}
                onChange={e => setHasConfirmedPrivacy(e.target.checked)}
                style={{ marginTop: '2px', accentColor: 'var(--color-primary-500)', width: '16px', height: '16px' }}
              />
              <span>Tôi xác nhận không để lộ số CCCD, thẻ ngân hàng hoặc thông tin mật của người khác trên bài đăng này.</span>
            </label>
          </div>
        )}

        {/* ── Sticky Bottom Bar cho Mobile ─────────────────── */}
        <div className={styles.stickyBottomBar}>
          <div className={styles.bottomBarInner}>
            <div className={styles.bottomSummary}>
              <div className={styles.bottomSummaryTitle}>
                <span>{isLost ? '🚨 Mất đồ' : '🎁 Nhặt được'}</span>
                <span>·</span>
                <span>{form.category ? POST_CATEGORIES.find(c => c.value === form.category)?.label : 'Chưa chọn loại'}</span>
              </div>
              <span className={styles.bottomSummarySub}>
                {form.location ? LOCATION_META[form.location] || form.location : 'Chưa chọn vị trí'}
              </span>
            </div>

            <button
              type="submit"
              className={styles.submitBtn}
              disabled={submitting}
              id="btn-submit-post"
            >
              {submitting ? 'Đang đăng tin…' : '🚀 Đăng tin ngay'}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
