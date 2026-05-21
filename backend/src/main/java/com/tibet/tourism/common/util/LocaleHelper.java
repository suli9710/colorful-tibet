package com.tibet.tourism.common.util;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import com.tibet.tourism.modules.content.domain.News;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import java.util.List;

public final class LocaleHelper {

    public static final String LOCALE_BO = "bo";

    private LocaleHelper() {}

    public static String resolveByLocale(String locale, String defaultText, String tibetanText) {
        if (LOCALE_BO.equals(locale) && tibetanText != null && !tibetanText.isEmpty()) {
            return tibetanText;
        }
        return defaultText;
    }

    public static ScenicSpot resolveScenicSpotLocale(ScenicSpot spot, String locale) {
        if (spot == null || !LOCALE_BO.equals(locale)) return spot;
        if (spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
            spot.setName(spot.getNameTibetan());
        }
        if (spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
            spot.setDescription(spot.getDescriptionTibetan());
        }
        return spot;
    }

    public static List<ScenicSpot> resolveScenicSpotLocale(List<ScenicSpot> spots, String locale) {
        if (!LOCALE_BO.equals(locale) || spots == null) return spots;
        spots.forEach(s -> resolveScenicSpotLocale(s, locale));
        return spots;
    }

    public static HeritageItem resolveHeritageItemLocale(HeritageItem item, String locale) {
        if (item == null || !LOCALE_BO.equals(locale)) return item;
        if (item.getNameTibetan() != null && !item.getNameTibetan().isEmpty()) {
            item.setName(item.getNameTibetan());
        }
        if (item.getDescriptionTibetan() != null && !item.getDescriptionTibetan().isEmpty()) {
            item.setDescription(item.getDescriptionTibetan());
        }
        return item;
    }

    public static News resolveNewsLocale(News news, String locale) {
        if (news == null || !LOCALE_BO.equals(locale)) return news;
        if (news.getTitleTibetan() != null && !news.getTitleTibetan().isEmpty()) {
            news.setTitle(news.getTitleTibetan());
        }
        if (news.getContentTibetan() != null && !news.getContentTibetan().isEmpty()) {
            news.setContent(news.getContentTibetan());
        }
        return news;
    }
}
