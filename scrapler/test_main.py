import unittest
from decimal import Decimal
from unittest.mock import patch

from fastapi.testclient import TestClient

from main import app
from scraper import PriceEvidence, ScrapeResult


class ScraplingApiTest(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_health_exposes_runtime_metadata(self):
        response = self.client.get("/health")

        self.assertEqual(200, response.status_code)
        payload = response.json()
        self.assertEqual("ok", payload["status"])
        self.assertIn("mode", payload)

    def test_scrape_price_keeps_backend_compatible_fields(self):
        result = ScrapeResult(
            base_price=Decimal("200"),
            peak_price=Decimal("200"),
            off_price=Decimal("100"),
            source="Scrapling (basic)",
            confidence=0.82,
            raw_data='{"evidence":[]}',
            evidence=(
                PriceEvidence(
                    price=Decimal("200"),
                    kind="base",
                    source_url="https://example.com/spot",
                    context="布达拉宫门票价格200元",
                    weight=1.3,
                ),
            ),
            queried_urls=("https://example.com/spot",),
        )

        with patch("main.scrape_price_for_spot", return_value=result):
            response = self.client.post(
                "/scrape/price",
                json={"spotName": "布达拉宫", "location": "拉萨", "maxSources": 2},
            )

        self.assertEqual(200, response.status_code)
        payload = response.json()
        self.assertEqual(200.0, payload["basePrice"])
        self.assertEqual(200.0, payload["peakSeasonPrice"])
        self.assertEqual(100.0, payload["offSeasonPrice"])
        self.assertEqual("Scrapling (basic)", payload["source"])
        self.assertEqual('{"evidence":[]}', payload["rawData"])
        self.assertEqual(1, len(payload["evidence"]))


if __name__ == "__main__":
    unittest.main()
