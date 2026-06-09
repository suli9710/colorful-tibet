"""Scrapling-backed price scraping for scenic spot tickets."""

from __future__ import annotations

import html
import ipaddress
import json
import logging
import re
import socket
import urllib.parse
from collections import Counter, defaultdict
from dataclasses import dataclass, replace
from decimal import ROUND_HALF_UP, Decimal
from typing import Any, Iterable, Optional

import httpx

logger = logging.getLogger(__name__)

DEFAULT_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
}

SEARCH_PROVIDERS: dict[str, str] = {
    "baidu": "https://www.baidu.com/s?wd={query}",
    "bing": "https://www.bing.com/search?q={query}",
    "sogou": "https://www.sogou.com/web?query={query}",
}

TRUSTED_PRICE_HOST_HINTS = (
    "ctrip.com",
    "trip.com",
    "qunar.com",
    "mafengwo.cn",
    "ly.com",
    "meituan.com",
    "dianping.com",
    "lvmama.com",
    "gov.cn",
    "xizang.gov.cn",
)

PRICE_KEYWORDS = (
    "门票",
    "票价",
    "价格",
    "成人票",
    "全价票",
    "景区门票",
    "旺季",
    "淡季",
    "优惠票",
    "学生票",
    "元/人",
    "元/张",
)

PRICE_PATTERNS: tuple[tuple[str, re.Pattern[str], float], ...] = (
    ("peak", re.compile(r"(?:旺季|旺季门票|旺季票价)[：:\s]*[¥￥]?\s*(\d+(?:\.\d+)?)\s*(?:元|块|人民币|CNY|RMB)", re.I), 1.15),
    ("off", re.compile(r"(?:淡季|淡季门票|淡季票价)[：:\s]*[¥￥]?\s*(\d+(?:\.\d+)?)\s*(?:元|块|人民币|CNY|RMB)", re.I), 1.15),
    ("base", re.compile(r"(?:门票价格|景区门票|门票|票价|价格|挂牌价)[：:\s]*[¥￥]?\s*(\d+(?:\.\d+)?)\s*(?:元|块|人民币|CNY|RMB)", re.I), 1.1),
    ("adult", re.compile(r"(?:成人票|全价票|普通票)[：:\s]*[¥￥]?\s*(\d+(?:\.\d+)?)\s*(?:元|块|人民币|CNY|RMB)", re.I), 1.05),
    ("student", re.compile(r"(?:学生票|儿童票|老人票|优惠票)[：:\s]*[¥￥]?\s*(\d+(?:\.\d+)?)\s*(?:元|块|人民币|CNY|RMB)", re.I), 0.65),
    ("symbol", re.compile(r"[¥￥]\s*(\d+(?:\.\d+)?)"), 0.55),
    ("generic", re.compile(r"(\d+(?:\.\d+)?)\s*(?:元/人|元/张|元起|元)", re.I), 0.45),
)


@dataclass(frozen=True)
class ScraperConfig:
    mode: str = "basic"
    timeout: int = 30
    retries: int = 2
    max_sources: int = 4
    search_providers: tuple[str, ...] = ("baidu", "bing")
    allowed_domains: tuple[str, ...] = ()
    proxy_url: Optional[str] = None
    solve_cloudflare: bool = False


@dataclass(frozen=True)
class PriceEvidence:
    price: Decimal
    kind: str
    source_url: str
    context: str
    weight: float

    def as_dict(self) -> dict[str, Any]:
        return {
            "price": float(self.price),
            "kind": self.kind,
            "sourceUrl": self.source_url,
            "context": self.context,
            "weight": round(self.weight, 3),
        }


@dataclass(frozen=True)
class ScrapeResult:
    base_price: Optional[Decimal]
    peak_price: Optional[Decimal]
    off_price: Optional[Decimal]
    source: str
    confidence: float
    raw_data: str
    evidence: tuple[PriceEvidence, ...]
    queried_urls: tuple[str, ...]


