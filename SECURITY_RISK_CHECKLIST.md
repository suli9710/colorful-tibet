# Security Risk Checklist

Remove each item after it is fully fixed and verified.

- [ ] JWT is stored in `localStorage`: `frontend/src/stores/auth.ts` and `frontend/src/api/index.ts` keep bearer tokens readable by JavaScript.
- [ ] CSRF is globally disabled: `WebSecurityConfig` disables CSRF; this must be revisited if auth moves to cookies.
