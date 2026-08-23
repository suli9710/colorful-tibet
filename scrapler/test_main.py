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
                "SCRAPLING_ALLOW_UNAUTHENTICATED": "true",
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

    def test_scrape_price_rejects_unconfigured_token_by_default(self):
        with patch.dict(
            os.environ,
            {
                "SCRAPLING_API_KEY": "",
                "SCRAPLING_ALLOW_UNAUTHENTICATED": "",
            },
        ):
            response = self.client.post(
                "/scrape/price",
                json={"spotName": "Potala Palace"},
            )

        self.assertEqual(503, response.status_code)

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

    def test_alertmanager_notification_logs_a_sanitized_summary(self):
        with patch.dict(os.environ, {"ALERT_LOG_TOKEN": "a" * 32}):
            with self.assertLogs("scrapling-service", level="WARNING") as captured:
                response = self.client.post(
                    "/internal/alertmanager",
                    headers={"X-Alert-Log-Token": "a" * 32},
                    json={
                        "status": "firing",
                        "alerts": [
                            {
                                "labels": {
                                    "alertname": "BackendDown",
                                    "severity": "critical",
                                    "secret": "must-not-be-logged",
                                }
                            }
                        ],
                    },
                )

        self.assertEqual(200, response.status_code)
        self.assertEqual({"status": "logged"}, response.json())
        summary = "\n".join(captured.output)
        self.assertIn("alertNames=BackendDown", summary)
        self.assertIn("severities=critical", summary)
        self.assertNotIn("must-not-be-logged", summary)

    def test_alertmanager_query_token_still_works_but_is_flagged(self):
        # The token is accepted for backward compatibility, but a query string reaches every access
        # log verbatim, so the operator has to be told to move it to the header.
        with patch.dict(os.environ, {"ALERT_LOG_TOKEN": "a" * 32}):
            with self.assertLogs("scrapling-service", level="WARNING") as captured:
                response = self.client.post(
                    "/internal/alertmanager?token=" + "a" * 32,
                    json={"status": "firing", "alerts": []},
                )

        self.assertEqual(200, response.status_code)
        self.assertIn("not written to access logs", "\n".join(captured.output))

    def test_alertmanager_accepts_a_bearer_credential(self):
        # Alertmanager 0.27 cannot set an arbitrary custom header, but it can send a Bearer
        # credential, so this is the form the documented webhook config actually uses.
        with patch.dict(os.environ, {"ALERT_LOG_TOKEN": "a" * 32}):
            with self.assertLogs("scrapling-service", level="WARNING") as captured:
                response = self.client.post(
                    "/internal/alertmanager",
                    headers={"Authorization": "Bearer " + "a" * 32},
                    json={"status": "firing", "alerts": []},
                )

        self.assertEqual(200, response.status_code)
        self.assertNotIn("not written to access logs", "\n".join(captured.output))

    def test_alertmanager_rejects_a_wrong_bearer_credential(self):
        with patch.dict(os.environ, {"ALERT_LOG_TOKEN": "a" * 32}):
            response = self.client.post(
                "/internal/alertmanager",
                headers={"Authorization": "Bearer " + "b" * 32},
                json={"status": "firing", "alerts": []},
            )

        self.assertEqual(401, response.status_code)

    def test_alertmanager_notification_requires_its_own_token(self):
        with patch.dict(os.environ, {"ALERT_LOG_TOKEN": "a" * 32}):
            response = self.client.post(
                "/internal/alertmanager",
                headers={"X-Alert-Log-Token": "b" * 32},
                json={"status": "firing", "alerts": []},
            )

        self.assertEqual(401, response.status_code)

    def test_non_ascii_credentials_are_rejected_not_crashed(self):
        # HTTP header values are latin-1, so a caller can legitimately send bytes that Starlette
        # decodes to a non-ASCII str. secrets.compare_digest raises TypeError for those, which turned
        # both credential checks into unhandled 500s for an unauthenticated caller.
        non_ascii_credential = "é".encode("latin-1") * 16

        with patch.dict(os.environ, {"SCRAPLING_API_KEY": "a" * 32, "ALERT_LOG_TOKEN": "a" * 32}):
            api_response = self.client.post(
                "/scrape/price",
                headers={"X-Scrapling-Api-Key": non_ascii_credential},
                json={"spotName": "布达拉宫"},
            )
            alert_response = self.client.post(
                "/internal/alertmanager",
                headers={"X-Alert-Log-Token": non_ascii_credential},
                json={"status": "firing", "alerts": []},
            )

        self.assertEqual(401, api_response.status_code)
        self.assertEqual(401, alert_response.status_code)


if __name__ == "__main__":
    unittest.main()
