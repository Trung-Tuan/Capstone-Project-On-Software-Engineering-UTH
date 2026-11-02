import type { ReactNode } from 'react';

interface ProvidersProps {
  children: ReactNode;
}

/**
 * Global providers wrapper.
 * Add AuthProvider (Phase 2), ToastProvider, QueryClientProvider, etc. here.
 */
export default function Providers({ children }: ProvidersProps) {
  return (
    <>
      {/* AuthProvider added in Phase 2 */}
      {children}
    </>
  );
}
