package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class PublicTravelTextTest {
    @Test void removesDirectIdentifiersFromPublicProse() {
        assertThat(PublicTravelText.minimize("联系13812345678，邮箱trip@example.com，证件110101199001011234"))
                .doesNotContain("13812345678", "trip@example.com", "110101199001011234");
    }
    @Test void removesExplicitPrivatePreparationFields() {
        assertThat(PublicTravelText.minimize("计划拉萨\n- 病史：个人记录\n护照号码: AB12345\n景点：大昭寺"))
                .doesNotContain("个人记录", "AB12345").contains("计划拉萨", "景点：大昭寺");
    }
    @Test void preservesItineraryTimesPricesAltitudeAndOfficialLinks() {
        String text = "2026-11-01 09:30 海拔3650米，¥1200。https://www.lasa.gov.cn/guide/20261031/";
        assertThat(PublicTravelText.minimize(text)).isEqualTo(text);
    }
    @Test void isNullSafeAndIdempotent() {
        assertThat(PublicTravelText.minimize(null)).isNull();
        String once = PublicTravelText.minimize("13812345678");
        assertThat(PublicTravelText.minimize(once)).isEqualTo(once);
    }
}
