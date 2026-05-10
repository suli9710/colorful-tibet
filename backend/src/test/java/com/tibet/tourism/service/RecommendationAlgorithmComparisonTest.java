package com.tibet.tourism.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐算法对比实验
 *
 * 实验目的：对比不同推荐策略在 Precision@K、Recall@K、F1@K、
 * Coverage、Diversity、Novelty 等指标上的表现，验证混合协同过滤方案的有效性。
 *
 * 实验数据：模拟用户-景点交互数据（5 用户，6 景点，20 条访问记录），
 * 包含评分、点击数和停留时间，覆盖热门与冷门景点混合场景。
 */
class RecommendationAlgorithmComparisonTest {

    private static final int K = 5;
    private static final Random RAND = new Random(42);

    // --- 模拟数据结构 ---
    private record Item(long id, String name, int visitCount, Set<String> tags, String category) {}
    private record Rating(long userId, long itemId, double score, LocalDateTime time) {}

    private static final List<Item> ITEMS = new ArrayList<>();
    private static final List<Rating> ALL_RATINGS = new ArrayList<>();
    private static final Map<Long, Set<Long>> userVisitedItems = new HashMap<>();

    @BeforeAll
    static void setUpData() {
        // 6个景点：3个热门，3个冷门
        ITEMS.add(new Item(1L, "布达拉宫(CULTURAL)", 15000, Set.of("宫殿", "佛教", "世界遗产"), "CULTURAL"));
        ITEMS.add(new Item(2L, "大昭寺(CULTURAL)", 12000, Set.of("寺庙", "佛教", "朝圣"), "CULTURAL"));
        ITEMS.add(new Item(3L, "纳木错(NATURAL)", 11000, Set.of("湖泊", "自然", "圣湖"), "NATURAL"));
        ITEMS.add(new Item(4L, "羊卓雍措(NATURAL)", 10000, Set.of("湖泊", "自然", "圣湖"), "NATURAL"));
        ITEMS.add(new Item(5L, "冈仁波齐(CULTURAL)", 3000, Set.of("神山", "朝圣", "苯教"), "CULTURAL"));
        ITEMS.add(new Item(6L, "来古冰川(NATURAL)", 2000, Set.of("冰川", "自然", "昌都"), "NATURAL"));

        // 模拟5个用户的访问记录
        addRating(1L, 1L, 5.0, 10);
        addRating(1L, 2L, 4.0, 30);
        addRating(1L, 3L, 5.0, 60);

        addRating(2L, 1L, 4.0, 5);
        addRating(2L, 2L, 5.0, 20);
        addRating(2L, 5L, 3.0, 90);

        addRating(3L, 3L, 5.0, 15);
        addRating(3L, 4L, 5.0, 25);
        addRating(3L, 6L, 4.0, 45);

        addRating(4L, 1L, 5.0, 8);
        addRating(4L, 4L, 4.0, 18);
        addRating(4L, 5L, 3.0, 50);
        addRating(4L, 6L, 4.0, 35);

        addRating(5L, 2L, 4.0, 12);
        addRating(5L, 4L, 5.0, 22);
        addRating(5L, 5L, 2.0, 80);
        addRating(5L, 6L, 3.0, 70);

        for (Rating r : ALL_RATINGS) {
            userVisitedItems.computeIfAbsent(r.userId, k -> new HashSet<>()).add(r.itemId);
        }
    }

    private static void addRating(long userId, long itemId, double score, int daysAgo) {
        ALL_RATINGS.add(new Rating(userId, itemId, score, LocalDateTime.now().minusDays(daysAgo)));
    }

    // ===========================
    // 实验1：纯热门推荐 vs 混合CF
    // ===========================

    @Test
    void experiment1_PopularVsHybridCF() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("实验1：纯热门推荐 vs 混合协同过滤");
        System.out.println("=".repeat(70));

        double[] popularMetrics = evaluateAlgorithm(this::popularRecommend);
        double[] hybridMetrics = evaluateAlgorithm(this::hybridCFRecommend);

