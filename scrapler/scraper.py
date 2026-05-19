"""Price scraping business logic using Scrapling, with httpx fallback."""

import logging
import re
import urllib.parse
from collections import Counter
from decimal import ROUND_HALF_UP, Decimal
from typing import Optional

import httpx

logger = logging.getLogger(__name__)

# Price regex patterns, ported from WebScrapingStrategy
_PRICE_PATTERNS = [
    re.compile(r"门票[：:]*\s*(?:价格|票价|费用)[：:]*\s*(\d+(?:\.\d+)?)\s*(?:元|块|¥)"),
    re.compile(r"(?:门票|票价|价格)[：:]*\s*(\d+(?:\.\d+)?)\s*(?:元|块|¥)"),
    re.compile(r"(?:旺季|淡季)[：:]*\s*(\d+(?:\.\d+)?)\s*(?:元|块|¥)"),
    re.compile(r"(?:成人票|学生票|儿童票)[：:]*\s*(\d+(?:\.\d+)?)\s*(?:元|块|¥)"),
    re.compile(r"(\d+(?:\.\d+)?)\s*(?:元|块|¥)(?:/人|/张)?"),
]


def _try_create_scrapling_session(mode: str, timeout: int):
    """Try to create a Scrapling session; returns None if unavailable."""
    try:
        if mode == "stealth":
            from scrapling.fetchers import StealthySession

            return StealthySession(headless=True, solve_cloudflare=True)
        else:
            from scrapling.fetchers import FetcherSession

            return FetcherSession(impersonate="chrome")
    except Exception as e:
        logger.warning("Scrapling session unavailable, falling back to httpx: %s", e)
        return None


def _fetch_with_scrapling(session, url: str, timeout: int) -> Optional[str]:
    """Fetch a URL using a Scrapling session."""
    try:
        if hasattr(session, "fetch"):
            page = session.fetch(url, google_search=False)
        else:
            page = session.get(url, stealthy_headers=True)
        return page.content
    except Exception as e:
        logger.warning("Scrapling fetch failed for %s: %s", url, e)
        return None


def _fetch_with_httpx(url: str, timeout: int) -> Optional[str]:
    """Fetch a URL using httpx as fallback."""
    try:
        headers = {
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/120.0.0.0 Safari/537.36"
            ),
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        }
        with httpx.Client(timeout=timeout, follow_redirects=True) as client:
            resp = client.get(url, headers=headers)
            resp.raise_for_status()
            return resp.text
    except Exception as e:
        logger.warning("httpx fetch failed for %s: %s", url, e)
        return None


def build_search_urls(spot_name: str, location: Optional[str] = None) -> list[str]:
    """Build Baidu search URLs for the spot."""
    urls = [
        f"https://www.baidu.com/s?wd={urllib.parse.quote(spot_name + ' 门票价格')}",
        f"https://www.baidu.com/s?wd={urllib.parse.quote(spot_name + ' 门票多少钱')}",
    ]
    if location:
        urls.append(
            f"https://www.baidu.com/s?wd={urllib.parse.quote(spot_name + ' ' + location + ' 门票')}"
        )
    return urls


def fetch_prices_for_spot(
    spot_name: str,
    location: Optional[str] = None,
    mode: str = "basic",
    timeout: int = 30,
) -> tuple[list[Decimal], str]:
    """Fetch all price candidates for a scenic spot.

    Returns (prices, source_label).
    """
    session = _try_create_scrapling_session(mode, timeout)
    source_label = f"Scrapling ({mode})" if session else "httpx (fallback)"

    urls = build_search_urls(spot_name, location)
    all_prices: list[Decimal] = []

    for url in urls:
        if session:
            html = _fetch_with_scrapling(session, url, timeout)
        else:
            html = _fetch_with_httpx(url, timeout)

        if html:
            prices = extract_prices_from_html(html, spot_name)
            all_prices.extend(prices)

    return all_prices, source_label


def extract_prices_from_html(html: str, spot_name: str) -> list[Decimal]:
    """Extract ticket prices from HTML using regex patterns."""
    # Strip HTML tags for text extraction
    text = re.sub(r"<[^>]+>", " ", html)
    text = text.replace("&nbsp;", " ").replace("&amp;", " ")

    prices: list[Decimal] = []

    # Build a spot-name–specific pattern
    spot_pattern = re.compile(
        re.escape(spot_name) + r".*?(\d+(?:\.\d+)?)\s*(?:元|块|¥)"
    )

    all_patterns = list(_PRICE_PATTERNS) + [spot_pattern]

    seen = set()
    for pattern in all_patterns:
        for match in pattern.finditer(text):
            try:
                price_str = match.group(1)
                price = Decimal(price_str)
                if _is_valid_price(price) and price not in seen:
                    seen.add(price)
                    prices.append(price)
            except Exception:
                continue

    return prices


def _is_valid_price(price: Decimal) -> bool:
    """Check if a price looks like a real ticket price (10-2000 CNY)."""
    value = float(price)
    if value < 10 or value > 2000:
        return False
    return value == int(value) or (value * 10) == int(value * 10)


def select_best_price(prices: list[Decimal]) -> Optional[Decimal]:
    """Select the most likely price from a list using frequency analysis."""
    if not prices:
        return None

    rounded = [
        p.divide(Decimal("10"), rounding=ROUND_HALF_UP).to_integral_value() * 10
        for p in prices
    ]

    freq = Counter(rounded)
    best = freq.most_common(1)[0]

    if best[1] == 1:
        sorted_prices = sorted(prices)
        return sorted_prices[len(sorted_prices) // 2]

    return best[0]


def calculate_confidence(prices: list[Decimal], selected: Decimal) -> float:
    """Calculate confidence score for the selected price."""
    if not prices or selected is None:
        return 0.5

    selected_rounded = (
        selected.divide(Decimal("10"), rounding=ROUND_HALF_UP).to_integral_value() * 10
    )

    count = sum(
        1
        for p in prices
        if p.divide(Decimal("10"), rounding=ROUND_HALF_UP).to_integral_value() * 10
        == selected_rounded
    )

    confidence = (count / len(prices)) * 0.8
    return max(0.5, min(0.8, confidence))
