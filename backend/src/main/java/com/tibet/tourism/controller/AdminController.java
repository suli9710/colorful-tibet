package com.tibet.tourism.controller;

import com.tibet.tourism.dto.DashboardStatsDTO;
import com.tibet.tourism.dto.ScenicSpotSimpleDTO;
import com.tibet.tourism.dto.TagUpdateRequest;
import com.tibet.tourism.entity.Booking;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.repository.BookingRepository;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.service.SpotTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ScenicSpotRepository scenicSpotRepository;

    @Autowired
    private SpotTagService spotTagService;


    @GetMapping("/users")
    public List<com.tibet.tourism.entity.User> getUsers() {
        return userRepository.findAll();
    }

    @org.springframework.web.bind.annotation.PostMapping("/users/{id}/role")
    public org.springframework.http.ResponseEntity<?> updateUserRole(@org.springframework.web.bind.annotation.PathVariable @NonNull Long id, @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> payload) {
        String roleStr = payload.get("role");
        com.tibet.tourism.entity.User.Role role;
        try {
            role = com.tibet.tourism.entity.User.Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return org.springframework.http.ResponseEntity.badRequest().body("Invalid role");
        }

        return userRepository.findById(id).map(user -> {
            user.setRole(role);
            userRepository.save(user);
            return org.springframework.http.ResponseEntity.ok().build();
        }).orElse(org.springframework.http.ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public DashboardStatsDTO getStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        
        stats.setUserCount(userRepository.count());
        stats.setBookingCount(bookingRepository.count());
        
        // Calculate total revenue
        List<Booking> allBookings = bookingRepository.findAll();
        BigDecimal revenue = allBookings.stream()
                .map(Booking::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalRevenue(revenue);

        // Get recent bookings (mocking "recent" by just taking last 5 for now, 
        // ideally should use a custom query like findTop5ByOrderByCreatedAtDesc)
        List<Booking> recentBookings = allBookings.stream()
                .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());
        stats.setRecentBookings(recentBookings);

        // Get popular spots sorted by visitCount (click count) in descending order
        List<ScenicSpot> popularSpots = scenicSpotRepository.findAll().stream()
                .sorted((s1, s2) -> {
                    Integer count1 = s1.getVisitCount() != null ? s1.getVisitCount() : 0;
                    Integer count2 = s2.getVisitCount() != null ? s2.getVisitCount() : 0;
                    return count2.compareTo(count1); // Descending order
                })
                .limit(5)
                .collect(Collectors.toList());
        stats.setPopularSpots(popularSpots);

        return stats;
    }

    @GetMapping("/spots")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllSpots() {
        System.out.println("=== AdminController.getAllSpots called ===");
        try {
            // 使用不加载tags的查询，然后转换为DTO，完全避免懒加载和序列化问题
            List<ScenicSpot> spots = scenicSpotRepository.findAllWithoutTags();
            List<ScenicSpotSimpleDTO> dtos = spots.stream()
                    .map(ScenicSpotSimpleDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            System.out.println("=== Error in getAllSpots ===");
            e.printStackTrace();
            String message = e.getMessage() != null ? e.getMessage() : "Unknown error: " + e.getClass().getName();
            Map<String, String> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", "获取景点数据失败");
            errorResponse.put("message", message);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @org.springframework.web.bind.annotation.PutMapping("/spots/{id}")
    public ScenicSpotSimpleDTO updateSpot(
            @org.springframework.web.bind.annotation.PathVariable @NonNull Long id,
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> payload) {
        
        ScenicSpot spot = scenicSpotRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Spot not found"));
            if (payload.containsKey("imageUrl")) {
                spot.setImageUrl((String) payload.get("imageUrl"));
            }
            if (payload.containsKey("name")) {
                spot.setName((String) payload.get("name"));
            }
            if (payload.containsKey("description")) {
                spot.setDescription((String) payload.get("description"));
            }
            if (payload.containsKey("ticketPrice")) {
                spot.setTicketPrice(new BigDecimal(payload.get("ticketPrice").toString()));
            }
            if (payload.containsKey("peakSeasonPrice")) {
                Object v = payload.get("peakSeasonPrice");
                spot.setPeakSeasonPrice(v != null ? new BigDecimal(v.toString()) : null);
            }
            if (payload.containsKey("offSeasonPrice")) {
                Object v = payload.get("offSeasonPrice");
                spot.setOffSeasonPrice(v != null ? new BigDecimal(v.toString()) : null);
            }
            if (payload.containsKey("peakStartDate")) {
                Object v = payload.get("peakStartDate");
                spot.setPeakStartDate(v != null && !v.toString().isEmpty() ? java.time.LocalDate.parse(v.toString()) : null);
            }
            if (payload.containsKey("peakEndDate")) {
                Object v = payload.get("peakEndDate");
                spot.setPeakEndDate(v != null && !v.toString().isEmpty() ? java.time.LocalDate.parse(v.toString()) : null);
            }
            if (payload.containsKey("freeStartDate")) {
                Object v = payload.get("freeStartDate");
                spot.setFreeStartDate(v != null && !v.toString().isEmpty() ? java.time.LocalDate.parse(v.toString()) : null);
            }
            if (payload.containsKey("freeEndDate")) {
                Object v = payload.get("freeEndDate");
                spot.setFreeEndDate(v != null && !v.toString().isEmpty() ? java.time.LocalDate.parse(v.toString()) : null);
            }
            
        ScenicSpot savedSpot = persistSpot(Objects.requireNonNull(spot));
        return ScenicSpotSimpleDTO.fromEntity(savedSpot);
    }

    @org.springframework.web.bind.annotation.GetMapping("/spots/{id}/tags")
    public org.springframework.http.ResponseEntity<?> getSpotTags(@org.springframework.web.bind.annotation.PathVariable @NonNull Long id) {
        try {
            return org.springframework.http.ResponseEntity.ok(spotTagService.getTagsForSpot(id));
        } catch (IllegalArgumentException ex) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }

    @org.springframework.web.bind.annotation.PutMapping("/spots/{id}/tags")
    public org.springframework.http.ResponseEntity<?> updateSpotTags(
            @org.springframework.web.bind.annotation.PathVariable @NonNull Long id,
            @org.springframework.web.bind.annotation.RequestBody(required = false) TagUpdateRequest request) {
        try {
            java.util.List<String> tags = spotTagService.updateSpotTags(id, request != null ? request.getTags() : null);
            return org.springframework.http.ResponseEntity.ok(tags);
        } catch (IllegalArgumentException ex) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }

    /**
     * 批量更新所有景点的点击量，以15000为基准，根据景点名称设置不同的差异
     */
    @org.springframework.web.bind.annotation.PostMapping("/spots/reset-visit-count")
    @Transactional
    public org.springframework.http.ResponseEntity<?> resetAllVisitCounts() {
        try {
            List<ScenicSpot> allSpots = scenicSpotRepository.findAll();
            int updatedCount = 0;
            
            // 定义景点名称与点击量差异的映射（以15000为基准）
            Map<String, Integer> visitCountMap = new java.util.HashMap<>();
            // 最热门景点
            visitCountMap.put("布达拉宫", 15000 + 5000);
            visitCountMap.put("大昭寺", 15000 + 4500);
            visitCountMap.put("纳木错", 15000 + 4000);
            visitCountMap.put("羊卓雍措", 15000 + 3800);
            visitCountMap.put("罗布林卡", 15000 + 3500);
            visitCountMap.put("巴松措", 15000 + 3200);
            visitCountMap.put("雅鲁藏布江大峡谷", 15000 + 3000);
            visitCountMap.put("哲蚌寺", 15000 + 2800);
            visitCountMap.put("色拉寺", 15000 + 2600);
            visitCountMap.put("羊八井地热温泉", 15000 + 2400);
            // 中等热门
            visitCountMap.put("珠穆朗玛峰", 15000 + 2200);
            visitCountMap.put("南迦巴瓦峰", 15000 + 2000);
            visitCountMap.put("鲁朗林海", 15000 + 1800);
            visitCountMap.put("然乌湖", 15000 + 1600);
            visitCountMap.put("扎什伦布寺", 15000 + 1500);
            visitCountMap.put("桑耶寺", 15000 + 1400);
            visitCountMap.put("米堆冰川", 15000 + 1300);
            visitCountMap.put("墨脱", 15000 + 1200);
            visitCountMap.put("雍布拉康", 15000 + 1100);
            visitCountMap.put("甘丹寺", 15000 + 1000);
            // 较冷门
            visitCountMap.put("冈仁波齐", 15000 + 900);
            visitCountMap.put("玛旁雍措", 15000 + 800);
            visitCountMap.put("卡若拉冰川", 15000 + 700);
            visitCountMap.put("来古冰川", 15000 + 600);
            visitCountMap.put("拉姆拉错", 15000 + 500);
            visitCountMap.put("萨迦寺", 15000 + 400);
            visitCountMap.put("古格王国遗址", 15000 + 300);
            visitCountMap.put("扎达土林", 15000 + 200);
            visitCountMap.put("纳木那尼峰", 15000 + 100);
            visitCountMap.put("当惹雍错", 15000 + 50);
            
            for (ScenicSpot spot : allSpots) {
                // 根据景点名称查找对应的点击量，如果找不到则使用15000
                Integer visitCount = visitCountMap.getOrDefault(spot.getName(), 15000);
                spot.setVisitCount(visitCount);
                scenicSpotRepository.save(spot);
                updatedCount++;
            }
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "成功更新 " + updatedCount + " 个景点的点击量（基准15000，根据景点设置差异）");
            response.put("updatedCount", updatedCount);
            return org.springframework.http.ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", "更新失败");
            errorResponse.put("message", e.getMessage());
            return org.springframework.http.ResponseEntity.status(500).body(errorResponse);
        }
    }

    @NonNull
    private ScenicSpot persistSpot(@NonNull ScenicSpot spot) {
            scenicSpotRepository.save(spot);
        return spot;
    }
}
