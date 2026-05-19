"""FastAPI microservice for scenic spot price scraping using Scrapling."""

import logging
import os
from typing import Optional

from dotenv import load_dotenv
from fastapi import FastAPI
from pydantic import BaseModel

from scraper import (
    calculate_confidence,
    fetch_prices_for_spot,
    select_best_price,
)

load_dotenv()

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
logging.basicConfig(level=LOG_LEVEL, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger("scrapling-service")

SCRAPLING_MODE = os.getenv("SCRAPLING_MODE", "basic")
SCRAPLING_TIMEOUT = int(os.getenv("SCRAPLING_TIMEOUT_SECONDS", "30"))

app = FastAPI(title="Scrapling Price Service", version="0.2.0")


class ScrapeRequest(BaseModel):
    spotName: str
    location: Optional[str] = None


class PriceResponse(BaseModel):
    basePrice: Optional[float] = None
    peakSeasonPrice: Optional[float] = None
    offSeasonPrice: Optional[float] = None
    source: str = "Scrapling"
    confidence: float = 0.7
    rawData: Optional[str] = None


@app.post("/scrape/price", response_model=PriceResponse)
async def scrape_price(request: ScrapeRequest) -> PriceResponse:
    logger.info(
        "Scraping price for spot=%s location=%s mode=%s",
        request.spotName,
        request.location,
        SCRAPLING_MODE,
    )

    all_prices, source_label = fetch_prices_for_spot(
        spot_name=request.spotName,
        location=request.location,
        mode=SCRAPLING_MODE,
        timeout=SCRAPLING_TIMEOUT,
    )

    if not all_prices:
        return PriceResponse(
            source=f"{source_label} — no prices found",
            confidence=0.0,
        )

    best = select_best_price(all_prices)
    if best is None:
        return PriceResponse(
            source=f"{source_label} — no valid price",
            confidence=0.0,
        )

    confidence = calculate_confidence(all_prices, best)

    return PriceResponse(
        basePrice=float(best),
        source=source_label,
        confidence=confidence,
        rawData=f"Raw prices found: {[float(p) for p in all_prices]}",
    )


@app.get("/health")
async def health():
    return {"status": "ok", "mode": SCRAPLING_MODE}