def normalize_config(config: ScraperConfig, mode: Optional[str] = None, max_sources: Optional[int] = None) -> ScraperConfig:
    next_config = config
    if mode:
        normalized_mode = mode.strip().lower()
        if normalized_mode in {"basic", "stealth", "dynamic", "httpx"}:
            next_config = replace(next_config, mode=normalized_mode)
    if max_sources is not None:
        next_config = replace(next_config, max_sources=max(1, min(10, max_sources)))
    return next_config


def build_search_urls(
    spot_name: str,
    location: Optional[str] = None,
    providers: Iterable[str] = ("baidu", "bing"),
    max_sources: int = 4,
) -> list[str]:
    """Build search result URLs across configured search providers."""
    queries = [
        f"{spot_name} 门票价格",
        f"{spot_name} 门票多少钱",
        f"{spot_name} 票价",
    ]
    if location:
        queries.append(f"{spot_name} {location} 门票")

    urls: list[str] = []
    for provider in providers:
        template = SEARCH_PROVIDERS.get(provider.strip().lower())
        if not template:
            logger.warning("Ignoring unknown search provider: %s", provider)
            continue
        for query in queries:
            encoded = urllib.parse.quote(query)
            urls.append(template.format(query=encoded))
            if len(urls) >= max_sources:
                return urls
    return urls


def scrape_price_for_spot(
    spot_name: str,
    location: Optional[str] = None,
    config: Optional[ScraperConfig] = None,
) -> ScrapeResult:
    """Fetch search result pages and extract ticket price evidence."""
    if not spot_name or not spot_name.strip():
        return _empty_result("invalid request", ())

    active_config = config or ScraperConfig()
    urls = build_search_urls(
        spot_name=spot_name.strip(),
        location=location.strip() if location else None,
        providers=active_config.search_providers,
        max_sources=active_config.max_sources,
    )
    evidence: list[PriceEvidence] = []
    fetched_urls: list[str] = []

    with ManagedFetcher(active_config) as fetcher:
        for url in urls:
            try:
                validate_target_url(url, allowed_domains=active_config.allowed_domains)
                page = fetcher.fetch(url)
            except Exception as exc:
                logger.warning("Price source fetch failed. url=%s cause=%s", url, exc)
                continue

            if not page:
                continue

            fetched_urls.append(url)
            text = extract_text(page)
            evidence.extend(extract_price_evidence(text, spot_name=spot_name, source_url=url))

    return build_scrape_result(evidence, fetched_urls, fetcher_label=fetcher.label)


def fetch_prices_for_spot(
    spot_name: str,
    location: Optional[str] = None,
    mode: str = "basic",
    timeout: int = 30,
) -> tuple[list[Decimal], str]:
    """Backward-compatible API used by the earlier FastAPI entrypoint."""
    result = scrape_price_for_spot(
        spot_name=spot_name,
        location=location,
        config=ScraperConfig(mode=mode, timeout=timeout),
    )
    return [item.price for item in result.evidence], result.source


def extract_price_evidence(text_or_html: str, spot_name: str, source_url: str = "") -> list[PriceEvidence]:
    """Extract weighted ticket price evidence from visible text or HTML."""
    text = normalize_text(text_or_html)
    evidence: list[PriceEvidence] = []
    seen: set[tuple[Decimal, str, str]] = set()

    for kind, pattern, base_weight in PRICE_PATTERNS:
        for match in pattern.finditer(text):
            price = parse_price(match.group(1))
            if price is None or not _is_valid_price(price):
                continue

            context = context_window(text, match.start(), match.end())
            if kind in {"symbol", "generic"} and not context_has_price_signal(context, spot_name):
                continue

            weight = base_weight
            if spot_name and spot_name in context:
                weight += 0.2
            if any(keyword in context for keyword in PRICE_KEYWORDS):
                weight += 0.15
            if is_trusted_price_source(source_url):
                weight += 0.1

            normalized_context = compact_context(context)
            key = (price, kind, normalized_context)
            if key in seen:
                continue
            seen.add(key)
            evidence.append(
                PriceEvidence(
                    price=price,
                    kind=kind,
                    source_url=source_url,
                    context=normalized_context,
                    weight=weight,
                )
            )

    return evidence


