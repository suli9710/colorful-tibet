"""FastAPI microservice for scenic spot price scraping using Scrapling."""

from __future__ import annotations

import importlib.metadata
import asyncio
import logging
import os
import secrets
from typing import Optional

from dotenv import load_dotenv
from fastapi import Depends, FastAPI, Header, HTTPException, status
from pydantic import BaseModel, Field
from starlette.concurrency import run_in_threadpool

from scraper import (
    ScrapeResult,
    ScraperConfig,
    normalize_config,
    scrape_price_for_spot,
)

load_dotenv()

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
logging.basicConfig(level=LOG_LEVEL, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger("scrapling-service")

SERVICE_VERSION = "0.3.0"
API_KEY_HEADER = "X-Scrapling-Api-Key"


def _env_int(name: str, default: int) -> int:
    try:
        return int(os.getenv(name, str(default)))
    except ValueError:
        logger.warning("Invalid integer for %s; using default=%s", name, default)
        return default


def _env_bool(name: str, default: bool = False) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


def _env_tuple(name: str, default: tuple[str, ...]) -> tuple[str, ...]:
    raw = os.getenv(name)
    if not raw:
        return default
    values = tuple(value.strip() for value in raw.split(",") if value.strip())
    return values or default


def load_config() -> ScraperConfig:
    return ScraperConfig(
        mode=os.getenv("SCRAPLING_MODE", "basic").strip().lower(),
        timeout=_env_int("SCRAPLING_TIMEOUT_SECONDS", 30),
        retries=_env_int("SCRAPLING_RETRIES", 2),
        max_sources=max(1, min(10, _env_int("SCRAPLING_MAX_SOURCES", 4))),
        search_providers=_env_tuple("SCRAPLING_SEARCH_PROVIDERS", ("baidu", "bing")),
        allowed_domains=_env_tuple("SCRAPLING_ALLOWED_DOMAINS", ()),
        proxy_url=os.getenv("SCRAPLING_PROXY_URL") or None,
        solve_cloudflare=_env_bool("SCRAPLING_SOLVE_CLOUDFLARE", False),
    )


def scrapling_version() -> Optional[str]:
    try:
        return importlib.metadata.version("scrapling")
    except importlib.metadata.PackageNotFoundError:
        return None


BASE_CONFIG = load_config()
MAX_CONCURRENCY = max(1, _env_int("SCRAPLING_MAX_CONCURRENCY", 2))
SCRAPE_SEMAPHORE = asyncio.Semaphore(MAX_CONCURRENCY)
app = FastAPI(title="Scrapling Price Service", version=SERVICE_VERSION)


class ScrapeRequest(BaseModel):
    spotName: str = Field(..., min_length=1, max_length=120)
    location: Optional[str] = Field(default=None, max_length=120)
    mode: Optional[str] = Field(default=None, pattern="^(basic|stealth|dynamic|httpx)$")
    maxSources: Optional[int] = Field(default=None, ge=1, le=10)


class PriceEvidenceResponse(BaseModel):
    price: float
    kind: str
    sourceUrl: str
    context: str
    weight: float


class PriceResponse(BaseModel):
    basePrice: Optional[float] = None
    peakSeasonPrice: Optional[float] = None
    offSeasonPrice: Optional[float] = None
    source: str = "Scrapling"
    confidence: float = 0.0
    rawData: Optional[str] = None
    evidence: list[PriceEvidenceResponse] = Field(default_factory=list)
    queriedUrls: list[str] = Field(default_factory=list)


def configured_api_key() -> str:
    return (os.getenv("SCRAPLING_API_KEY") or "").strip()


def allow_request_mode_override() -> bool:
    return _env_bool("SCRAPLING_ALLOW_REQUEST_MODE_OVERRIDE", False)


def require_api_key(api_key: Optional[str] = Header(default=None, alias=API_KEY_HEADER)) -> None:
    expected = configured_api_key()
    if not expected:
        return

    provided = (api_key or "").strip()
    if not provided or not secrets.compare_digest(provided, expected):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized",
        )


def response_from_result(result: ScrapeResult) -> PriceResponse:
    return PriceResponse(
        basePrice=float(result.base_price) if result.base_price is not None else None,
        peakSeasonPrice=float(result.peak_price) if result.peak_price is not None else None,
        offSeasonPrice=float(result.off_price) if result.off_price is not None else None,
        source=result.source,
        confidence=result.confidence,
        rawData=result.raw_data,
        evidence=[PriceEvidenceResponse(**item.as_dict()) for item in result.evidence],
        queriedUrls=list(result.queried_urls),
    )


@app.post("/scrape/price", response_model=PriceResponse, dependencies=[Depends(require_api_key)])
async def scrape_price(request: ScrapeRequest) -> PriceResponse:
    spot_name = request.spotName.strip()
    if not spot_name:
        raise HTTPException(status_code=400, detail="spotName must not be blank")

    request_mode = request.mode if allow_request_mode_override() else None
    active_config = normalize_config(BASE_CONFIG, mode=request_mode, max_sources=request.maxSources)
    logger.info(
        "Scraping price for spot=%s location=%s mode=%s max_sources=%s",
        spot_name,
        request.location,
        active_config.mode,
        active_config.max_sources,
    )

    async with SCRAPE_SEMAPHORE:
        result = await run_in_threadpool(
            scrape_price_for_spot,
            spot_name=spot_name,
            location=request.location,
            config=active_config,
        )
    return response_from_result(result)


@app.get("/scrape/capabilities", dependencies=[Depends(require_api_key)])
async def capabilities():
    return {
        "serviceVersion": SERVICE_VERSION,
        "scraplingVersion": scrapling_version(),
        "modes": ["basic", "stealth", "dynamic", "httpx"],
        "defaultMode": BASE_CONFIG.mode,
        "searchProviders": BASE_CONFIG.search_providers,
        "maxSources": BASE_CONFIG.max_sources,
        "allowedDomains": BASE_CONFIG.allowed_domains,
        "proxyConfigured": bool(BASE_CONFIG.proxy_url),
        "solveCloudflare": BASE_CONFIG.solve_cloudflare,
        "maxConcurrency": MAX_CONCURRENCY,
        "requestModeOverrideAllowed": allow_request_mode_override(),
    }


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "serviceVersion": SERVICE_VERSION,
        "scraplingVersion": scrapling_version(),
        "mode": BASE_CONFIG.mode,
        "searchProviders": BASE_CONFIG.search_providers,
        "maxSources": BASE_CONFIG.max_sources,
    }
