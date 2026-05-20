# Scrapling Price Service

FastAPI microservice wrapping Scrapling for scenic spot ticket price discovery.

## Endpoints

- `GET /health` returns service and Scrapling runtime metadata.
- `GET /scrape/capabilities` returns enabled modes, search providers, and limits.
- `POST /scrape/price` extracts ticket prices for a scenic spot.

Example request:

```json
{
  "spotName": "布达拉宫",
  "location": "拉萨",
  "mode": "basic",
  "maxSources": 4
}
```

The response keeps the backend-compatible fields `basePrice`, `peakSeasonPrice`,
`offSeasonPrice`, `source`, `confidence`, and `rawData`, and adds `evidence`
plus `queriedUrls` for diagnostics.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `SCRAPLING_MODE` | `basic` | `basic`, `stealth`, `dynamic`, or `httpx`. |
| `SCRAPLING_TIMEOUT_SECONDS` | `30` | Per-source fetch timeout. |
| `SCRAPLING_RETRIES` | `2` | Retries for Scrapling's HTTP fetcher. |
| `SCRAPLING_MAX_SOURCES` | `4` | Maximum search result pages queried per request. |
| `SCRAPLING_SEARCH_PROVIDERS` | `baidu,bing` | Comma-separated providers: `baidu`, `bing`, `sogou`. |
| `SCRAPLING_PROXY_URL` | empty | Optional outbound proxy URL. |
| `SCRAPLING_SOLVE_CLOUDFLARE` | `false` | Enables Scrapling stealth Cloudflare solving where supported. |
| `SCRAPLING_ALLOWED_DOMAINS` | empty | Reserved allowlist for direct target scraping. |

## Local Checks

```powershell
scrapler\.venv\Scripts\python.exe -m unittest discover -s scrapler -p "test_*.py"
```