def extract_prices_from_html(html_content: str, spot_name: str) -> list[Decimal]:
    """Backward-compatible price list extractor."""
    prices: list[Decimal] = []
    seen: set[Decimal] = set()
    for item in extract_price_evidence(html_content, spot_name=spot_name):
        if item.price not in seen:
            seen.add(item.price)
            prices.append(item.price)
    return prices


def build_scrape_result(
    evidence: Iterable[PriceEvidence],
    queried_urls: Iterable[str],
    fetcher_label: str,
) -> ScrapeResult:
    items = tuple(evidence)
    urls = tuple(queried_urls)
    if not items:
        return _empty_result(f"{fetcher_label} - no prices found", urls)

    peak = select_best_evidence(item for item in items if item.kind == "peak")
    off = select_best_evidence(item for item in items if item.kind == "off")
    base_candidates = [item for item in items if item.kind not in {"off", "student"}]
    base = select_best_evidence(base_candidates or items)

    confidence = calculate_evidence_confidence(items, base)
    raw_data = json.dumps(
        {
            "queriedUrls": urls,
            "candidateCount": len(items),
            "evidence": [item.as_dict() for item in sorted(items, key=lambda item: item.weight, reverse=True)[:12]],
        },
        ensure_ascii=False,
    )

    return ScrapeResult(
        base_price=base.price if base else None,
        peak_price=peak.price if peak else None,
        off_price=off.price if off else None,
        source=fetcher_label,
        confidence=confidence,
        raw_data=raw_data,
        evidence=items,
        queried_urls=urls,
    )


def select_best_evidence(evidence: Iterable[PriceEvidence]) -> Optional[PriceEvidence]:
    items = list(evidence)
    if not items:
        return None

    scores: dict[Decimal, float] = defaultdict(float)
    representatives: dict[Decimal, PriceEvidence] = {}
    for item in items:
        bucket = item.price.quantize(Decimal("0.1"))
        scores[bucket] += item.weight
        current = representatives.get(bucket)
        if current is None or item.weight > current.weight:
            representatives[bucket] = item

    best_bucket = max(scores.items(), key=lambda entry: (entry[1], entry[0]))[0]
    return representatives[best_bucket]