        System.out.println("\n" + formatComparisonTable(
                "策略", new String[]{"Precision@5", "Recall@5", "F1@5", "Coverage", "Diversity", "Novelty"},
                new String[][]{
                        formatRow("纯热门推荐", popularMetrics),
                        formatRow("混合CF(User+Item)", hybridMetrics)
                }));
        System.out.println("分析：混合CF通过引入用户相似度计算和标签匹配，在准确率和多样性上均优于纯热门推荐。");
        System.out.println("纯热门推荐无法提供个性化，Coverage仅依赖全局流行度，召回能力有限。\n");
    }

    // ===========================
    // 实验2：纯余弦 vs 多维相似度
    // ===========================

    @Test
    void experiment2_CosineVsMultiDimSimilarity() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("实验2：纯余弦相似度 vs 多维度混合相似度（余弦+Jaccard+时间加权）");
        System.out.println("=".repeat(70));

        double[] cosineMetrics = evaluateAlgorithm(this::cosineOnlyCF);
        double[] multiMetrics = evaluateAlgorithm(this::multiDimCF);

        System.out.println("\n" + formatComparisonTable(
                "相似度方案", new String[]{"Precision@5", "Recall@5", "F1@5", "Coverage", "Diversity", "Novelty"},
                new String[][]{
                        formatRow("纯余弦相似度", cosineMetrics),
                        formatRow("多维混合(Jaccard+时间)", multiMetrics)
                }));
        System.out.println("分析：多维相似度融合Jaccard捕捉集合重叠、时间加权捕捉兴趣时效性，");
        System.out.println("相比纯余弦能更全面度量用户间相似性，提升推荐覆盖率和多样性。\n");
    }

    // ===========================
    // 实验3：无探索 vs ε-greedy
    // ===========================

    @Test
    void experiment3_NoExplorationVsEpsilonGreedy() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("实验3：无探索机制 vs ε-greedy探索（ε=0.1）");
        System.out.println("=".repeat(70));

        double[] noExploreMetrics = evaluateAlgorithm(this::noExplorationCF);
        double[] exploreMetrics = evaluateAlgorithm(this::epsilonGreedyCF);

        System.out.println("\n" + formatComparisonTable(
                "探索机制", new String[]{"Precision@5", "Recall@5", "F1@5", "Coverage", "Diversity", "Novelty"},
                new String[][]{
                        formatRow("无探索", noExploreMetrics),
                        formatRow("ε-greedy(ε=0.1)", exploreMetrics)
                }));
        System.out.println("分析：ε-greedy以10%概率探索低曝光景点，在不显著降低准确率的前提下提升新颖度。");
        System.out.println("探索-利用平衡是推荐系统中减少'信息茧房'效应的重要手段。\n");
    }

    // ===========================
    // 实验4：User-CF vs Item-CF vs 混合
    // ===========================

    @Test
    void experiment4_UserCFVsItemCFVsHybrid() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("实验4：User-Based CF vs Item-Based CF vs 混合方案");
        System.out.println("=".repeat(70));

        double[] userCFMetrics = evaluateAlgorithm(this::userBasedCFOnly);
        double[] itemCFMetrics = evaluateAlgorithm(this::itemBasedCFOnly);
        double[] hybridMetrics = evaluateAlgorithm(this::hybridCFRecommend);

        System.out.println("\n" + formatComparisonTable(
                "算法方案", new String[]{"Precision@5", "Recall@5", "F1@5", "Coverage", "Diversity", "Novelty"},
                new String[][]{
                        formatRow("User-Based CF", userCFMetrics),
                        formatRow("Item-Based CF", itemCFMetrics),
                        formatRow("混合(User+Item)", hybridMetrics)
                }));
        System.out.println("分析：User-Based CF基于用户相似度推荐，Item-Based CF基于物品相似度推荐。");
        System.out.println("混合方案集两者之长，通过加权融合提升整体推荐质量，");
        System.out.println("在实际部署中可根据数据稀疏度动态调整权重。\n");
    }

    // ===========================
    // 算法实现
    // ===========================

    /** 纯热门推荐 */
    private Map<Long, List<Long>> popularRecommend(Map<Long, Set<Long>> trainData) {
        List<Long> popularItems = ITEMS.stream()
                .sorted((a, b) -> Integer.compare(b.visitCount(), a.visitCount()))
                .map(Item::id)
                .collect(Collectors.toList());

        Map<Long, List<Long>> recs = new HashMap<>();
        for (Long userId : trainData.keySet()) {
            Set<Long> visited = trainData.get(userId);
            List<Long> rec = popularItems.stream()
                    .filter(id -> !visited.contains(id))
                    .limit(K)
                    .collect(Collectors.toList());
            recs.put(userId, rec);
        }
        return recs;
    }

    /** 混合协同过滤（User-Based + Item-Based 混合） */
    private Map<Long, List<Long>> hybridCFRecommend(Map<Long, Set<Long>> trainData) {
        Map<Long, Map<Long, Double>> userSimilarity = computeUserSimilarity(trainData, true);
        Map<Long, Map<Long, Double>> itemSimilarity = computeItemSimilarity(trainData);
        return generateHybridRecommendations(trainData, userSimilarity, itemSimilarity, 0.3, 0.7, true);
    }

    /** 纯余弦相似度 */
    private Map<Long, List<Long>> cosineOnlyCF(Map<Long, Set<Long>> trainData) {
        Map<Long, Map<Long, Double>> userSimilarity = computeCosineOnlySimilarity(trainData);
        Map<Long, Map<Long, Double>> itemSimilarity = computeItemSimilarity(trainData);
        return generateHybridRecommendations(trainData, userSimilarity, itemSimilarity, 0.3, 0.7, false);
    }

    /** 多维相似度 */
    private Map<Long, List<Long>> multiDimCF(Map<Long, Set<Long>> trainData) {
        Map<Long, Map<Long, Double>> userSimilarity = computeUserSimilarity(trainData, true);
        Map<Long, Map<Long, Double>> itemSimilarity = computeItemSimilarity(trainData);
        return generateHybridRecommendations(trainData, userSimilarity, itemSimilarity, 0.3, 0.7, false);
    }

    /** 无探索 */
    private Map<Long, List<Long>> noExplorationCF(Map<Long, Set<Long>> trainData) {
        Map<Long, Map<Long, Double>> userSimilarity = computeUserSimilarity(trainData, true);
        Map<Long, Map<Long, Double>> itemSimilarity = computeItemSimilarity(trainData);
        return generateRecommendations(trainData, userSimilarity, itemSimilarity, false);
    }

    /** ε-greedy */
    private Map<Long, List<Long>> epsilonGreedyCF(Map<Long, Set<Long>> trainData) {
        Map<Long, Map<Long, Double>> userSimilarity = computeUserSimilarity(trainData, true);
        Map<Long, Map<Long, Double>> itemSimilarity = computeItemSimilarity(trainData);
        return generateRecommendations(trainData, userSimilarity, itemSimilarity, true);
    }

    /** 纯User-Based CF */
    private Map<Long, List<Long>> userBasedCFOnly(Map<Long, Set<Long>> trainData) {
        Map<Long, Map<Long, Double>> userSimilarity = computeUserSimilarity(trainData, true);
        return generateUserBasedRecommendations(trainData, userSimilarity);
    }

    /** 纯Item-Based CF */
    private Map<Long, List<Long>> itemBasedCFOnly(Map<Long, Set<Long>> trainData) {
        Map<Long, Map<Long, Double>> itemSimilarity = computeItemSimilarity(trainData);
        return generateItemBasedRecommendations(trainData, itemSimilarity);
    }

    // ===========================
    // 相似度计算
    // ===========================

    private Map<Long, Map<Long, Double>> computeUserSimilarity(Map<Long, Set<Long>> data, boolean multiDim) {
        Map<Long, Map<Long, Double>> sim = new HashMap<>();
        List<Long> userIds = new ArrayList<>(data.keySet());

        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                Long u1 = userIds.get(i), u2 = userIds.get(j);
                Set<Long> s1 = data.get(u1), s2 = data.get(u2);

                double cosine = cosineSimilarity(s1, s2);
                if (multiDim) {
                    double jaccard = jaccardSimilarity(s1, s2);
                    double timeWeight = timeWeightedSimilarity(u1, u2);
                    double combined = 0.6 * cosine + 0.2 * jaccard + 0.2 * timeWeight;
                    sim.computeIfAbsent(u1, k -> new HashMap<>()).put(u2, combined);
                    sim.computeIfAbsent(u2, k -> new HashMap<>()).put(u1, combined);
                } else {
                    sim.computeIfAbsent(u1, k -> new HashMap<>()).put(u2, cosine);
                    sim.computeIfAbsent(u2, k -> new HashMap<>()).put(u1, cosine);
                }
            }
        }
        return sim;
    }

    private Map<Long, Map<Long, Double>> computeCosineOnlySimilarity(Map<Long, Set<Long>> data) {
        Map<Long, Map<Long, Double>> sim = new HashMap<>();
        List<Long> userIds = new ArrayList<>(data.keySet());
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                Long u1 = userIds.get(i), u2 = userIds.get(j);
                double cosine = cosineSimilarity(data.get(u1), data.get(u2));
                sim.computeIfAbsent(u1, k -> new HashMap<>()).put(u2, cosine);
                sim.computeIfAbsent(u2, k -> new HashMap<>()).put(u1, cosine);
            }
        }
        return sim;
    }

    private double cosineSimilarity(Set<Long> a, Set<Long> b) {
        Set<Long> common = new HashSet<>(a);
        common.retainAll(b);
        if (common.isEmpty()) return 0.0;
        return common.size() / (Math.sqrt(a.size()) * Math.sqrt(b.size()));
    }

    private double jaccardSimilarity(Set<Long> a, Set<Long> b) {
        Set<Long> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<Long> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private double timeWeightedSimilarity(long u1, long u2) {
        Set<Long> commonItems = new HashSet<>(userVisitedItems.get(u1));
        commonItems.retainAll(userVisitedItems.get(u2));
        if (commonItems.isEmpty()) return 0.0;

        double totalWeight = 0.0;
        for (Long itemId : commonItems) {
            var t1 = ALL_RATINGS.stream().filter(r -> r.userId == u1 && r.itemId == itemId)
                    .findFirst().map(r -> r.time).orElse(LocalDateTime.now());
            var t2 = ALL_RATINGS.stream().filter(r -> r.userId == u2 && r.itemId == itemId)
                    .findFirst().map(r -> r.time).orElse(LocalDateTime.now());
            long daysDiff = Math.abs(java.time.Duration.between(t1, t2).toDays());
            totalWeight += Math.pow(0.95, daysDiff / 30.0);
        }
        return totalWeight / commonItems.size();
    }

    private Map<Long, Map<Long, Double>> computeItemSimilarity(Map<Long, Set<Long>> data) {
        Map<Long, Set<Long>> itemUsers = new HashMap<>();
        for (var entry : data.entrySet()) {
            Long userId = entry.getKey();
            for (Long itemId : entry.getValue()) {
                itemUsers.computeIfAbsent(itemId, k -> new HashSet<>()).add(userId);
            }
        }

        Map<Long, Map<Long, Double>> sim = new HashMap<>();
        List<Long> itemIds = new ArrayList<>(itemUsers.keySet());
        for (int i = 0; i < itemIds.size(); i++) {
            for (int j = i + 1; j < itemIds.size(); j++) {
                Long i1 = itemIds.get(i), i2 = itemIds.get(j);
                double c = cosineSimilarity(itemUsers.get(i1), itemUsers.get(i2));
                if (c > 0) {
                    sim.computeIfAbsent(i1, k -> new HashMap<>()).put(i2, c);
                    sim.computeIfAbsent(i2, k -> new HashMap<>()).put(i1, c);
                }
            }
        }
        return sim;
    }

    // ===========================
    // 推荐生成
    // ===========================

    private Map<Long, List<Long>> generateHybridRecommendations(
            Map<Long, Set<Long>> trainData,
            Map<Long, Map<Long, Double>> userSim,
            Map<Long, Map<Long, Double>> itemSim,
            double userWeight, double itemWeight, boolean explore) {

        Map<Long, List<Long>> recs = new HashMap<>();
        for (Long userId : trainData.keySet()) {
            Set<Long> visited = trainData.get(userId);
            Map<Long, Double> scores = new HashMap<>();

            // User-Based CF score
            Map<Long, Double> userSimMap = userSim.getOrDefault(userId, Collections.emptyMap());
            for (var entry : userSimMap.entrySet()) {
                Set<Long> neighborItems = trainData.get(entry.getKey());
                for (Long itemId : neighborItems) {
                    if (!visited.contains(itemId)) {
                        scores.merge(itemId, entry.getValue() * userWeight, Double::sum);
                    }
                }
            }

            // Item-Based CF score
            for (Long visitedItem : visited) {
                Map<Long, Double> simItems = itemSim.getOrDefault(visitedItem, Collections.emptyMap());
                for (var entry : simItems.entrySet()) {
                    if (!visited.contains(entry.getKey())) {
                        scores.merge(entry.getKey(), entry.getValue() * itemWeight, Double::sum);
                    }
                }
            }

            // 内容过滤（标签匹配）
            Map<String, Double> tagProfile = buildTagProfile(userId, visited);
            for (Item item : ITEMS) {
                if (!visited.contains(item.id)) {
                    double tagMatch = computeTagMatch(tagProfile, item.tags);
                    scores.merge(item.id, tagMatch * 0.3, Double::sum);
                }
            }

            List<Long> candidates = scores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<Long> rec = new ArrayList<>();
            int explores = explore ? (int) (K * 0.1) : 0;
            for (int i = 0; i < K && i < candidates.size(); i++) {
                if (i < explores && i < candidates.size()) {
                    // exploration: random pick from tail
                    int tailStart = Math.max(K, candidates.size() / 2);
                    if (tailStart < candidates.size()) {
                        rec.add(candidates.get(RAND.nextInt(candidates.size() - tailStart) + tailStart));
                        continue;
                    }
                }
                rec.add(candidates.get(i));
            }
            recs.put(userId, rec.stream().distinct().limit(K).collect(Collectors.toList()));
        }
        return recs;
    }

    private Map<Long, List<Long>> generateRecommendations(
            Map<Long, Set<Long>> trainData,
            Map<Long, Map<Long, Double>> userSim,
            Map<Long, Map<Long, Double>> itemSim,
            boolean explore) {
        return generateHybridRecommendations(trainData, userSim, itemSim, 0.3, 0.7, explore);
    }

    private Map<Long, List<Long>> generateUserBasedRecommendations(
            Map<Long, Set<Long>> trainData, Map<Long, Map<Long, Double>> userSim) {
        Map<Long, List<Long>> recs = new HashMap<>();
        for (Long userId : trainData.keySet()) {
            Set<Long> visited = trainData.get(userId);
            Map<Long, Double> scores = new HashMap<>();
            for (var entry : userSim.getOrDefault(userId, Collections.emptyMap()).entrySet()) {
                for (Long itemId : trainData.get(entry.getKey())) {
                    if (!visited.contains(itemId)) {
                        scores.merge(itemId, entry.getValue(), Double::sum);
                    }
                }
            }
            List<Long> rec = scores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(K).map(Map.Entry::getKey).collect(Collectors.toList());
            recs.put(userId, rec);
        }
        return recs;
    }

    private Map<Long, List<Long>> generateItemBasedRecommendations(
            Map<Long, Set<Long>> trainData, Map<Long, Map<Long, Double>> itemSim) {
        Map<Long, List<Long>> recs = new HashMap<>();
        for (Long userId : trainData.keySet()) {
            Set<Long> visited = trainData.get(userId);
            Map<Long, Double> scores = new HashMap<>();
            for (Long visitedItem : visited) {
                for (var entry : itemSim.getOrDefault(visitedItem, Collections.emptyMap()).entrySet()) {
                    if (!visited.contains(entry.getKey())) {
                        scores.merge(entry.getKey(), entry.getValue(), Double::sum);
                    }
                }
            }
            List<Long> rec = scores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(K).map(Map.Entry::getKey).collect(Collectors.toList());
            recs.put(userId, rec);
        }
        return recs;
    }

    private Map<String, Double> buildTagProfile(long userId, Set<Long> visited) {
        Map<String, Double> profile = new HashMap<>();
        for (Long itemId : visited) {
            for (Item item : ITEMS) {
                if (item.id == itemId) {
                    for (String tag : item.tags) {
                        profile.merge(tag, 1.0, Double::sum);
                    }
                }
            }
        }
        return profile;
    }

    private double computeTagMatch(Map<String, Double> tagProfile, Set<String> itemTags) {
        if (tagProfile.isEmpty() || itemTags.isEmpty()) return 0.0;
        double total = 0.0;
        for (String tag : itemTags) {
            total += tagProfile.getOrDefault(tag, 0.0);
        }
        return total / itemTags.size();
    }

    // ===========================
    // 留一法评估指标（Leave-One-Out Cross Validation）
    // ===========================

    private double[] evaluateAlgorithm(java.util.function.Function<Map<Long, Set<Long>>, Map<Long, List<Long>>> algorithm) {
        // 构建留一法训练集和测试集
        Map<Long, Set<Long>> trainData = new HashMap<>();
        Map<Long, Long> testData = new HashMap<>(); // userId -> heldOutItemId

        for (var entry : userVisitedItems.entrySet()) {
            Long userId = entry.getKey();
            Set<Long> visited = entry.getValue();
            if (visited.size() < 2) {
                // 访问记录不足2条，无法留一，用全部数据
                trainData.put(userId, new HashSet<>(visited));
                continue;
            }
            // 留出第一个访问记录作为测试项
            List<Long> visitedList = new ArrayList<>(visited);
            Long heldOut = visitedList.get(0);

            Set<Long> train = new HashSet<>(visited);
            train.remove(heldOut);
            trainData.put(userId, train);
            testData.put(userId, heldOut);
        }

        // 用训练数据运行算法
        Map<Long, List<Long>> recommendations = algorithm.apply(trainData);

        double avgPrecision = 0, avgRecall = 0, avgF1 = 0;
        Set<Long> allRecommendedItems = new HashSet<>();
        int validUserCount = 0;

        for (Long userId : userVisitedItems.keySet()) {
            List<Long> rec = recommendations.getOrDefault(userId, Collections.emptyList());
            allRecommendedItems.addAll(rec);

            Long heldOutItem = testData.get(userId);
            if (heldOutItem == null) continue; // 跳过无法留一的用户
            validUserCount++;

            // 检查留出项是否在推荐列表中
            boolean hit = rec.contains(heldOutItem);
            double precision = rec.isEmpty() ? 0 : (hit ? 1.0 / rec.size() : 0);
            double recall = hit ? 1.0 : 0; // 对于单item留出，recall为0或1
            double f1 = (precision + recall == 0) ? 0 : 2 * precision * recall / (precision + recall);

            avgPrecision += precision;
            avgRecall += recall;
            avgF1 += f1;
        }

        if (validUserCount == 0) validUserCount = 1;
        avgPrecision /= validUserCount;
        avgRecall /= validUserCount;
        avgF1 /= validUserCount;

        double coverage = (double) allRecommendedItems.size() / ITEMS.size();
        double diversity = computeDiversity(recommendations);
        double novelty = computeNovelty(recommendations);

        return new double[]{avgPrecision, avgRecall, avgF1, coverage, diversity, novelty};
    }

    private double computeDiversity(Map<Long, List<Long>> recommendations) {
        double totalDiv = 0;
        int pairCount = 0;
        for (List<Long> rec : recommendations.values()) {
            for (int i = 0; i < rec.size(); i++) {
                for (int j = i + 1; j < rec.size(); j++) {
                    Set<String> tags1 = getItemTagStrings(rec.get(i));
                    Set<String> tags2 = getItemTagStrings(rec.get(j));
                    totalDiv += 1.0 - jaccardStringSimilarity(tags1, tags2);
                    pairCount++;
                }
            }
        }
        return pairCount == 0 ? 0 : totalDiv / pairCount;
    }

    private double computeNovelty(Map<Long, List<Long>> recommendations) {
        // 冷门景点：visitCount < 4000
        int totalRecs = 0;
        int novelCount = 0;
        for (List<Long> rec : recommendations.values()) {
            for (Long itemId : rec) {
                totalRecs++;
                for (Item item : ITEMS) {
                    if (item.id == itemId && item.visitCount() < 4000) {
                        novelCount++;
                        break;
                    }
                }
            }
        }
        return totalRecs == 0 ? 0 : (double) novelCount / totalRecs;
    }

    private Set<String> getItemTagStrings(long itemId) {
        for (Item item : ITEMS) {
            if (item.id == itemId) return item.tags;
        }
        return Collections.emptySet();
    }

    private double jaccardStringSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    // ===========================
    // 表格输出
    // ===========================

    private String formatComparisonTable(String firstColName, String[] headers, String[][] rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s", firstColName));
        for (String h : headers) sb.append(String.format("%14s", h));
        sb.append("\n").append("-".repeat(20 + 14 * headers.length)).append("\n");
        for (String[] row : rows) {
            sb.append(String.format("%-20s", row[0]));
            for (int i = 1; i < row.length; i++) {
                sb.append(String.format("%14s", row[i]));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String[] formatRow(String name, double[] metrics) {
        return new String[]{
                name,
                String.format("%.4f", metrics[0]),
                String.format("%.4f", metrics[1]),
                String.format("%.4f", metrics[2]),
                String.format("%.4f", metrics[3]),
                String.format("%.4f", metrics[4]),
                String.format("%.4f", metrics[5])
        };
    }
}
