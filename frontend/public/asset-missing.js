(() => {
  const key = 'colorful-tibet:asset-reload-at';
  const now = Date.now();
  const lastReloadAt = Number(sessionStorage.getItem(key) || 0);

  if (now - lastReloadAt > 10000) {
    sessionStorage.setItem(key, String(now));
    const url = new URL(window.location.href);
    url.searchParams.set('_reload', String(now));
    window.location.replace(url.toString());
    return;
  }

  console.error('Colorful Tibet asset is stale. Please refresh the page or clear the browser cache.');
})();