def select_best_price(prices: list[Decimal]) -> Optional[Decimal]:
    """Select the most likely price from a list using frequency analysis."""
    if not prices:
        return None

    rounded = [round_to_ten(price) for price in prices]
    freq = Counter(rounded)
    best = freq.most_common(1)[0]
    if best[1] == 1:
        sorted_prices = sorted(prices)
        return sorted_prices[len(sorted_prices) // 2]
    return best[0]


def calculate_confidence(prices: list[Decimal], selected: Decimal) -> float:
    """Backward-compatible confidence score for raw price lists."""
    if not prices or selected is None:
        return 0.0

    selected_rounded = round_to_ten(selected)
    count = sum(
        1
        for price in prices
        if round_to_ten(price) == selected_rounded
    )
    return max(0.35, min(0.85, 0.35 + (count / len(prices)) * 0.5))


def calculate_evidence_confidence(items: tuple[PriceEvidence, ...], selected: Optional[PriceEvidence]) -> float:
    if not items or selected is None:
        return 0.0

    selected_bucket = selected.price.quantize(Decimal("0.1"))
    selected_score = sum(item.weight for item in items if item.price.quantize(Decimal("0.1")) == selected_bucket)
    total_score = sum(item.weight for item in items)
    distinct_sources = len({item.source_url for item in items if item.source_url})
    exact_signal = any(item.context and "门票" in item.context for item in items)

    confidence = 0.3
    confidence += (selected_score / total_score) * 0.45
    confidence += min(distinct_sources, 3) * 0.05
    confidence += 0.05 if exact_signal else 0
    return round(max(0.0, min(0.95, confidence)), 3)


class ManagedFetcher:
    """Scrapling session wrapper with httpx fallback."""

    def __init__(self, config: ScraperConfig):
        self.config = config
        self._manager: Any = None
        self._session: Any = None
        self.label = f"Scrapling ({config.mode})"

    def __enter__(self) -> "ManagedFetcher":
        if self.config.mode == "httpx":
            self.label = "httpx fallback"
            return self

        try:
            self._manager = self._build_session()
            self._session = self._manager.__enter__() if hasattr(self._manager, "__enter__") else self._manager
        except Exception as exc:
            logger.warning("Scrapling session unavailable, falling back to httpx: %s", exc)
            self._manager = None
            self._session = None
            self.label = "httpx fallback"
        return self

    def __exit__(self, exc_type, exc, tb) -> None:
        if self._manager and hasattr(self._manager, "__exit__"):
            self._manager.__exit__(exc_type, exc, tb)

    def _build_session(self) -> Any:
        if self.config.mode == "stealth":
            from scrapling.fetchers import StealthySession

            kwargs: dict[str, Any] = {
                "headless": True,
                "timeout": self.config.timeout * 1000,
                "solve_cloudflare": self.config.solve_cloudflare,
            }
            if self.config.proxy_url:
                kwargs["proxy"] = self.config.proxy_url
            return StealthySession(**kwargs)

        if self.config.mode == "dynamic":
            from scrapling.fetchers import DynamicSession

            kwargs = {
                "headless": True,
                "timeout": self.config.timeout * 1000,
            }
            if self.config.proxy_url:
                kwargs["proxy"] = self.config.proxy_url
            return DynamicSession(**kwargs)

        from scrapling.fetchers import FetcherSession

        kwargs = {
            "impersonate": "chrome",
            "stealthy_headers": True,
            "timeout": self.config.timeout,
            "retries": self.config.retries,
            "headers": DEFAULT_HEADERS,
        }
        if self.config.proxy_url:
            kwargs["proxy"] = self.config.proxy_url
        return FetcherSession(**kwargs)

    def fetch(self, url: str) -> Optional[str]:
        if self._session is None:
            return fetch_with_httpx(url, self.config)

        try:
            if hasattr(self._session, "get"):
                response = self._session.get(url)
            else:
                response = self._session.fetch(url)
            return response_to_text(response)
        except Exception as exc:
            logger.warning("Scrapling fetch failed for %s, trying httpx fallback: %s", url, exc)
            return fetch_with_httpx(url, self.config)


def fetch_with_httpx(url: str, config: ScraperConfig) -> Optional[str]:
    try:
        client_kwargs: dict[str, Any] = {
            "timeout": config.timeout,
            "follow_redirects": True,
            "headers": DEFAULT_HEADERS,
        }
        if config.proxy_url:
            client_kwargs["proxy"] = config.proxy_url
        with httpx.Client(**client_kwargs) as client:
            response = client.get(url)
            response.raise_for_status()
            return response.text
    except Exception as exc:
        logger.warning("httpx fetch failed for %s: %s", url, exc)
        return None


def response_to_text(response: Any) -> str:
    for attribute in ("text", "content", "body"):
        value = getattr(response, attribute, None)
        if callable(value):
            value = value()
        if value is None:
            continue
        if isinstance(value, bytes):
            encoding = getattr(response, "encoding", None) or "utf-8"
            return value.decode(encoding, errors="ignore")
        return str(value)
    return str(response)


def extract_text(html_content: str) -> str:
    """Prefer Scrapling's parser when available, then fall back to tag stripping."""
    try:
        from scrapling.parser import Selector

        page = Selector(html_content)
        text_nodes = page.css("body ::text").getall()
        if text_nodes:
            return " ".join(str(node) for node in text_nodes)
    except Exception:
        pass
    return re.sub(r"<[^>]+>", " ", html_content)


def normalize_text(text_or_html: str) -> str:
    text = re.sub(r"<script\b[^<]*(?:(?!</script>)<[^<]*)*</script>", " ", text_or_html, flags=re.I)
    text = re.sub(r"<style\b[^<]*(?:(?!</style>)<[^<]*)*</style>", " ", text, flags=re.I)
    text = re.sub(r"<[^>]+>", " ", text)
    text = html.unescape(text)
    return re.sub(r"\s+", " ", text).strip()


def parse_price(raw: str) -> Optional[Decimal]:
    try:
        return Decimal(raw).quantize(Decimal("0.1")).normalize()
    except Exception:
        return None


def round_to_ten(price: Decimal) -> Decimal:
    return (price / Decimal("10")).to_integral_value(rounding=ROUND_HALF_UP) * Decimal("10")


def _is_valid_price(price: Decimal) -> bool:
    value = float(price)
    if value < 1 or value > 2000:
        return False
    return value == int(value) or (value * 10) == int(value * 10)


def context_window(text: str, start: int, end: int, size: int = 80) -> str:
    return text[max(0, start - size): min(len(text), end + size)]


def compact_context(context: str, max_length: int = 180) -> str:
    compact = re.sub(r"\s+", " ", context).strip()
    if len(compact) <= max_length:
        return compact
    return compact[: max_length - 1] + "…"


def context_has_price_signal(context: str, spot_name: str) -> bool:
    if spot_name and spot_name in context:
        return True
    return any(keyword in context for keyword in PRICE_KEYWORDS)


def is_trusted_price_source(url: str) -> bool:
    host = urllib.parse.urlparse(url).hostname or ""
    host = host.lower()
    return any(host == hint or host.endswith("." + hint) for hint in TRUSTED_PRICE_HOST_HINTS)


def validate_target_url(url: str, allowed_domains: Iterable[str] = ()) -> None:
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme not in {"http", "https"}:
        raise ValueError("Only HTTP and HTTPS URLs are allowed")
    if parsed.username or parsed.password or not parsed.hostname:
        raise ValueError("URL must not include credentials and must include a hostname")

    host = parsed.hostname.strip().lower()
    ascii_host = host.encode("idna").decode("ascii")
    domains = tuple(domain.strip().lower() for domain in allowed_domains if domain.strip())
    if domains and not any(ascii_host == domain or ascii_host.endswith("." + domain) for domain in domains):
        raise ValueError("URL host is outside the configured allowlist")

    if _is_blocked_host(ascii_host):
        raise ValueError("URL host resolves to a blocked network address")


def _is_blocked_host(host: str) -> bool:
    try:
        return _is_blocked_ip(ipaddress.ip_address(host))
    except ValueError:
        pass

    try:
        infos = socket.getaddrinfo(host, None, proto=socket.IPPROTO_TCP)
    except socket.gaierror as exc:
        raise ValueError("URL host could not be resolved safely") from exc

    for info in infos:
        address = ipaddress.ip_address(info[4][0])
        if _is_blocked_ip(address):
            return True
    return False


def _is_blocked_ip(address: ipaddress._BaseAddress) -> bool:
    return any(
        (
            address.is_loopback,
            address.is_private,
            address.is_link_local,
            address.is_multicast,
            address.is_reserved,
            address.is_unspecified,
        )
    )


def _empty_result(source: str, queried_urls: Iterable[str]) -> ScrapeResult:
    return ScrapeResult(
        base_price=None,
        peak_price=None,
        off_price=None,
        source=source,
        confidence=0.0,
        raw_data=json.dumps({"queriedUrls": tuple(queried_urls), "evidence": []}, ensure_ascii=False),
        evidence=(),
        queried_urls=tuple(queried_urls),
    )
