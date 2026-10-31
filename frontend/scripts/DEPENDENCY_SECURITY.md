# Dependency security maintenance

The October 2026 security refresh keeps Vue 3 and Vite 8. It updates Vue I18n
9 → 11 and ECharts 5 → 6 because their older major lines have no suitable
security fix. The application already uses Vue I18n composition mode and JIT;
production messages already escape literal `@`. ECharts modular imports remain
unchanged. Unit/render/build checks are not browser visual acceptance.

Migration references:
- https://vue-i18n.intlify.dev/guide/migration/breaking10
- https://vue-i18n.intlify.dev/guide/migration/breaking11
- https://echarts.apache.org/handbook/en/basics/release-note/v6-upgrade-guide/

## Temporary Redocly compatibility bridge

Latest `openapi-typescript` 7.13.0 depends on Redocly 1.34.22 and imports its
private `lib/ref-utils.js`. Overriding Redocly to 2.72.1 was tested and fails
with `ERR_PACKAGE_PATH_NOT_EXPORTED`; blindly downgrading OpenAPI tooling is
not an acceptable repair. Redocly 1 requires the old callable minimatch export,
whereas the patched minimatch 11 exposes the named `minimatch` function.

A scoped npm override installs the actual patched minimatch 11.0.1. The
`postinstall` script adapts exactly that one Redocly CommonJS import. It has
no network or shell calls, checks the exact package version and SHA-256 of
the entire source file, and fails closed on any unexpected input. Repeated
installation accepts only the known patched hash. It does not modify audit
reports, vulnerability databases, or severity thresholds.

Normal `npm ci` applies the bridge. Production-only `npm ci --omit=dev`
skips it only when npm explicitly omits dev dependencies and OpenAPI tooling
is actually absent; broken installed packages or unexpected resolution failures
still fail closed. If deliberately using `--ignore-scripts`,
run `npm run postinstall` before checks or API generation. Do not remove the
bridge while retaining the major-version override. Once upstream OpenAPI
supports a secure Redocly release, remove both after running the reference
bundling/type-generation and positive/negative URL glob compatibility tests.

Regression tests: `src/lib/dependencyCompatibility.test.ts`. Validate with
`npm ci`, `npm run check`, and `npm audit --audit-level=moderate`. Lockfile
artifacts use the official npm registry and retain integrity hashes.
