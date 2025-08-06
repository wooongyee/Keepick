# Keepick FSD 아키텍처 가이드

## 🏗️ 레이어 구조

```
app/ → widgets/ → features/ → entities/ → shared/
```

**핵심 규칙**: 하위 레이어만 import 가능, 같은 레이어 간 cross-import 금지

## 📂 각 레이어 역할

### `entities/` - 비즈니스 엔티티
```
entities/user/
├── model/userSlice.ts    # 사용자 데이터 상태
└── index.ts
```
- 순수한 비즈니스 데이터 관리
- `shared/`만 import 가능

### `features/` - 비즈니스 기능  
```
features/auth/
├── api/authApi.ts
├── model/authSlice.ts
├── ui/SocialLoginButton.tsx
└── index.ts
```
- 격리된 비즈니스 기능 구현
- `entities/`, `shared/` import 가능
- **다른 features import 절대 금지**

### `widgets/` - 복합 UI 블록
```
widgets/auth/
├── ui/LoginWidget.tsx    # 여러 features 조합
└── index.ts
```
- 여러 features를 조합한 복합 컴포넌트

### `shared/` - 공유 리소스  
```
shared/
├── api/http/            # HTTP 클라이언트
├── config/store.ts      # Redux 스토어
├── types/              # 글로벌 타입
└── ui/                 # 기본 UI 컴포넌트
```
- 어떤 상위 레이어도 import 금지
- 순수 유틸리티와 인프라만

## 🔄 Next.js App Router 구조 조정

**Next.js App Router 사용으로 인한 FSD 구조 조정:**
- **`app/`**: Next.js 라우팅 페이지 + 전역 설정을 providers 폴더로 구분
- **`pages/`**: 레거시 페이지 컴포넌트 (점진적 마이그레이션 예정)

```
app/
├── login/page.tsx        # 라우팅 페이지
├── profile/page.tsx      # 라우팅 페이지  
├── layout.tsx           # 전역 레이아웃
└── providers/           # 전역 설정 (구분)
    ├── Providers.tsx
    ├── StoreProvider.tsx
    └── QueryProvider.tsx

pages/
└── LoginPage.tsx        # 레거시 (마이그레이션 예정)
```

## ✅ 사용 예시

### 올바른 사용
```typescript
// features/auth → entities, shared
import { User } from '@/entities/user'
import { apiClient } from '@/shared/api'

// widgets → features  
import { SocialLoginButton } from '@/features/auth'
```

### 잘못된 사용
```typescript
// ❌ features → features (금지!)
import { profileFeature } from '@/features/profile'

// ❌ shared → 상위 레이어 (금지!)
import { authSlice } from '@/features/auth'
```

## 🚦 Redux 스토어 구조

```typescript
// shared/config/store.ts
const store = configureStore({
  reducer: {
    user: userReducer,     // from entities/user
    auth: authReducer,     // from features/auth
  }
})
```

## 📝 파일 명명 규칙

- Slice: `userSlice.ts`, `authSlice.ts`
- API: `userApi.ts`, `authApi.ts`  
- Hook: `useAuth.ts`, `useProfile.ts`
- Component: `SocialLoginButton.tsx`

## 🏷️ 타입(Types) 관리 전략

### 모듈별 타입 분리 방식 (권장)

각 모듈의 책임에 따라 타입을 분리하여 관리합니다.

```
features/auth/
├── api/
│   ├── authApi.ts         # API 함수
│   └── types.ts          # API 관련 타입
├── model/
│   ├── authSlice.ts      # Redux slice
│   └── types.ts          # 상태 관련 타입
├── ui/
│   ├── SocialLoginButton.tsx
│   └── types.ts          # UI 컴포넌트 Props 타입
├── types.ts              # 공통 auth 타입
└── index.ts              # 모든 타입 re-export
```

### 타입 분류 기준

#### 1. API 관련 타입 → `api/types.ts`
```typescript
// features/auth/api/types.ts
export interface UserResponse {
  user: {
    id: number;
    email: string;
    nickname: string;
    profileUrl?: string;
  };
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken?: string;
}
```

#### 2. 상태 관리 타입 → `model/types.ts`
```typescript
// features/auth/model/types.ts
export interface AuthState {
  isAuthenticated: boolean;
  accessToken: string | null;
  refreshToken: string | null;
  isLoading: boolean;
}
```

#### 3. UI 컴포넌트 타입 → `ui/types.ts`
```typescript
// features/auth/ui/types.ts
export interface SocialLoginButtonProps {
  provider: SocialProvider;
  disabled?: boolean;
  className?: string;
}
```

#### 4. 공통 기능 타입 → `types.ts`
```typescript
// features/auth/types.ts
export type SocialProvider = 'kakao' | 'naver' | 'google';

export interface AuthCredentials {
  email: string;
  password: string;
}
```

### Re-export 패턴

```typescript
// features/auth/index.ts
// API 타입
export type { 
  UserResponse, 
  TokenRefreshResponse 
} from './api/types';

// 상태 타입
export type { AuthState } from './model/types';

// UI 타입  
export type { SocialLoginButtonProps } from './ui/types';

// 공통 타입
export type { 
  SocialProvider, 
  AuthCredentials 
} from './types';
```

### 글로벌 vs 도메인 타입

#### 글로벌 타입 → `shared/types/`
```typescript
// shared/types/api.ts - 공통 API 응답 구조
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

// shared/types/common.ts - 공통 유틸리티 타입
export type ID = string | number;
export type Timestamp = string;
```

#### 도메인 타입 → 해당 레이어
```typescript
// entities/user/model/types.ts - 사용자 도메인
export interface User {
  id: number;
  nickname: string;
  profileUrl?: string;
}

// features/auth/types.ts - 인증 기능 도메인
export type SocialProvider = 'kakao' | 'naver' | 'google';
```

### 장점

- ✅ **응집도 향상**: 관련 타입들이 해당 모듈에 위치
- ✅ **유지보수성**: 타입 변경 시 해당 모듈만 수정
- ✅ **가독성**: 각 모듈의 책임이 명확히 구분
- ✅ **확장성**: 기능 확장 시 타입도 함께 확장 가능

## ⚠️ 주의사항

1. **features 간 cross-import 절대 금지**
2. **shared에서 상위 레이어 import 금지**  
3. **의존성 방향 항상 확인**: 하위 → 상위만 가능
4. **기존 components/ 폴더는 점진적 마이그레이션 예정**

---
*코드 리뷰 시 위 규칙들을 반드시 확인해주세요.*