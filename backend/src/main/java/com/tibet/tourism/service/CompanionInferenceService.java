package com.tibet.tourism.service;

import com.tibet.tourism.entity.Booking;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.UserVisitHistory;
import com.tibet.tourism.repository.BookingRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 旅伴类型推断服务
 * 基于用户的历史行为数据隐性地推断用户的旅伴类型
 */
@Service
public class CompanionInferenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(CompanionInferenceService.class);
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private UserVisitHistoryRepository historyRepository;
    
    /**
     * 旅伴类型推断结果
     */
    public static class CompanionInference {
        private String companionType; // ALONE, COUPLE, FAMILY, FRIENDS, GROUP
        private double confidence; // 置信度 0.0-1.0
        private String reason; // 推断原因
        
        public CompanionInference(String companionType, double confidence, String reason) {
            this.companionType = companionType;
            this.confidence = confidence;
            this.reason = reason;
        }
        
        // Getters
        public String getCompanionType() { return companionType; }
        public double getConfidence() { return confidence; }
        public String getReason() { return reason; }
    }
    
    /**
     * 推断用户的旅伴类型
     * 
     * @param userId 用户ID
     * @return 旅伴类型推断结果
     */
    public CompanionInference inferCompanionType(Long userId) {
        logger.info("🔍 开始推断用户 {} 的旅伴类型", userId);
        
        // 1. 基于预订票数推断
        CompanionInference bookingInference = inferFromBookings(userId);
        
        // 2. 基于访问模式推断
        CompanionInference patternInference = inferFromVisitPatterns(userId);
        
        // 3. 基于景点偏好推断
        CompanionInference preferenceInference = inferFromSpotPreferences(userId);
        
        // 4. 综合推断
        CompanionInference finalInference = combineInferences(bookingInference, patternInference, preferenceInference);
        
        logger.info("✅ 推断完成: {} (置信度: {:.2f}%) - {}", 
                finalInference.getCompanionType(), 
                finalInference.getConfidence() * 100,
                finalInference.getReason());
        
        return finalInference;
    }
    
    /**
     * 基于预订票数推断
     */
    private CompanionInference inferFromBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        
        if (bookings.isEmpty()) {
            return new CompanionInference("ALONE", 0.3, "无预订记录，默认推断为独自旅行");
        }
        
        // 统计票数分布
        Map<Integer, Long> ticketCountDistribution = bookings.stream()
                .filter(b -> b.getTicketCount() != null)
                .collect(Collectors.groupingBy(
                    Booking::getTicketCount,
                    Collectors.counting()
                ));
        
        // 计算平均票数
        double avgTickets = bookings.stream()
                .filter(b -> b.getTicketCount() != null)
                .mapToInt(Booking::getTicketCount)
                .average()
                .orElse(1.0);
        
        // 最常见的票数
        int mostCommonTickets = ticketCountDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(1);
        
        String companionType;
        double confidence;
        String reason;
        
        if (avgTickets <= 1.2) {
            // 平均票数 <= 1.2，推断为独自旅行
            companionType = "ALONE";
            confidence = 0.85;
            reason = String.format("平均预订票数%.1f张，推断为独自旅行", avgTickets);
        } else if (avgTickets >= 1.8 && avgTickets <= 2.5) {
            // 平均票数 1.8-2.5，推断为情侣
            companionType = "COUPLE";
            confidence = 0.80;
            reason = String.format("平均预订票数%.1f张，推断为情侣出行", avgTickets);
        } else if (avgTickets >= 2.5 && avgTickets <= 4.5) {
            // 平均票数 2.5-4.5，推断为家庭
            companionType = "FAMILY";
            confidence = 0.75;
            reason = String.format("平均预订票数%.1f张，推断为家庭出行", avgTickets);
        } else if (avgTickets >= 4.5 && avgTickets <= 8.0) {
            // 平均票数 4.5-8.0，推断为朋友
            companionType = "FRIENDS";
            confidence = 0.70;
            reason = String.format("平均预订票数%.1f张，推断为朋友出行", avgTickets);
        } else {
            // 平均票数 > 8.0，推断为团队
            companionType = "GROUP";
            confidence = 0.75;
            reason = String.format("平均预订票数%.1f张，推断为团队出行", avgTickets);
        }
        
        // 如果最常见的票数与平均值差异较大，调整置信度
        if (Math.abs(mostCommonTickets - avgTickets) > 1.0) {
            confidence *= 0.8; // 降低置信度
        }
        
        return new CompanionInference(companionType, confidence, reason);
    }
    
    /**
     * 基于访问模式推断
     */
    private CompanionInference inferFromVisitPatterns(Long userId) {
        List<UserVisitHistory> histories = historyRepository.findByUserId(userId);
        
        if (histories.isEmpty()) {
            return new CompanionInference("ALONE", 0.3, "无访问记录");
        }
        
        // 1. 分析访问时间（工作日 vs 周末）
        long weekendVisits = histories.stream()
                .filter(h -> h.getVisitDate() != null)
                .filter(h -> {
                    DayOfWeek dayOfWeek = h.getVisitDate().getDayOfWeek();
                    return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
                })
                .count();
        
        double weekendRatio = (double) weekendVisits / histories.size();
        
        // 2. 分析停留时间
        double avgDwellTime = histories.stream()
                .filter(h -> h.getDwellSeconds() != null && h.getDwellSeconds() > 0)
                .mapToInt(UserVisitHistory::getDwellSeconds)
                .average()
                .orElse(0.0);
        
        // 3. 分析访问频率
        long recentVisits = histories.stream()
                .filter(h -> h.getVisitDate() != null)
                .filter(h -> {
                    long daysAgo = java.time.Duration.between(h.getVisitDate(), LocalDateTime.now()).toDays();
                    return daysAgo <= 90; // 最近90天
                })
                .count();
        
        String companionType;
        double confidence;
        String reason;
        
        // 推断逻辑
        if (weekendRatio > 0.6 && avgDwellTime > 1800) {
            // 周末访问多 + 停留时间长 = 家庭出行
            companionType = "FAMILY";
            confidence = 0.70;
            reason = String.format("周末访问比例%.0f%%，平均停留%.0f分钟，推断为家庭出行", 
                    weekendRatio * 100, avgDwellTime / 60);
        } else if (weekendRatio > 0.5 && avgDwellTime > 1200) {
            // 周末访问多 + 停留时间中等 = 情侣/朋友
            companionType = "COUPLE";
            confidence = 0.65;
            reason = String.format("周末访问比例%.0f%%，平均停留%.0f分钟，推断为情侣出行", 
                    weekendRatio * 100, avgDwellTime / 60);
        } else if (avgDwellTime < 600 && recentVisits > 5) {
            // 停留时间短 + 访问频繁 = 独自旅行
            companionType = "ALONE";
            confidence = 0.75;
            reason = String.format("平均停留%.0f分钟，访问频繁，推断为独自旅行", avgDwellTime / 60);
        } else if (recentVisits > 10) {
            // 访问非常频繁 = 可能是团队或朋友
            companionType = "FRIENDS";
            confidence = 0.60;
            reason = String.format("最近90天访问%d次，推断为朋友出行", recentVisits);
        } else {
            // 默认推断
            companionType = "ALONE";
            confidence = 0.50;
            reason = "访问模式不明显，默认推断为独自旅行";
        }
        
        return new CompanionInference(companionType, confidence, reason);
    }
    
    /**
     * 基于景点偏好推断
     */
    private CompanionInference inferFromSpotPreferences(Long userId) {
        List<UserVisitHistory> histories = historyRepository.findByUserId(userId);
        
        if (histories.isEmpty()) {
            return new CompanionInference("ALONE", 0.3, "无访问记录");
        }
        
        // 统计各类别景点的访问比例
        Map<ScenicSpot.Category, Long> categoryCounts = histories.stream()
                .filter(h -> h.getSpot() != null && h.getSpot().getCategory() != null)
                .collect(Collectors.groupingBy(
                    h -> h.getSpot().getCategory(),
                    Collectors.counting()
                ));
        
        long total = categoryCounts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return new CompanionInference("ALONE", 0.3, "无有效访问记录");
        }
        
        // 计算各类别比例
        double naturalRatio = (double) categoryCounts.getOrDefault(ScenicSpot.Category.NATURAL, 0L) / total;
        double culturalRatio = (double) categoryCounts.getOrDefault(ScenicSpot.Category.CULTURAL, 0L) / total;
        double religiousRatio = (double) categoryCounts.getOrDefault(ScenicSpot.Category.RELIGIOUS, 0L) / total;
        double historicalRatio = (double) categoryCounts.getOrDefault(ScenicSpot.Category.HISTORICAL, 0L) / total;
        
        String companionType;
        double confidence;
        String reason;
        
        // 推断逻辑
        if (religiousRatio > 0.4 || culturalRatio > 0.5) {
            // 宗教/文化景点多 = 家庭出行（通常家庭更偏好文化教育类）
            companionType = "FAMILY";
            confidence = 0.65;
            reason = String.format("文化/宗教类景点占比%.0f%%，推断为家庭出行", 
                    (culturalRatio + religiousRatio) * 100);
        } else if (naturalRatio > 0.6) {
            // 自然风光多 = 情侣/朋友（更偏好浪漫、风景优美的地方）
            companionType = "COUPLE";
            confidence = 0.70;
            reason = String.format("自然风光类景点占比%.0f%%，推断为情侣出行", naturalRatio * 100);
        } else if (historicalRatio > 0.3) {
            // 历史遗迹多 = 可能是朋友/团队（喜欢探索）
            companionType = "FRIENDS";
            confidence = 0.60;
            reason = String.format("历史遗迹类景点占比%.0f%%，推断为朋友出行", historicalRatio * 100);
        } else {
            // 类别分布均匀 = 独自旅行（个人兴趣广泛）
            companionType = "ALONE";
            confidence = 0.55;
            reason = "景点类别分布均匀，推断为独自旅行";
        }
        
        return new CompanionInference(companionType, confidence, reason);
    }
    
    /**
     * 综合多个推断结果
     */
    private CompanionInference combineInferences(CompanionInference... inferences) {
        // 统计各类型的加权得分
        Map<String, Double> scores = new java.util.HashMap<>();
        Map<String, Integer> counts = new java.util.HashMap<>();
        Map<String, StringBuilder> reasons = new java.util.HashMap<>();
        
        for (CompanionInference inference : inferences) {
            String type = inference.getCompanionType();
            double confidence = inference.getConfidence();
            
            scores.merge(type, confidence, (a, b) -> a + b);
            counts.merge(type, 1, (a, b) -> a + b);
            reasons.computeIfAbsent(type, k -> new StringBuilder())
                    .append(inference.getReason()).append("; ");
        }
        
        // 选择得分最高的类型
        String bestType = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("ALONE");
        
        // 计算综合置信度（取平均值，但考虑一致性）
        double totalScore = scores.getOrDefault(bestType, 0.0);
        int count = counts.getOrDefault(bestType, 1);
        double avgConfidence = totalScore / count;
        
        // 如果多个推断结果一致，提升置信度
        if (count >= 2) {
            avgConfidence = Math.min(1.0, avgConfidence * 1.2);
        }
        
        // 如果推断结果不一致，降低置信度
        if (scores.size() > 1 && count < inferences.length) {
            avgConfidence *= 0.8;
        }
        
        String combinedReason = reasons.getOrDefault(bestType, new StringBuilder("综合推断")).toString();
        if (combinedReason.endsWith("; ")) {
            combinedReason = combinedReason.substring(0, combinedReason.length() - 2);
        }
        
        return new CompanionInference(bestType, avgConfidence, combinedReason);
    }
    
    /**
     * 获取推断的旅伴类型（简化接口）
     */
    public String getCompanionType(Long userId) {
        CompanionInference inference = inferCompanionType(userId);
        return inference.getCompanionType();
    }
    
    /**
     * 获取推断的旅伴类型和置信度
     */
    public Map<String, Object> getCompanionTypeWithConfidence(Long userId) {
        CompanionInference inference = inferCompanionType(userId);
        return Map.of(
            "companionType", inference.getCompanionType(),
            "confidence", inference.getConfidence(),
            "reason", inference.getReason()
        );
    }
}

