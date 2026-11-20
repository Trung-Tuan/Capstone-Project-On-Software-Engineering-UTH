import type { Post, PostType, PostStatus } from '@types/index';

const STORAGE_POSTS_KEY = 'lnf_mock_posts';

export const CURRENT_USER_ID = 'current_user';

export const INITIAL_POSTS: Post[] = [
  {
    id: '1',
    type: 'LOST',
    title: 'Mất ví da nam màu đen Calvin Klein tại Căng tin',
    description: `Ví da màu đen gập đôi, bên trong có thẻ sinh viên trường ĐH Giao thông Vận tải TP.HCM (khoa CNTT), thẻ xe buýt và một ít tiền mặt.

📍 Vị trí mất: Quên trên bàn số 4 khu vực Căng tin trung tâm vào khoảng 11h30 trưa.
Đặc điểm nhận diện riêng: Mặt trong ví có đường chỉ may màu xanh dương nhạt và 1 chiếc móc kim loại nhỏ.

Lưu ý: Mình đã khóa các thẻ ngân hàng liên quan. Bạn nào nhặt được vui lòng cho mình xin lại giấy tờ, mình xin cảm ơn và hậu tạ!`,
    category: 'wallet',
    location: 'canteen',
    date: '2024-01-15',
    imageUrls: [
      'https://images.unsplash.com/photo-1627123424574-724758594e93?auto=format&fit=crop&w=1000&q=80',
      'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=1000&q=80',
      'https://images.unsplash.com/photo-1548036328-c9fa89d128fa?auto=format&fit=crop&w=1000&q=80',
    ],
    status: 'ACTIVE',
    authorId: CURRENT_USER_ID,
    author: {
      id: CURRENT_USER_ID,
      name: 'Nguyễn Văn An (Tôi)',
      avatarUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80',
    },
    createdAt: '2024-01-15T11:45:00Z',
    updatedAt: '2024-01-15T11:45:00Z',
  },
  {
    id: '2',
    type: 'FOUND',
    title: 'Tìm thấy laptop Dell Inspiron màu bạc tại Thư viện',
    description: `Chiều nay tại Thư viện tầng 3 (khu vực bàn học nhóm dãy C), mình có nhặt được 1 chiếc laptop Dell Inspiron 14 inch màu bạc để quên trong bao chống sốc màu xám.

Máy có dán sticker hình logo UTH và 1 sticker GitHub ở nắp lưng.
Hiện tại mình đã gửi lại bàn trực thủ thư Thư viện tầng 1 để bảo quản an toàn. Bạn nào là chủ nhân vui lòng mang theo thẻ sinh viên và mật khẩu mở máy đến đối chiếu để nhận lại nhé!`,
    category: 'electronics',
    location: 'library',
    date: '2024-01-14',
    imageUrls: [
      'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?auto=format&fit=crop&w=1000&q=80',
      'https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?auto=format&fit=crop&w=1000&q=80',
    ],
    status: 'ACTIVE',
    authorId: 'user_2',
    author: {
      id: 'user_2',
      name: 'Trần Thị Mai',
      avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=200&q=80',
    },
    createdAt: '2024-01-14T15:20:00Z',
    updatedAt: '2024-01-14T15:20:00Z',
  },
  {
    id: '3',
    type: 'LOST',
    title: 'Rơi chùm chìa khóa xe Honda Smartkey + thẻ giữ xe',
    description: `Khoảng 17h00 chiều qua sau giờ học tại Phòng Lab Tòa B, mình di chuyển xuống bãi xe thì phát hiện làm rơi chùm chìa khóa.

Chùm gồm: 1 smartkey xe Lead màu đen, 1 móc khóa bông hình gấu dâu Lotso màu hồng và 1 thẻ từ thang máy chung cư.
Bạn nào nhặt được liên hệ giúp mình qua số điện thoại hoặc gửi phòng bảo vệ cổng chính nhé, mình xin cảm ơn nhiều!`,
    category: 'keys',
    location: 'parking',
    date: '2024-01-13',
    imageUrls: [
      'https://images.unsplash.com/photo-1582139329536-e7284fece509?auto=format&fit=crop&w=1000&q=80',
    ],
    status: 'ACTIVE',
    authorId: 'user_3',
    author: {
      id: 'user_3',
      name: 'Lê Hoàng Long',
      avatarUrl: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=200&q=80',
    },
    createdAt: '2024-01-13T17:30:00Z',
    updatedAt: '2024-01-13T17:30:00Z',
  },
  {
    id: '4',
    type: 'FOUND',
    title: 'Nhặt được Thẻ sinh viên UTH mang tên Phạm Minh Tuấn',
    description: `Mình nhặt được 1 thẻ sinh viên rơi ở ghế đá trước sân bóng rổ trường vào chiều ngày 12/01.
Thông tin trên thẻ:
- Sinh viên: Phạm Minh Tuấn
- Khóa: 2021
- Khoa: Kinh tế vận tải

Để bảo mật thông tin cá nhân, mình đã che phần mã số SV và ngày sinh. Tuấn hoặc bạn bè của Tuấn thấy tin này liên hệ mình để nhận lại nhé!`,
    category: 'documents',
    location: 'sports-ground',
    date: '2024-01-12',
    imageUrls: [
      'https://images.unsplash.com/photo-1589330694653-dad6d3240a2b?auto=format&fit=crop&w=1000&q=80',
    ],
    status: 'ACTIVE',
    authorId: CURRENT_USER_ID,
    author: {
      id: CURRENT_USER_ID,
      name: 'Nguyễn Văn An (Tôi)',
      avatarUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80',
    },
    createdAt: '2024-01-12T16:00:00Z',
    updatedAt: '2024-01-12T16:00:00Z',
  },
  {
    id: '5',
    type: 'LOST',
    title: 'Bỏ quên tai nghe AirPods Pro 2 tại Tòa nhà A',
    description: `Sáng nay học tiết 1-3 tại phòng A204 Tòa A, mình có để quên hộp tai nghe AirPods Pro 2 có ốp silicon màu xanh rêu.
Bên trong có 2 tai nghe đầy đủ. Ốp có móc khóa kim loại nhỏ.

Vì đồ vật có giá trị khá lớn và gắn liền với việc học tập nên mình rất mong nhận lại. Mình xin hậu tạ chu đáo cho bạn nào tìm thấy!`,
    category: 'electronics',
    location: 'classroom-a',
    date: '2024-01-11',
    imageUrls: [
      'https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?auto=format&fit=crop&w=1000&q=80',
      'https://images.unsplash.com/photo-1572569511254-d8f925fe2cbb?auto=format&fit=crop&w=1000&q=80',
    ],
    status: 'RESOLVED',
    authorId: CURRENT_USER_ID,
    author: {
      id: CURRENT_USER_ID,
      name: 'Nguyễn Văn An (Tôi)',
      avatarUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80',
    },
    createdAt: '2024-01-11T10:15:00Z',
    updatedAt: '2024-01-11T14:30:00Z',
  },
  {
    id: '6',
    type: 'FOUND',
    title: 'Nhặt được balo màu đen Adidas tại Cổng trường',
    description: `Tại ghế đá gần Cổng số 1, có 1 chiếc balo thể thao màu đen thương hiệu Adidas bị bỏ quên từ trưa.
Bên trong có: 2 cuốn giáo trình Toán cao cấp, 1 bình nước giữ nhiệt màu bạc và 1 hộp bút.

Đã gửi phòng Bảo vệ cổng trường số 1 để chủ nhân đến nhận. Nhớ miêu tả đúng các vật dụng bên trong để các chú bảo vệ đối chiếu nhé!`,
    category: 'bags',
    location: 'entrance',
    date: '2024-01-10',
    imageUrls: [
      'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=1000&q=80',
    ],
    status: 'ACTIVE',
    authorId: 'user_4',
    author: {
      id: 'user_4',
      name: 'Võ Thị Hồng Vân',
      avatarUrl: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=200&q=80',
    },
    createdAt: '2024-01-10T12:00:00Z',
    updatedAt: '2024-01-10T12:00:00Z',
  },
];

