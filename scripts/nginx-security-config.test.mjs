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

  it('renders the real-ip config both templates promise', () => {
    // Without this the documented NGINX_REAL_IP_FROM knob is silently inert and, behind a CDN,
    // $binary_remote_addr stays the proxy address so every limit_req/limit_conn zone collapses
    // into one bucket shared by the whole internet.
    assert.match(frontendDockerfile, /render_real_ip\(\) \{/)
    assert.match(frontendDockerfile, /set_real_ip_from \$trusted_cidr;/)
    assert.match(frontendDockerfile, /real_ip_header \$real_ip_header_name;/)
    assert.match(frontendDockerfile, /\/etc\/nginx\/conf\.d\/real-ip\.conf/)
    assert.match(frontendDockerfile, /NGINX_REAL_IP_FROM entries must be IPv4\/IPv6 addresses or CIDRs/)

    const call = frontendDockerfile.indexOf("'render_real_ip'")
    const httpRender = frontendDockerfile.indexOf('/etc/nginx/http.conf.template', call)
    assert.ok(call >= 0, 'render_real_ip is never invoked')
    assert.ok(httpRender > call, 'real-ip.conf must be rendered before the server templates')
  })

  it('never lets a caller contribute to the forwarded client IP', () => {
    for (const [name, conf] of [['nginx.conf', prodNginx], ['nginx.http.conf', httpNginx]]) {
      assert.doesNotMatch(
        conf,
        /proxy_set_header\s+X-Forwarded-For\s+\$proxy_add_x_forwarded_for/,
        `${name} must overwrite X-Forwarded-For, not append to the caller's value`
      )
      assert.match(conf, /proxy_set_header X-Forwarded-For \$remote_addr;/)
      assert.match(conf, /proxy_set_header Forwarded "";/)
      assert.match(conf, /proxy_set_header X-Forwarded-Host "";/)
    }
  })

  it('keeps nginx as PID 1 so container stop shuts it down gracefully', () => {
    assert.match(frontendDockerfile, /exec nginx -g/)
  })
})
