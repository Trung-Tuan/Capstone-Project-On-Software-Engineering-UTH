import styles from './LoadingSpinner.module.css';

interface Props {
  size?: 'sm' | 'md' | 'lg';
  label?: string;
  fullScreen?: boolean;
  inline?: boolean;
}

export default function LoadingSpinner({
  size = 'md',
  label = 'Đang tải...',
  fullScreen,
  inline,
}: Props) {
  return (
    <div
      className={`${styles.wrapper} ${fullScreen ? styles.fullScreen : ''} ${inline ? styles.inline : ''}`}
      role="status"
      aria-live="polite"
      aria-label={label}
    >
      <div className={`${styles.spinner} ${styles[size]}`} />
      {label && <span className={styles.label}>{label}</span>}
    </div>
  );
}