// Helper to get posts from localStorage
export function getStoredPosts(): Post[] {
  try {
    const raw = localStorage.getItem(STORAGE_POSTS_KEY);
    if (!raw) {
      localStorage.setItem(STORAGE_POSTS_KEY, JSON.stringify(INITIAL_POSTS));
      return INITIAL_POSTS;
    }
    return JSON.parse(raw) as Post[];
  } catch {
    return INITIAL_POSTS;
  }
}

// Helper to save posts
export function saveStoredPosts(posts: Post[]): void {
  try {
    localStorage.setItem(STORAGE_POSTS_KEY, JSON.stringify(posts));
  } catch (err) {
    console.error('Failed to save posts to localStorage', err);
  }
}

// Get post by ID
export function getPostById(id: string): Post | null {
  const posts = getStoredPosts();
  return posts.find(p => p.id === id) || null;
}

// Get posts for current user
export function getMyPosts(): Post[] {
  const posts = getStoredPosts();
  return posts.filter(p => p.authorId === CURRENT_USER_ID);
}

// Toggle or update status (e.g. mark as RESOLVED "Đã nhận lại")
export function updatePostStatus(id: string, status: PostStatus): Post | null {
  const posts = getStoredPosts();
  const index = posts.findIndex(p => p.id === id);
  if (index === -1) return null;

  posts[index] = {
    ...posts[index],
    status,
    updatedAt: new Date().toISOString(),
  };

  saveStoredPosts(posts);
  return posts[index];
}

// Update post contents (Edit post)
export function updatePost(
  id: string,
  updatedData: Partial<Omit<Post, 'id' | 'author' | 'authorId' | 'createdAt'>>,
): Post | null {
  const posts = getStoredPosts();
  const index = posts.findIndex(p => p.id === id);
  if (index === -1) return null;

  posts[index] = {
    ...posts[index],
    ...updatedData,
    updatedAt: new Date().toISOString(),
  };

  saveStoredPosts(posts);
  return posts[index];
}

// Delete post
export function deletePost(id: string): boolean {
  const posts = getStoredPosts();
  const filtered = posts.filter(p => p.id !== id);
  if (filtered.length === posts.length) return false;

  saveStoredPosts(filtered);
  return true;
}
