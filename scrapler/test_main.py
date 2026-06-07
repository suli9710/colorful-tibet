import os
import unittest
from decimal import Decimal
from unittest.mock import patch

from fastapi.testclient import TestClient

import main
from scraper import PriceEvidence, ScrapeResult, ScraperConfig


class ScraplingApiTest(unittest.TestCase):
    def setUp(self):
        self.env_patcher = patch.dict(
            os.environ,
            {
                "SCRAPLING_API_KEY": "",
                "SCRAPLING_ALLOW_REQUEST_MODE_OVERRIDE": "",
            },
        )
        self.env_patcher.start()
        self.client = TestClient(main.app)

    def tearDown(self):
        self.env_patcher.stop()

    def sample_result(self):
        return ScrapeResult(
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
                    context="ticket price 200 CNY",
                    weight=1.3,
                ),
            ),
            queried_urls=("https://example.com/spot",),
        )

    def test_health_exposes_runtime_metadata(self):
        response = self.client.get("/health")

        self.assertEqual(200, response.status_code)
        payload = response.json()
        self.assertEqual("ok", payload["status"])
        self.assertIn("mode", payload)

    def test_scrape_price_keeps_backend_compatible_fields(self):
        result = self.sample_result()

        with patch("main.scrape_price_for_spot", return_value=result):
            response = self.client.post(
                "/scrape/price",
                json={"spotName": "Potala Palace", "location": "Lhasa", "maxSources": 2},
            )

        self.assertEqual(200, response.status_code)
        payload = response.json()
        self.assertEqual(200.0, payload["basePrice"])
        self.assertEqual(200.0, payload["peakSeasonPrice"])
        self.assertEqual(100.0, payload["offSeasonPrice"])
        self.assertEqual("Scrapling (basic)", payload["source"])
        self.assertEqual('{"evidence":[]}', payload["rawData"])
        self.assertEqual(1, len(payload["evidence"]))

    def test_scrape_price_requires_token_when_configured(self):
        with patch.dict(os.environ, {"SCRAPLING_API_KEY": "secret-token"}):
            response = self.client.post(
                "/scrape/price",
                json={"spotName": "Potala Palace"},
            )

        self.assertEqual(401, response.status_code)

    def test_scrape_price_accepts_configured_token(self):
        result = self.sample_result()

        with (
            patch.dict(os.environ, {"SCRAPLING_API_KEY": "secret-token"}),
            patch("main.scrape_price_for_spot", return_value=result),
        ):
            response = self.client.post(
                "/scrape/price",
                headers={"X-Scrapling-Api-Key": "secret-token"},
                json={"spotName": "Potala Palace"},
            )

        self.assertEqual(200, response.status_code)
        self.assertEqual(200.0, response.json()["basePrice"])

    def test_scrape_price_ignores_request_mode_override_by_default(self):
        result = self.sample_result()

        with (
            patch("main.BASE_CONFIG", ScraperConfig(mode="basic")),
            patch("main.scrape_price_for_spot", return_value=result) as scrape_mock,
        ):
            response = self.client.post(
                "/scrape/price",
                json={"spotName": "Potala Palace", "mode": "httpx"},
            )

        self.assertEqual(200, response.status_code)
        self.assertEqual("basic", scrape_mock.call_args.kwargs["config"].mode)

    def test_scrape_price_allows_request_mode_override_when_enabled(self):
        result = self.sample_result()

        with (
            patch.dict(os.environ, {"SCRAPLING_ALLOW_REQUEST_MODE_OVERRIDE": "true"}),
            patch("main.BASE_CONFIG", ScraperConfig(mode="basic")),
            patch("main.scrape_price_for_spot", return_value=result) as scrape_mock,
        ):
            response = self.client.post(
                "/scrape/price",
                json={"spotName": "Potala Palace", "mode": "httpx"},
            )

        self.assertEqual(200, response.status_code)
        self.assertEqual("httpx", scrape_mock.call_args.kwargs["config"].mode)


if __name__ == "__main__":
    unittest.main()
