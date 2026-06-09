import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { describe, it } from 'node:test'

const prodNginx = readFileSync('frontend/nginx.conf', 'utf8').replace(/\r\n/g, '\n')
const httpNginx = readFileSync('frontend/nginx.http.conf', 'utf8').replace(/\r\n/g, '\n')
const frontendDockerfile = readFileSync('frontend/Dockerfile', 'utf8').replace(/\r\n/g, '\n')

describe('nginx edge security config', () => {
  it('fails closed for unmatched HTTP and HTTPS hosts', () => {
    assert.match(
      prodNginx,
      /server\s*\{\s*listen\s+8080\s+default_server;[\s\S]*?server_name\s+_;[\s\S]*?return\s+444;[\s\S]*?\}/
    )
    assert.match(
      prodNginx,
      /server\s*\{\s*listen\s+8443\s+ssl\s+default_server;[\s\S]*?server_name\s+_;[\s\S]*?return\s+444;[\s\S]*?\}/
    )

    const defaultTlsIndex = prodNginx.indexOf('listen 8443 ssl default_server;')
    const appTlsIndex = prodNginx.indexOf('listen 8443 ssl;', defaultTlsIndex + 1)
    assert.ok(defaultTlsIndex >= 0, 'missing HTTPS default server')
    assert.ok(appTlsIndex > defaultTlsIndex, 'HTTPS default server must be declared before the app server')
  })

  it('keeps the local HTTP template scoped to the configured server name', () => {
    assert.doesNotMatch(httpNginx, /listen\s+8080\s+default_server;/)
    assert.match(httpNginx, /server_name\s+\$\{NGINX_SERVER_NAME\};/)
    assert.match(httpNginx, /location\s+\^~\s+\/api\//)
    assert.match(httpNginx, /proxy_pass\s+http:\/\/backend:8080;/)
  })

  it('validates nginx runtime env before rendering templates', () => {
    assert.match(frontendDockerfile, /set -f/)
    assert.match(frontendDockerfile, /require_server_names "\$NGINX_SERVER_NAME"/)
    assert.match(frontendDockerfile, /require_single_host NGINX_REDIRECT_HOST "\$NGINX_REDIRECT_HOST"/)
    assert.match(frontendDockerfile, /require_single_host NGINX_CERT_DOMAIN "\$NGINX_CERT_DOMAIN"/)
    assert.match(frontendDockerfile, /must be a single DNS host without scheme, path, port, comma, whitespace, or control characters/)

    const redirectCheck = frontendDockerfile.indexOf('require_single_host NGINX_REDIRECT_HOST')
    const certCheck = frontendDockerfile.indexOf('require_single_host NGINX_CERT_DOMAIN')
    const tlsRender = frontendDockerfile.indexOf('/etc/nginx/default.conf.template', redirectCheck)
    assert.ok(redirectCheck >= 0 && redirectCheck < tlsRender)
    assert.ok(certCheck >= 0 && certCheck < tlsRender)
  })
})
