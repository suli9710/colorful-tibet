import unittest
from decimal import Decimal

from scraper import (
    ScraperConfig,
    build_scrape_result,
    build_search_urls,
    extract_price_evidence,
    select_best_price,
    validate_target_url,
)


class ScraperExtractionTest(unittest.TestCase):
    def test_extracts_seasonal_and_base_ticket_prices(self):
        text = """
        布达拉宫门票价格200元，旺季200元，淡季100元。
        成人票200元/人，学生票100元/人。
        """

        evidence = extract_price_evidence(text, "布达拉宫", "https://example.com/spot")
        prices_by_kind = {(item.kind, item.price) for item in evidence}

        self.assertIn(("base", Decimal("200")), prices_by_kind)
        self.assertIn(("peak", Decimal("200")), prices_by_kind)
        self.assertIn(("off", Decimal("100")), prices_by_kind)

    def test_build_scrape_result_keeps_seasonal_prices(self):
        evidence = extract_price_evidence(
            "纳木错景区门票价格120元，旺季120元，淡季60元。",
            "纳木错",
            "https://example.com/namtso",
        )

        result = build_scrape_result(evidence, ["https://example.com/namtso"], "Scrapling (basic)")

        self.assertEqual(Decimal("120"), result.base_price)
        self.assertEqual(Decimal("120"), result.peak_price)
        self.assertEqual(Decimal("60"), result.off_price)
        self.assertGreater(result.confidence, 0)

    def test_select_best_price_uses_frequency_before_median(self):
        selected = select_best_price([Decimal("80"), Decimal("81"), Decimal("200")])

        self.assertEqual(Decimal("80"), selected)


class ScraperUrlSafetyTest(unittest.TestCase):
    def test_build_search_urls_respects_provider_and_source_limits(self):
        urls = build_search_urls("布达拉宫", "拉萨", providers=("baidu", "bing"), max_sources=3)

        self.assertEqual(3, len(urls))
        self.assertTrue(all(url.startswith("https://www.baidu.com/") for url in urls))

    def test_validate_target_url_blocks_localhost(self):
        with self.assertRaises(ValueError):
            validate_target_url("http://127.0.0.1:8080/admin")

    def test_validate_target_url_applies_domain_allowlist(self):
        validate_target_url("https://www.baidu.com/s?wd=test", allowed_domains=("baidu.com",))
        with self.assertRaises(ValueError):
            validate_target_url("https://www.bing.com/search?q=test", allowed_domains=("baidu.com",))

    def test_config_defaults_are_bounded(self):
        config = ScraperConfig(max_sources=4, search_providers=("baidu", "bing"))

        self.assertEqual(4, config.max_sources)
        self.assertEqual(("baidu", "bing"), config.search_providers)


if __name__ == "__main__":
    unittest.main()
