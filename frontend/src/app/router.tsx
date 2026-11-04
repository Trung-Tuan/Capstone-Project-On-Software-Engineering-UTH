import { lazy, Suspense } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import MainLayout from '@layouts/MainLayout';
import AuthLayout from '@layouts/AuthLayout';
import AdminLayout from '@layouts/AdminLayout';
import { ROUTES } from '@constants/index';
import LoadingSpinner from '@components/ui/LoadingSpinner/LoadingSpinner';

// ── Lazy-loaded pages ─────────────────────────────────────
const HomePage         = lazy(() => import('@pages/HomePage'));
const PostsPage        = lazy(() => import('@pages/PostsPage'));
const PostDetailPage   = lazy(() => import('@pages/PostDetailPage'));
const PostNewPage      = lazy(() => import('@pages/PostNewPage'));
const LoginPage        = lazy(() => import('@pages/LoginPage'));
const ClaimPage        = lazy(() => import('@pages/ClaimPage'));
const MyClaimsPage     = lazy(() => import('@pages/MyClaimsPage'));
const ProfilePage      = lazy(() => import('@pages/ProfilePage'));
const AdminDashboard   = lazy(() => import('@pages/admin/AdminDashboardPage'));
const AdminPostsPage   = lazy(() => import('@pages/admin/AdminPostsPage'));
const AdminClaimsPage  = lazy(() => import('@pages/admin/AdminClaimsPage'));
const AdminUsersPage   = lazy(() => import('@pages/admin/AdminUsersPage'));
const NotFoundPage     = lazy(() => import('@pages/NotFoundPage'));

// ── Suspense wrapper ──────────────────────────────────────
const withSuspense = (Component: React.ComponentType) => (
  <Suspense fallback={<LoadingSpinner fullScreen />}>
    <Component />
  </Suspense>
);

// ── Router definition ─────────────────────────────────────
export const router = createBrowserRouter([
  {
    path: ROUTES.HOME,
    element: <MainLayout />,
    children: [
      { index: true,           element: withSuspense(HomePage) },
      { path: 'posts',         element: withSuspense(PostsPage) },
      { path: 'posts/:id',     element: withSuspense(PostDetailPage) },
      { path: 'posts/new',     element: withSuspense(PostNewPage) },
      { path: 'claims/:postId', element: withSuspense(ClaimPage) },
      { path: 'my-claims',     element: withSuspense(MyClaimsPage) },
      { path: 'profile',       element: withSuspense(ProfilePage) },
    ],
  },
  {
    path: '/login',
    element: <AuthLayout />,
    children: [
      { index: true, element: withSuspense(LoginPage) },
    ],
  },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      { index: true,   element: withSuspense(AdminDashboard) },
      { path: 'posts', element: withSuspense(AdminPostsPage) },
      { path: 'claims', element: withSuspense(AdminClaimsPage) },
      { path: 'users', element: withSuspense(AdminUsersPage) },
    ],
  },
  {
    path: '*',
    element: withSuspense(NotFoundPage),
  },
]);

export default function AppRouter() {
  return <RouterProvider router={router} />;
}
