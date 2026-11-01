package com.tibet.tourism.modules.route.application;

import com.tibet.tourism.modules.route.domain.Itinerary;
import com.tibet.tourism.modules.route.domain.ItineraryDay;
import com.tibet.tourism.modules.route.domain.ItineraryItem;
import com.tibet.tourism.modules.route.web.dto.itinerary.ItineraryReadinessResponse;
import com.tibet.tourism.modules.route.web.dto.itinerary.ItineraryReadinessResponse.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/** Shared conservative product constraints. Passing these checks never certifies travel safety. */
public final class ItineraryTrustPolicy {
    public static final String VERSION = "2026-10-31.1";
    public static final int MAX_PLANNED_SLEEP_ALTITUDE = 4500;
    private static final Pattern SLEEP = Pattern.compile("住宿|入住|夜宿|过夜|宿营|accommodation|overnight|sleep|hotel", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTITUDE = Pattern.compile("(?<![0-9])(\\d{4,5})\\s*(?:米|m(?:eters?)?\\b)", Pattern.CASE_INSENSITIVE);
    private ItineraryTrustPolicy() {}

    public static boolean violatesOvernightPolicy(String markdown) {
        if (markdown == null) return false;
        for (String line : markdown.split("\\R")) {
            if (!SLEEP.matcher(line).find()) continue;
            // Only lodging suggestions are blocked, not warnings advising against them.
            if (line.strip().replaceFirst("^[-*]\\s*", "").matches("^(禁止|不要|不安排|避免)[^；;。]*$")) continue;
            if (line.contains("大本营") || line.contains("绒布寺") || line.contains("纳木错湖边")) return true;
            var matcher = ALTITUDE.matcher(line);
            while (matcher.find()) if (Integer.parseInt(matcher.group(1)) > MAX_PLANNED_SLEEP_ALTITUDE) return true;
        }
        return false;
    }

    public static ItineraryReadinessResponse evaluate(Itinerary itinerary) {
        List<Issue> issues = new ArrayList<>();
        if (itinerary.getStartDate() == null || itinerary.getStartDate().isBefore(LocalDate.now())) {
            issues.add(new Issue("DEPARTURE_DATE", "BLOCKING", null, null, "请确认有效的出发日期；历史方案需要重新核验。"));
        }
        if (itinerary.getDays() == null || itinerary.getDays() != itinerary.getItineraryDays().size()) {
            issues.add(new Issue("DAY_COUNT", "BLOCKING", null, null, "行程天数与逐日内容不一致。"));
        }
        if (itinerary.getTravelers() == null || itinerary.getTravelers() < 1 || itinerary.getTravelers() > 20) issues.add(new Issue("TRAVELERS_UNKNOWN", "BLOCKING", null, null, "历史行程没有出行人数，请按实际人数重新生成核验。"));
        if (!VERSION.equals(itinerary.getGenerationRuleVersion())) issues.add(new Issue("RULE_VERSION_CHANGED", "WARNING", null, null, "方案由旧版或未知规则生成；本次按最新规则重新检查，原始来源仍需核验。"));
        List<Interval> intervals = new ArrayList<>();
        Set<Integer> numbers = new HashSet<>();
        for (ItineraryDay day : itinerary.getItineraryDays()) {
            if (day.getDayNumber() == null || day.getDayNumber() < 1 || (itinerary.getDays() != null && day.getDayNumber() > itinerary.getDays()) || !numbers.add(day.getDayNumber())
                    || (itinerary.getStartDate() != null && day.getDayNumber() != null && day.getDayNumber() > 0
                    && !Objects.equals(day.getTravelDate(), itinerary.getStartDate().plusDays(day.getDayNumber() - 1L)))) {
                add(issues, "DAY_DATE", "BLOCKING", day, null, "逐日编号或日期不一致，需要调整后再核验。");
            }
            boolean hasHotel = false;
            for (ItineraryItem item : day.getItems()) {
                if (item.getEstimatedCost() == null || item.getEstimatedCost().signum() < 0) add(issues, "PRICE_UNKNOWN", "WARNING", day, item, "费用缺失或无效，不能视为确认报价。");
                try {
                    int start = LocalTime.parse(Objects.requireNonNullElse(item.getStartTime(), "")).toSecondOfDay() / 60;
                    int duration = item.getDurationMinutes() == null ? 0 : item.getDurationMinutes();
                    if (duration <= 0 || duration > 1440) add(issues, "DURATION_UNKNOWN", "WARNING", day, item, "停留或交通时长需要确认。");
                    if (start + duration > 1440 && item.getItemType() != ItineraryItem.ItemType.HOTEL) add(issues, "CROSSES_MIDNIGHT", "BLOCKING", day, item, "非住宿安排跨越当天，请明确拆分日期。" );
                    if (duration > 0 && duration <= 1440 && day.getTravelDate() != null) {
                        long absoluteStart = day.getTravelDate().toEpochDay() * 1440L + start;
                        Interval interval = new Interval(absoluteStart, absoluteStart + duration, day, item);
                        for (Interval other : intervals) {
                            if (interval.start() < other.end() && interval.end() > other.start()) {
                                add(issues, "TIME_OVERLAP", "BLOCKING", day, item,
                                        "与第" + other.day().getDayNumber() + "天的“" + other.item().getTitle() + "”时段重叠，请调整（包含跨日住宿）。");
                                break;
                            }
                        }
                        intervals.add(interval);
                    }
                } catch (DateTimeParseException ex) { add(issues, "TIME_UNKNOWN", "WARNING", day, item, "此项时间尚未确认。"); }
                if (item.getItemType() == ItineraryItem.ItemType.HOTEL) {
                    hasHotel = true;
                    if (item.getHotel() == null || item.getRoomType() == null) add(issues, "LODGING_UNRESOLVED", "BLOCKING", day, item, "住宿地点或适合人数的房型未匹配，未确认可订。");
                    if (item.getAltitudeMeters() == null) add(issues, "SLEEP_ALTITUDE_UNKNOWN", "WARNING", day, item, "住宿海拔未核实；景点海拔不能代替住宿海拔。");
                    else if (item.getAltitudeMeters() > MAX_PLANNED_SLEEP_ALTITUDE) add(issues, "SLEEP_ALTITUDE_POLICY", "BLOCKING", day, item, "住宿超过本产品的规划阈值，需要更换方案；阈值不是医学安全保证。");
                    if (item.getRoomType() != null && (item.getRoomType().getInventoryUnits() == null || item.getRoomType().getInventoryUnits() <= 0)) add(issues, "INVENTORY_UNKNOWN", "BLOCKING", day, item, "房型库存未配置或已售罄；有房型资料不代表可订。");
                    if (item.getRoomType() != null && (item.getRoomType().getCapacity() == null || item.getRoomType().getCapacity() < Math.max(1, itinerary.getTravelers() == null ? 1 : itinerary.getTravelers()))) add(issues, "CAPACITY_UNKNOWN", "BLOCKING", day, item, "房型容量未核实。");
                }
                if (item.getItemType() == ItineraryItem.ItemType.SCENIC_SPOT) add(issues, "OPENING_RESERVATION_UNKNOWN", "WARNING", day, item, "开放时段、预约余量及临时公告需向官方确认。");
                if (item.getItemType() == ItineraryItem.ItemType.TRANSPORT) add(issues, "TRAVEL_TIME_UNVERIFIED", "WARNING", day, item, "交通时长是规划占位，并非实时导航；请核验路况和接驳。");
            }
            if (!hasHotel) add(issues, "LODGING_MISSING", "BLOCKING", day, null, "缺少当晚住宿或明确返程安排。");
        }
        issues.add(new Issue("SUPPLIER_UNCONFIRMED", "WARNING", null, null, "费用为参考估计；提交咨询不代表供应商确认、付款成功或保证履约。"));
        String status = issues.stream().anyMatch(i -> "BLOCKING".equals(i.severity())) ? "BLOCKED" : "NEEDS_CONFIRMATION";
        return new ItineraryReadinessResponse(VERSION, java.time.OffsetDateTime.now(ZoneOffset.UTC), status, List.copyOf(issues),
                List.of(new Source("tourism", "西藏文旅官方公告", "https://wlt.xizang.gov.cn/", "REFERENCE_ONLY", null),
                        new Source("permit", "国家移民管理局办事服务", "https://s.nia.gov.cn/", "REFERENCE_ONLY", null),
                        new Source("health", "CDC 高海拔旅行参考", "https://www.cdc.gov/yellow-book/hcp/environmental-hazards-risks/high-altitude-travel-and-altitude-illness.html", "REFERENCE_ONLY", null)),
                List.of(new ChecklistItem("reservation", "按出行日期核对景区开放和官方预约", "TO_CONFIRM", "tourism"),
                        new ChecklistItem("documents", "按身份与目的地向官方确认所需证件（不同手续不可混同）", "TO_CONFIRM", "permit"),
                        new ChecklistItem("altitude", "核实每晚住宿海拔与适应节奏；健康问题咨询专业人士", "TO_CONFIRM", "health"),
                        new ChecklistItem("supplier", "确认供应商、最终价格、退改条件和真实预订凭证", "TO_CONFIRM", "tourism"),
                        new ChecklistItem("offline", "出发前更新并保存离线行程、联系信息及备选安排", "TO_CONFIRM", "tourism")),
                "规则检查不等于已核验可执行或医疗安全评估。来源为参考入口，尚未逐项人工核验；离线副本可能过时。");
    }
    private record Interval(long start, long end, ItineraryDay day, ItineraryItem item) {}

    private static void add(List<Issue> list, String code, String severity, ItineraryDay day, ItineraryItem item, String text) {
        list.add(new Issue(code, severity, day.getDayNumber(), item == null ? null : item.getId(), text));
    }
}
