package com.tibet.tourism.modules.route.application;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.route.domain.Itinerary;
import com.tibet.tourism.modules.route.domain.ItineraryDay;
import com.tibet.tourism.modules.route.domain.ItineraryItem;
import com.tibet.tourism.modules.route.domain.TibetTravelKit;
import com.tibet.tourism.modules.route.domain.TibetTravelKitAlert;
import com.tibet.tourism.modules.route.domain.TibetTravelKitCultureTip;
import com.tibet.tourism.modules.route.domain.TibetTravelKitDayAdvice;
import com.tibet.tourism.modules.route.domain.TibetTravelKitEmergencyContact;
import com.tibet.tourism.modules.route.domain.TibetTravelKitMapPin;
import com.tibet.tourism.modules.route.domain.TibetTravelKitPhrase;
import com.tibet.tourism.modules.route.domain.TibetTravelKitSustainableOption;
import com.tibet.tourism.modules.route.infra.ItineraryRepository;
import com.tibet.tourism.modules.route.infra.TibetTravelKitRepository;
import com.tibet.tourism.modules.route.web.dto.specialty.CulturalTipResponse;
import com.tibet.tourism.modules.route.web.dto.specialty.EmergencyContactResponse;
import com.tibet.tourism.modules.route.web.dto.specialty.HighlandAssessmentRequest;
import com.tibet.tourism.modules.route.web.dto.specialty.HighlandAssessmentResponse;
import com.tibet.tourism.modules.route.web.dto.specialty.HighlandDayAdviceResponse;
import com.tibet.tourism.modules.route.web.dto.specialty.OfflineMapPinResponse;
import com.tibet.tourism.modules.route.web.dto.specialty.OfflineTravelPackageResponse;
import com.tibet.tourism.modules.route.web.dto.specialty.PhraseGuideItemResponse;
import com.tibet.tourism.modules.route.web.dto.specialty.SustainableOptionResponse;
import com.tibet.tourism.modules.route.web.dto.specialty.TibetTravelKitResponse;
import com.tibet.tourism.modules.route.web.dto.specialty.TravelAlertResponse;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.user.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TibetTravelKitService {

    private static final Pattern ALTITUDE_NUMBER = Pattern.compile("(\\d{3,5})");
    private static final String LIST_SEPARATOR = "\n";

    private final ItineraryRepository itineraryRepository;
    private final TibetTravelKitRepository travelKitRepository;

    public TibetTravelKitService(ItineraryRepository itineraryRepository,
                                 TibetTravelKitRepository travelKitRepository) {
        this.itineraryRepository = itineraryRepository;
        this.travelKitRepository = travelKitRepository;
    }

    @Transactional
    public TibetTravelKitResponse buildTravelKit(User user, Long itineraryId) {
        Itinerary itinerary = loadUserItinerary(user, itineraryId);
        Optional<TibetTravelKit> cachedKit = travelKitRepository
                .findFirstByItineraryIdAndUserIdAndValidUntilAfterOrderByGeneratedAtDesc(
                        itinerary.getId(), user.getId(), LocalDateTime.now());
        if (cachedKit.isPresent()) {
            return toResponse(cachedKit.get());
        }

        HighlandAssessmentResponse highlandAssessment = assessItineraryRisk(itinerary,
                new HighlandAssessmentRequest(itineraryId, null, 2, "MEDIUM", false, false, false,
                        itinerary.getDays(), null, "BALANCED"));

        TibetTravelKitResponse generated = new TibetTravelKitResponse(
                itinerary.getId(),
                itinerary.getTitle(),
                highlandAssessment,
                getCultureTipsForItinerary(itinerary),
                getPhrasebook("essential"),
                buildOfflinePackage(itinerary),
                buildTravelAlerts(itinerary, highlandAssessment),
                getSustainableOptions(primaryRegion(itinerary))
        );
        TibetTravelKit saved = travelKitRepository.save(toEntity(user, itinerary, generated));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public HighlandAssessmentResponse assessHighlandRisk(User user, HighlandAssessmentRequest request) {
        HighlandAssessmentRequest safeRequest = request == null
                ? new HighlandAssessmentRequest(null, null, 2, "MEDIUM", false, false, false, 1, null, "BALANCED")
                : request;

        if (safeRequest.itineraryId() != null) {
            return assessItineraryRisk(loadUserItinerary(user, safeRequest.itineraryId()), safeRequest);
        }

        return assessManualRisk(safeRequest);
    }

    public List<CulturalTipResponse> getCultureTips(String scene) {
        String normalizedScene = normalizeFilter(scene);
        List<CulturalTipResponse> tips = baseCultureTips();
        if (!StringUtils.hasText(normalizedScene) || "ALL".equals(normalizedScene)) {
            return tips;
        }
        return tips.stream()
                .filter(tip -> tip.scene().equals(normalizedScene))
                .toList();
    }

    public List<PhraseGuideItemResponse> getPhrasebook(String category) {
        String normalizedCategory = normalizeFilter(category);
        List<PhraseGuideItemResponse> phrases = basePhrasebook();
        if (!StringUtils.hasText(normalizedCategory) || "ALL".equals(normalizedCategory)
                || "ESSENTIAL".equals(normalizedCategory)) {
            return phrases.stream().limit(8).toList();
        }
        return phrases.stream()
                .filter(phrase -> phrase.category().equals(normalizedCategory))
                .toList();
    }

    public List<SustainableOptionResponse> getSustainableOptions(String region) {
        String safeRegion = InputSanitizer.optionalPlainText(region, 80, "地区");
        List<SustainableOptionResponse> options = new ArrayList<>(List.of(
                new SustainableOptionResponse(
                        "LOCAL_GUIDE",
                        "优先选择本地向导和非遗体验",
                        "把旅游收益留在社区，也让讲解更接近真实生活语境。",
                        List.of("选择有资质的本地向导", "优先预订小规模文化体验", "购买明码标价的本地手作"),
                        "支持社区就业和非遗传承",
                        "减少重复绕行，降低无效交通里程"
                ),
                new SustainableOptionResponse(
                        "LOW_CARBON",
                        "顺路合并景点，减少折返",
                        "西藏目的地距离长，路线顺路比单点打卡更重要。",
                        List.of("同一区域景点排在同一天", "城市内优先步行或拼车", "长距离移动预留休整日"),
                        "减少道路压力和旅行疲劳",
                        "同城合并游览通常可减少 15%-25% 交通里程"
                ),
                new SustainableOptionResponse(
                        "ECOLOGY",
                        "高原生态零遗留",
                        "高寒环境恢复慢，轻微破坏也可能长期存在。",
                        List.of("带走所有个人垃圾", "不进入未开放草甸和湿地", "不投喂野生动物"),
                        "保护当地牧场、水源和野生动物栖息地",
                        "减少一次性用品和临时补给浪费"
                ),
                new SustainableOptionResponse(
                        "CULTURE",
                        "尊重宗教和民俗空间",
                        "把寺庙、转经道和村落当作正在生活的公共空间，而不是布景。",
                        List.of("拍摄前先征得同意", "绕行方向遵守现场提示", "进入宗教空间降低音量"),
                        "减少对居民和信众的打扰",
                        "慢游停留比密集打卡更友好"
                )
        ));

        if (StringUtils.hasText(safeRegion) && (safeRegion.contains("珠峰") || safeRegion.contains("阿里"))) {
            options.add(new SustainableOptionResponse(
                    "HIGH_ALTITUDE",
                    safeRegion + "脆弱高海拔路线",
                    "高海拔区域补给和救援成本更高，建议减少临时改线和无计划穿越。",
                    List.of("提前确认边防/景区开放状态", "选择正规车辆和向导", "不离开开放道路进入无人区"),
                    "降低当地救援和道路管理压力",
                    "稳定路线比临时包车绕行更低碳"
            ));
        }
        return options;
    }

    private TibetTravelKit toEntity(User user, Itinerary itinerary, TibetTravelKitResponse response) {
        TibetTravelKit kit = new TibetTravelKit();
        kit.setUser(user);
        kit.setItinerary(itinerary);
        kit.setTitle(response.title());
        kit.setGeneratedAt(response.offlinePackage().generatedAt());
        kit.setValidUntil(response.offlinePackage().validUntil());
        kit.setRiskScore(response.highlandAssessment().riskScore());
        kit.setRiskLevel(response.highlandAssessment().riskLevel());
        kit.setRiskLabel(response.highlandAssessment().riskLabel());
        kit.setMaxAltitudeMeters(response.highlandAssessment().maxAltitudeMeters());
        kit.setHighAltitudeDays(response.highlandAssessment().highAltitudeDays());
        kit.setHighlandSummary(response.highlandAssessment().summary());
        kit.setIncludedSections(joinList(response.offlinePackage().includedSections()));
        kit.setAdaptationChecklist(joinList(response.highlandAssessment().adaptationChecklist()));
        kit.setWarningSigns(joinList(response.highlandAssessment().warningSigns()));
        kit.setGoSlowRules(joinList(response.highlandAssessment().goSlowRules()));
        kit.setOfflineChecklist(joinList(response.offlinePackage().offlineChecklist()));
        kit.setVoucherHints(joinList(response.offlinePackage().voucherHints()));
        kit.setBackupNotes(joinList(response.offlinePackage().backupNotes()));

        int sort = 0;
        for (HighlandDayAdviceResponse advice : response.highlandAssessment().dailyAdvice()) {
            TibetTravelKitDayAdvice entity = new TibetTravelKitDayAdvice();
            entity.setDayNumber(advice.dayNumber());
            entity.setTitle(advice.title());
            entity.setMaxAltitudeMeters(advice.maxAltitudeMeters());
            entity.setRiskLevel(advice.riskLevel());
            entity.setPaceAdvice(advice.paceAdvice());
            entity.setHydrationAdvice(advice.hydrationAdvice());
            entity.setActivityLimit(advice.activityLimit());
            entity.setWarning(advice.warning());
            kit.addDayAdvice(entity);
        }

        sort = 0;
        for (CulturalTipResponse tip : response.culturalTips()) {
            TibetTravelKitCultureTip entity = new TibetTravelKitCultureTip();
            entity.setScene(tip.scene());
            entity.setTitle(tip.title());
            entity.setContext(tip.context());
            entity.setDoTips(joinList(tip.doTips()));
            entity.setAvoidTips(joinList(tip.avoidTips()));
            entity.setSortOrder(sort++);
            kit.addCultureTip(entity);
        }

        sort = 0;
        for (PhraseGuideItemResponse phrase : response.phrasebook()) {
            TibetTravelKitPhrase entity = new TibetTravelKitPhrase();
            entity.setCategory(phrase.category());
            entity.setChinese(phrase.chinese());
            entity.setTibetan(phrase.tibetan());
            entity.setEnglish(phrase.english());
            entity.setPronunciation(phrase.pronunciation());
            entity.setUsageText(phrase.usage());
            entity.setSortOrder(sort++);
            kit.addPhrase(entity);
        }

        sort = 0;
        for (EmergencyContactResponse contact : response.offlinePackage().emergencyContacts()) {
            TibetTravelKitEmergencyContact entity = new TibetTravelKitEmergencyContact();
            entity.setName(contact.name());
            entity.setPhone(contact.phone());
            entity.setDescription(contact.description());
            entity.setSortOrder(sort++);
            kit.addEmergencyContact(entity);
        }

        sort = 0;
        for (OfflineMapPinResponse pin : response.offlinePackage().mapPins()) {
            TibetTravelKitMapPin entity = new TibetTravelKitMapPin();
            entity.setType(pin.type());
            entity.setName(pin.name());
            entity.setLatitude(pin.latitude());
            entity.setLongitude(pin.longitude());
            entity.setAltitudeMeters(pin.altitudeMeters());
            entity.setNote(pin.note());
            entity.setSortOrder(sort++);
            kit.addMapPin(entity);
        }

        sort = 0;
        for (TravelAlertResponse alert : response.realtimeAlerts()) {
            TibetTravelKitAlert entity = new TibetTravelKitAlert();
            entity.setLevel(alert.level());
            entity.setType(alert.type());
            entity.setTitle(alert.title());
            entity.setMessage(alert.message());
            entity.setAction(alert.action());
            entity.setRelatedDay(alert.relatedDay());
            entity.setExpiresAt(alert.expiresAt());
            entity.setSortOrder(sort++);
            kit.addAlert(entity);
        }

        sort = 0;
        for (SustainableOptionResponse option : response.sustainableOptions()) {
            TibetTravelKitSustainableOption entity = new TibetTravelKitSustainableOption();
            entity.setCategory(option.category());
            entity.setTitle(option.title());
            entity.setImpact(option.impact());
            entity.setActions(joinList(option.actions()));
            entity.setLocalBenefit(option.localBenefit());
            entity.setCarbonHint(option.carbonHint());
            entity.setSortOrder(sort++);
            kit.addSustainableOption(entity);
        }
        return kit;
    }

    private TibetTravelKitResponse toResponse(TibetTravelKit kit) {
        HighlandAssessmentResponse highland = new HighlandAssessmentResponse(
                kit.getRiskScore(),
                kit.getRiskLevel(),
                kit.getRiskLabel(),
                kit.getMaxAltitudeMeters(),
                kit.getHighAltitudeDays(),
                kit.getHighlandSummary(),
                kit.getDayAdvices().stream()
                        .map(advice -> new HighlandDayAdviceResponse(
                                advice.getDayNumber(),
                                advice.getTitle(),
                                advice.getMaxAltitudeMeters(),
                                advice.getRiskLevel(),
                                advice.getPaceAdvice(),
                                advice.getHydrationAdvice(),
                                advice.getActivityLimit(),
                                advice.getWarning()
                        ))
                        .toList(),
                splitList(kit.getAdaptationChecklist()),
                splitList(kit.getWarningSigns()),
                splitList(kit.getGoSlowRules())
        );

        OfflineTravelPackageResponse offlinePackage = new OfflineTravelPackageResponse(
                kit.getItinerary().getId(),
                kit.getTitle(),
                kit.getGeneratedAt(),
                kit.getValidUntil(),
                splitList(kit.getIncludedSections()),
                kit.getEmergencyContacts().stream()
                        .map(contact -> new EmergencyContactResponse(
                                contact.getName(),
                                contact.getPhone(),
                                contact.getDescription()
                        ))
                        .toList(),
                kit.getMapPins().stream()
                        .map(pin -> new OfflineMapPinResponse(
                                pin.getType(),
                                pin.getName(),
                                pin.getLatitude(),
                                pin.getLongitude(),
                                pin.getAltitudeMeters(),
                                pin.getNote()
                        ))
                        .toList(),
                splitList(kit.getOfflineChecklist()),
                splitList(kit.getVoucherHints()),
                splitList(kit.getBackupNotes())
        );

        return new TibetTravelKitResponse(
                kit.getItinerary().getId(),
                kit.getTitle(),
                highland,
                kit.getCultureTips().stream()
                        .map(tip -> new CulturalTipResponse(
                                tip.getScene(),
                                tip.getTitle(),
                                tip.getContext(),
                                splitList(tip.getDoTips()),
                                splitList(tip.getAvoidTips())
                        ))
                        .toList(),
                kit.getPhrases().stream()
                        .map(phrase -> new PhraseGuideItemResponse(
                                phrase.getCategory(),
                                phrase.getChinese(),
                                phrase.getTibetan(),
                                phrase.getEnglish(),
                                phrase.getPronunciation(),
                                phrase.getUsageText()
                        ))
                        .toList(),
                offlinePackage,
                kit.getAlerts().stream()
                        .map(alert -> new TravelAlertResponse(
                                alert.getLevel(),
                                alert.getType(),
                                alert.getTitle(),
                                alert.getMessage(),
                                alert.getAction(),
                                alert.getRelatedDay(),
                                alert.getExpiresAt()
                        ))
                        .toList(),
                kit.getSustainableOptions().stream()
                        .map(option -> new SustainableOptionResponse(
                                option.getCategory(),
                                option.getTitle(),
                                option.getImpact(),
                                splitList(option.getActions()),
                                option.getLocalBenefit(),
                                option.getCarbonHint()
                        ))
                        .toList()
        );
    }

    private String joinList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return String.join(LIST_SEPARATOR, items);
    }

    private List<String> splitList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return List.of(value.split("\\R"));
    }

    private Itinerary loadUserItinerary(User user, Long itineraryId) {
        if (user == null || user.getId() == null || itineraryId == null) {
            throw new NoSuchElementException("行程不存在");
        }
        return itineraryRepository.findByIdAndUserId(itineraryId, user.getId())
                .orElseThrow(() -> new NoSuchElementException("行程不存在"));
    }

    private HighlandAssessmentResponse assessItineraryRisk(Itinerary itinerary, HighlandAssessmentRequest request) {
        List<ItineraryDay> days = sortedDays(itinerary);
        int maxAltitude = days.stream()
                .flatMap(day -> day.getItems().stream())
                .map(this::itemAltitude)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(defaultAltitude(request.maxAltitudeMeters(), 3650));

        int highAltitudeDays = (int) days.stream()
                .filter(day -> maxDayAltitude(day) >= 3800)
                .count();

        List<HighlandDayAdviceResponse> dailyAdvice = days.stream()
                .map(day -> buildDayAdvice(day.getDayNumber(), day.getTitle(), maxDayAltitude(day)))
                .toList();

        return buildAssessment(
                request,
                defaultValue(itinerary.getDays(), Math.max(days.size(), 1)),
                maxAltitude,
                highAltitudeDays,
                dailyAdvice
        );
    }

    private HighlandAssessmentResponse assessManualRisk(HighlandAssessmentRequest request) {
        int days = defaultValue(request.days(), 1);
        int maxAltitude = defaultAltitude(request.maxAltitudeMeters(), 3650);
        int highAltitudeDays = maxAltitude >= 3800 ? days : 0;
        List<HighlandDayAdviceResponse> dailyAdvice = IntStream.rangeClosed(1, days)
                .mapToObj(day -> buildDayAdvice(day, "第 " + day + " 天高原适应", maxAltitude))
                .toList();

        return buildAssessment(request, days, maxAltitude, highAltitudeDays, dailyAdvice);
    }

    private HighlandAssessmentResponse buildAssessment(HighlandAssessmentRequest request,
                                                       int days,
                                                       int maxAltitude,
                                                       int highAltitudeDays,
                                                       List<HighlandDayAdviceResponse> dailyAdvice) {
        int score = 15;
        if (maxAltitude >= 5000) {
            score += 38;
        } else if (maxAltitude >= 4500) {
            score += 28;
        } else if (maxAltitude >= 3800) {
            score += 18;
        } else if (maxAltitude >= 3000) {
            score += 8;
        }
        score += Math.min(24, highAltitudeDays * 4);
        if (days <= 3 && maxAltitude >= 3800) score += 10;
        if (Boolean.TRUE.equals(request.hasCardioRespiratoryHistory())) score += 25;
        if (Boolean.TRUE.equals(request.withChildren())) score += 8;
        if (Boolean.TRUE.equals(request.withSeniors()) || defaultValue(request.age(), 0) >= 60) score += 10;

        String fitness = normalizeChoice(request.fitnessLevel(), "MEDIUM");
        if ("LOW".equals(fitness)) score += 10;
        if ("HIGH".equals(fitness)) score -= 4;

        String pace = normalizeChoice(request.pace(), "BALANCED");
        if ("FAST".equals(pace)) score += 8;
        if ("RELAXED".equals(pace)) score -= 5;

        int clampedScore = Math.max(0, Math.min(100, score));
        String riskLevel = scoreRiskLevel(clampedScore);
        String riskLabel = riskLabel(riskLevel);
        String summary = "本行程最高海拔约 " + maxAltitude + " 米，" + highAltitudeDays
                + " 天涉及 3800 米以上区域，综合判断为" + riskLabel + "。";

        return new HighlandAssessmentResponse(
                clampedScore,
                riskLevel,
                riskLabel,
                maxAltitude,
                highAltitudeDays,
                summary,
                dailyAdvice,
                adaptationChecklist(riskLevel),
                warningSigns(),
                goSlowRules()
        );
    }

    private HighlandDayAdviceResponse buildDayAdvice(Integer dayNumber, String title, int altitude) {
        String risk = altitudeRiskLevel(altitude);
        String paceAdvice = switch (risk) {
            case "HIGH" -> "上午只安排一个核心点位，下午预留休息或吸氧观察时间。";
            case "MEDIUM" -> "控制步行节奏，避免连续爬升和长时间奔走。";
            default -> "可正常游览，但保持补水和防晒。";
        };
        String activityLimit = switch (risk) {
            case "HIGH" -> "避免跑跳、饮酒、洗长时间热水澡和夜间赶路。";
            case "MEDIUM" -> "减少剧烈运动，拍照点停留不宜过久。";
            default -> "按常规旅行强度安排。";
        };
        String warning = switch (risk) {
            case "HIGH" -> "头痛加重、胸闷、持续呕吐或步态不稳时应立即停止上行并寻求医疗帮助。";
            case "MEDIUM" -> "若夜间睡眠明显变差或心率异常，次日降低强度。";
            default -> "初到高原当天仍建议早点休息。";
        };
        return new HighlandDayAdviceResponse(
                dayNumber,
                title,
                altitude,
                risk,
                paceAdvice,
                "少量多次饮水，搭配电解质或热饮，避免空腹长时间移动。",
                activityLimit,
                warning
        );
    }

    private OfflineTravelPackageResponse buildOfflinePackage(Itinerary itinerary) {
        LocalDateTime now = LocalDateTime.now();
        return new OfflineTravelPackageResponse(
                itinerary.getId(),
                itinerary.getTitle(),
                now,
                now.plusDays(14),
                List.of("结构化行程", "景点地图点位", "酒店/门票凭证提示", "高原适应建议", "紧急联系人", "常用藏汉英短语"),
                emergencyContacts(),
                mapPins(itinerary),
                List.of("提前缓存行程页和酒店地址", "保存身份证件照片和订单号", "下载离线地图", "准备充电宝、保暖层、防晒和常用药", "把同行人电话写入纸质备份"),
                List.of("酒店订单：保留入住人、手机号、入住日期和订单号", "景区门票：保留入园日期、身份证件和核销码", "包车/向导：保留车牌、司机电话和集合点"),
                List.of("弱网区域优先使用短信或电话联系", "每日出发前向酒店或向导确认道路和景区开放", "高海拔日不要把最后一班车/最后入园时间作为唯一方案")
        );
    }

    private List<TravelAlertResponse> buildTravelAlerts(Itinerary itinerary, HighlandAssessmentResponse highland) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(12);
        List<TravelAlertResponse> alerts = new ArrayList<>();
        if ("HIGH".equals(highland.riskLevel()) || "EXTREME".equals(highland.riskLevel())) {
            alerts.add(new TravelAlertResponse(
                    "HIGH",
                    "ALTITUDE",
                    "高海拔日程需要降强度",
                    "行程包含 4500 米以上或连续高海拔移动，建议减少当天景点数量并保留撤退方案。",
                    "优先选择“更轻松”版本，或把高海拔节点改到适应后的第 3 天以后。",
                    firstHighAltitudeDay(itinerary),
                    expiresAt
            ));
        }

        Month month = LocalDate.now().getMonth();
        if (month.getValue() >= Month.MAY.getValue() && month.getValue() <= Month.OCTOBER.getValue()) {
            alerts.add(new TravelAlertResponse(
                    "MEDIUM",
                    "CROWD",
                    "旺季景区客流提醒",
                    "当前处于西藏旅游旺季，热门寺庙、湖泊和观景台更容易出现排队和停车压力。",
                    "把核心景点放在上午，备选点位放在下午。",
                    null,
                    expiresAt
            ));
        }

        alerts.add(new TravelAlertResponse(
                "MEDIUM",
                "ROAD",
                "长距离道路通行需二次确认",
                "高原道路受天气、施工和临时管制影响明显，跨地区移动前应向酒店、向导或官方渠道确认。",
                "出发前一晚确认路况，并准备热水、干粮和离线导航。",
                null,
                expiresAt
        ));
        alerts.add(new TravelAlertResponse(
                "LOW",
                "CULTURE",
                "寺庙和民俗空间参观提示",
                "进入宗教空间请降低音量，拍摄前先看现场提示并征得同意。",
                "把文化礼仪助手加入离线包。",
                null,
                expiresAt.plusHours(12)
        ));
        return alerts;
    }

    private List<CulturalTipResponse> getCultureTipsForItinerary(Itinerary itinerary) {
        String preference = Optional.ofNullable(itinerary.getPreference()).orElse("").toLowerCase(Locale.ROOT);
        if (preference.contains("photo") || preference.contains("摄影")) {
            return getCultureTips("PHOTOGRAPHY");
        }
        if (preference.contains("cultural") || preference.contains("文化")) {
            return getCultureTips("TEMPLE");
        }
        return List.of(baseCultureTips().get(0), baseCultureTips().get(2), baseCultureTips().get(3));
    }

    private List<CulturalTipResponse> baseCultureTips() {
        return List.of(
                new CulturalTipResponse(
                        "TEMPLE",
                        "寺庙与转经礼仪",
                        "寺庙是信众正在使用的宗教空间，游览时以安静、顺向和不打扰为原则。",
                        List.of("按现场提示顺时针行走", "进入殿堂前摘帽并降低音量", "供灯、转经、排队时给信众留出空间"),
                        List.of("未经允许拍摄佛像和僧人", "跨越经书、法器或门槛中央", "在殿堂内饮食或大声讲解")
                ),
                new CulturalTipResponse(
                        "FOLK_CUSTOM",
                        "民俗家访与村落互动",
                        "村落体验不是表演场，先确认边界，再交流体验。",
                        List.of("拍人像前先询问", "接受茶点时表达感谢", "购买手作尊重明码标价"),
                        List.of("随意进入民居和牧场", "对服饰、信仰和生活方式开玩笑", "把儿童作为摆拍对象")
                ),
                new CulturalTipResponse(
                        "PHOTOGRAPHY",
                        "摄影与航拍边界",
                        "西藏很多地点对摄影、航拍和边境区域有特殊要求，现场规则优先。",
                        List.of("拍摄前观察禁拍标识", "航拍前确认空域和景区规则", "给被拍摄者看照片并尊重删除请求"),
                        List.of("在寺庙殿堂内使用闪光灯", "在边境、军事或检查区域拍摄", "为取景进入未开放湿地和草甸")
                ),
                new CulturalTipResponse(
                        "ECOLOGY",
                        "高原生态与环保",
                        "高寒生态恢复慢，一次不当踩踏也可能长期留下痕迹。",
                        List.of("自带水杯和垃圾袋", "走已有步道和观景平台", "远距离观察动物"),
                        List.of("投喂野生动物", "采摘植物或堆砌石堆", "把经幡、哈达随意系在非指定区域")
                ),
                new CulturalTipResponse(
                        "FOOD",
                        "饮食与身体适应",
                        "初到高原时饮食宜温热、清淡、规律，避免让肠胃和心肺同时承压。",
                        List.of("第一天选择热汤、主食和清淡菜", "随身带巧克力或能量棒", "少量多次补水"),
                        List.of("初到当天饮酒", "空腹赶路", "连续食用过辣过油食物")
                )
        );
    }

    private List<PhraseGuideItemResponse> basePhrasebook() {
        return List.of(
                new PhraseGuideItemResponse("BASIC", "你好", "བཀྲ་ཤིས་བདེ་ལེགས།", "Hello", "Tashi delek", "见面问候"),
                new PhraseGuideItemResponse("BASIC", "谢谢", "ཐུགས་རྗེ་ཆེ།", "Thank you", "Thuk-je-che", "接受帮助或服务后使用"),
                new PhraseGuideItemResponse("BASIC", "再见", "ག་ལེར་ཕེབས།", "Goodbye", "Kale phe", "告别时使用"),
                new PhraseGuideItemResponse("EMERGENCY", "我需要帮助", "རོགས་པ་དགོས།", "I need help", "Rokpa go", "身体不适或迷路时使用"),
                new PhraseGuideItemResponse("EMERGENCY", "请帮我叫医生", "སྨན་པ་སྐད་གཏོང་རོགས།", "Please call a doctor", "Menpa ke tong rok", "紧急医疗场景"),
                new PhraseGuideItemResponse("TRAFFIC", "请慢一点", "དལ་བུར་ཕེབས་རོགས།", "Please slow down", "Dalbur phe rok", "包车或徒步节奏过快时"),
                new PhraseGuideItemResponse("TEMPLE", "这里可以拍照吗", "འདིར་པར་རྒྱག་ཆོག་གམ།", "Can I take photos here?", "Dir par gyap chok gam", "进入寺庙、民俗空间前询问"),
                new PhraseGuideItemResponse("HOTEL", "我有预订", "ང་ལ་སྔོན་མངགས་ཡོད།", "I have a reservation", "Nga la ngon ngak yo", "酒店入住"),
                new PhraseGuideItemResponse("SHOPPING", "多少钱", "རིན་གོང་ག་ཚོད་རེད།", "How much is it?", "Rin gong gatso re", "购买手作或补给"),
                new PhraseGuideItemResponse("FOOD", "不要太辣", "ཚ་བོ་མང་པོ་མ་དགོས།", "Not too spicy", "Tsawo mangpo ma go", "点餐时使用")
        );
    }

    private List<EmergencyContactResponse> emergencyContacts() {
        return List.of(
                new EmergencyContactResponse("医疗急救", "120", "出现严重高反、外伤或急症时优先拨打"),
                new EmergencyContactResponse("公安报警", "110", "人身安全、证件遗失或紧急求助"),
                new EmergencyContactResponse("消防救援", "119", "火灾、险情和救援"),
                new EmergencyContactResponse("道路救援", "12122", "高速和道路通行求助"),
                new EmergencyContactResponse("政务和旅游投诉咨询", "12345", "景区、消费、交通等综合咨询")
        );
    }

    private List<OfflineMapPinResponse> mapPins(Itinerary itinerary) {
        Map<String, OfflineMapPinResponse> pins = new LinkedHashMap<>();
        sortedDays(itinerary).forEach(day -> day.getItems().forEach(item -> {
            ScenicSpot spot = item.getScenicSpot();
            if (spot == null || spot.getLatitude() == null || spot.getLongitude() == null) {
                return;
            }
            pins.putIfAbsent("SPOT:" + spot.getId(), new OfflineMapPinResponse(
                    "SCENIC_SPOT",
                    spot.getName(),
                    spot.getLatitude(),
                    spot.getLongitude(),
                    itemAltitude(item),
                    "第 " + day.getDayNumber() + " 天 · " + Optional.ofNullable(spot.getOpenInfo()).orElse("开放信息以景区当日公告为准")
            ));
        }));
        return new ArrayList<>(pins.values());
    }

    private List<ItineraryDay> sortedDays(Itinerary itinerary) {
        return itinerary.getItineraryDays().stream()
                .sorted(Comparator.comparing(day -> defaultValue(day.getDayNumber(), 0)))
                .toList();
    }

    private int maxDayAltitude(ItineraryDay day) {
        return day.getItems().stream()
                .map(this::itemAltitude)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(3650);
    }

    private Integer itemAltitude(ItineraryItem item) {
        if (item.getAltitudeMeters() != null) {
            return item.getAltitudeMeters();
        }
        if (item.getScenicSpot() != null) {
            return parseAltitude(item.getScenicSpot().getAltitude());
        }
        return null;
    }

    private Integer parseAltitude(String altitude) {
        if (!StringUtils.hasText(altitude)) {
            return null;
        }
        Matcher matcher = ALTITUDE_NUMBER.matcher(altitude.replace(",", ""));
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String altitudeRiskLevel(int altitude) {
        if (altitude >= 5000) return "HIGH";
        if (altitude >= 3800) return "MEDIUM";
        return "LOW";
    }

    private String scoreRiskLevel(int score) {
        if (score >= 80) return "EXTREME";
        if (score >= 60) return "HIGH";
        if (score >= 35) return "MEDIUM";
        return "LOW";
    }

    private String riskLabel(String riskLevel) {
        return switch (riskLevel) {
            case "EXTREME" -> "极高风险";
            case "HIGH" -> "高风险";
            case "MEDIUM" -> "中等风险";
            default -> "低风险";
        };
    }

    private List<String> adaptationChecklist(String riskLevel) {
        List<String> base = new ArrayList<>(List.of(
                "抵达当天不洗长时间热水澡，不饮酒",
                "每天保证热水、保暖层、防晒和能量补给",
                "睡前观察头痛、胸闷、恶心和心率变化",
                "高海拔日预留可取消或可替换节点"
        ));
        if ("HIGH".equals(riskLevel) || "EXTREME".equals(riskLevel)) {
            base.add("提前咨询医生，确认是否适合进入 4500 米以上区域");
            base.add("安排低海拔撤退城市或供氧酒店作为备选");
        }
        return base;
    }

    private List<String> warningSigns() {
        return List.of("头痛持续加重", "胸闷或呼吸困难", "持续呕吐", "走路不稳或意识模糊", "静息心率异常升高");
    }

    private List<String> goSlowRules() {
        return List.of("先适应，再上升", "上午核心游览，下午减负", "每天只跨一个海拔强度等级", "宁可少打卡，也不要硬扛高反");
    }

    private Integer firstHighAltitudeDay(Itinerary itinerary) {
        return sortedDays(itinerary).stream()
                .filter(day -> maxDayAltitude(day) >= 3800)
                .map(ItineraryDay::getDayNumber)
                .findFirst()
                .orElse(null);
    }

    private String primaryRegion(Itinerary itinerary) {
        return sortedDays(itinerary).stream()
                .map(ItineraryDay::getRegion)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("西藏");
    }

    private int defaultAltitude(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private int defaultValue(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String normalizeChoice(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeFilter(String value) {
        String sanitized = InputSanitizer.optionalTagFilter(value);
        return sanitized == null ? null : sanitized.toUpperCase(Locale.ROOT);
    }
}
