# Auth Integration Guide

## How the JWT flow works

### 1. Login (Email/Password) - Using Hook (Recommended)
```tsx
"use client";
import { useAuth } from "@/modules/auth";

export default function LoginPage() {
  const { loginWithEmail } = useAuth();

  async function handleLogin(email: string, password: string) {
    await loginWithEmail({ email, password });
    // Automatically stores token and redirects based on role
  }

  return <form onSubmit={(e) => { /* ... */ }}>{/* ... */}</form>;
}
```

### 1b. Login (Direct API Call)
```tsx
import { authApi } from "@/modules/auth";

async function handleLogin(email: string, password: string) {
  const { accessToken, role } = await authApi.loginWithEmail({ email, password });
  window.__mysawit_access_token = accessToken;
  router.push(`/${role.toLowerCase()}`);
}
```

### 2. Login (Google OAuth2) - Using Hook (Recommended)
```tsx
"use client";
import { useAuth } from "@/modules/auth";

export default function LoginPage() {
  const { loginWithGoogle } = useAuth();

  return (
    <button onClick={loginWithGoogle}>
      Login with Google
    </button>
  );
}

// In app/auth/callback/page.tsx (handles OAuth redirect)
"use client";
import { useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { useAuth } from "@/modules/auth";

export default function AuthCallback() {
  const { handleOAuthCallback } = useAuth();
  const searchParams = useSearchParams();

  useEffect(() => {
    handleOAuthCallback(searchParams);
  }, [searchParams, handleOAuthCallback]);

  return <div>Authenticating...</div>;
}
```

### 3. Register
```tsx
"use client";
import { useAuth } from "@/modules/auth";

export default function RegisterPage() {
  const { register } = useAuth();

  async function handleRegister() {
    await register({
      email: "user@example.com",
      password: "password123",
      name: "John Doe",
      role: "BURUH"
    });
    // Redirect manually or show success message
  }

  return <form onSubmit={(e) => { /* ... */ }}>{/* ... */}</form>;
}
```

### 4. Logout - Using Hook (Recommended)
```tsx
"use client";
import { useAuth } from "@/modules/auth";

export default function Dashboard() {
  const { logout } = useAuth();

  return <button onClick={logout}>Logout</button>;
}
```

### 5. Automatic Token Attachment
Once `window.__mysawit_access_token` is set, the Axios client automatically:
- Adds `Authorization: Bearer <token>` to all requests
- Handles 401 errors by clearing token and redirecting to `/login`

### 6. Protected Routes (Middleware Example)
```tsx
// middleware.ts
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

export function middleware(request: NextRequest) {
  // Note: Token is in-memory, so SSR can't access it
  // Either use public routes or check auth client-side in components
  const publicRoutes = ["/", "/login", "/register", "/auth/callback"];
  
  if (publicRoutes.includes(request.nextUrl.pathname)) {
    return NextResponse.next();
  }
  
  // For protected routes, render page and let client check auth
  return NextResponse.next();
}
```

### 7. Client-Side Auth Check - Using Hook (Recommended)
```tsx
"use client";
import { useEffect } from "react";
import { useAuth } from "@/modules/auth";
import { useRouter } from "next/navigation";

export default function ProtectedPage() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();
  
  useEffect(() => {
    if (!isAuthenticated()) {
      router.push("/login");
    }
  }, [isAuthenticated, router]);
  
  // Your protected content
  return <div>Protected content</div>;
}
```

## useAuth Hook API

```tsx
const {
  loginWithEmail,       // (credentials) => Promise<{accessToken, role}>
  register,             // (data) => Promise<UserDTO>
  loginWithGoogle,      // () => Promise<void> - redirects to Google
  handleOAuthCallback,  // (searchParams) => Promise<role | null>
  logout,               // () => Promise<void>
  getToken,             // () => string | undefined
  isAuthenticated,      // () => boolean
} = useAuth();
```

## Quick Import Reference

```tsx
// Barrel export - use any of these:
import { useAuth } from "@/modules/auth";
import { authApi } from "@/modules/auth";
import type { UserDTO, LoginRequest, RegisterRequest } from "@/modules/auth";
```

## Key Changes Made to client.ts

1. **Removed `withCredentials: true`** - Not needed for JWT (only for cookies)
2. **Simplified 401 handler** - Clears token and redirects to `/login`
3. **Added pathname check** - Prevents redirect loop on login page
4. **Removed retry logic** - Stateless JWT doesn't need refresh token flow

## Security Notes

- Token stored in `window` object (not localStorage) - safer from XSS
- Token automatically cleared on 401 responses
- Backend should set short JWT expiration (15-30 min recommended)
