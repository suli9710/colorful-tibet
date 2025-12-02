# 网页爬虫价格提取使用指南

## 功能概述

网页爬虫策略通过正则表达式从网页HTML内容中提取景点门票价格。支持多种价格格式和多个数据源。

## 工作原理

### 1. 搜索URL构建

系统会构建多个搜索URL来提高成功率：

```java
// 示例URL
https://www.baidu.com/s?wd=布达拉宫 门票价格
https://www.baidu.com/s?wd=布达拉宫 门票多少钱
https://www.baidu.com/s?wd=布达拉宫 拉萨 门票
```

### 2. HTML内容获取

使用 `WebClient` 发送HTTP请求获取网页内容：

```java
String htmlContent = webClientBuilder.build()
    .get()
    .uri(searchUrl)
    .header("User-Agent", "Mozilla/5.0...")
    .retrieve()
    .bodyToMono(String.class)
    .timeout(Duration.ofSeconds(15))
    .block();
```

### 3. 正则表达式提取

系统使用多个正则表达式模式按优先级提取价格：

#### 模式1：门票价格（最精确）
```regex
门票[：:]*\s*(?:价格|票价|费用)[：:]*\s*(\d+(?:\.\d+)?)\s*(?:元|块|¥)
```
**匹配示例**：
- "门票价格：200元"
- "门票: 150.5元"
- "票价：¥200"

#### 模式2：直接价格格式
```regex
(?:门票|票价|价格)[：:]*\s*(\d+(?:\.\d+)?)\s*(?:元|块|¥)
```
**匹配示例**：
- "门票200元"
- "票价：150块"

#### 模式3：旺季/淡季价格
```regex
(?:旺季|淡季)[：:]*\s*(\d+(?:\.\d+)?)\s*(?:元|块|¥)
```
**匹配示例**：
- "旺季：250元"
- "淡季150元"

#### 模式4：成人票/学生票价格
```regex
(?:成人票|学生票|儿童票)[：:]*\s*(\d+(?:\.\d+)?)\s*(?:元|块|¥)
```
**匹配示例**：
- "成人票：200元"
- "学生票100元"

#### 模式5：景点名称附近的价格
```regex
布达拉宫.*?(\d+(?:\.\d+)?)\s*(?:元|块|¥)
```
**匹配示例**：
- "布达拉宫门票200元"

#### 模式6：通用价格格式（最后尝试）
```regex
(\d+(?:\.\d+)?)\s*(?:元|块|¥)(?:/人|/张)?
```
**匹配示例**：
- "200元"
- "150.5元/人"

### 4. 价格验证

提取的价格会经过合理性验证：

```java
private boolean isValidPrice(BigDecimal price) {
    double value = price.doubleValue();
    // 门票价格通常在 10-2000 元之间
    return value >= 10 && value <= 2000 && 
           value == price.intValue() || (value * 10) == (int)(value * 10);
}
```

**过滤规则**：
- 价格范围：10-2000元
- 过滤年份（如2024、2025）
- 过滤日期（如1-31）
- 过滤电话号码

### 5. 最佳价格选择

从多个提取的价格中选择最佳值：

1. **频率统计**：统计每个价格（四舍五入到10元）的出现次数
2. **选择高频价格**：选择出现频率最高的价格
3. **中位数备选**：如果所有价格只出现一次，选择中位数

```java
// 示例：提取到的价格
[200, 200, 200, 150, 250, 200, 200]
// 统计：200出现5次，150出现1次，250出现1次
// 选择：200元（出现频率最高）
```

### 6. 置信度计算

```java
置信度 = (选中价格出现次数 / 总价格数量) * 0.8
```

**置信度范围**：0.5 - 0.8

## 使用示例

### 示例1：提取单个景点价格

```java
@Autowired
private PriceFetchService priceFetchService;

@Autowired
private ScenicSpotRepository scenicSpotRepository;

public void testPriceExtraction() {
    ScenicSpot spot = scenicSpotRepository.findById(1L).orElse(null);
    if (spot != null) {
        PriceInfo priceInfo = priceFetchService.fetchPrice(spot);
        if (priceInfo != null) {
            System.out.println("价格: " + priceInfo.getBasePrice());
            System.out.println("来源: " + priceInfo.getSource());
            System.out.println("置信度: " + priceInfo.getConfidence());
        }
    }
}
```

### 示例2：通过API调用

```bash
# 查询价格（会尝试网页爬虫策略）
GET http://localhost:8080/api/prices/fetch/1

# 响应示例
{
  "success": true,
  "priceInfo": {
    "basePrice": 200.00,
    "source": "网页爬虫",
    "fetchTime": "2025-01-15T10:30:00",
    "confidence": 0.75
  },
  "currentPrice": 200.00
}
```

## 自定义正则表达式

如果需要添加自定义价格提取模式，可以修改 `extractPrices` 方法：

```java
// 添加新的价格模式
Pattern customPattern = Pattern.compile("你的正则表达式");
Matcher matcher = customPattern.matcher(textContent);
while (matcher.find()) {
    String priceStr = matcher.group(1);
    BigDecimal price = new BigDecimal(priceStr);
    if (isValidPrice(price)) {
        uniquePrices.add(price);
    }
}
```

## 常见价格格式支持

系统支持以下价格格式：

| 格式 | 示例 | 是否支持 |
|------|------|---------|
| 整数价格 | 200元 | ✅ |
| 小数价格 | 150.5元 | ✅ |
| 带符号 | ¥200 | ✅ |
| 带单位 | 200元/人 | ✅ |
| 旺季/淡季 | 旺季：250元 | ✅ |
| 票种区分 | 成人票：200元 | ✅ |
| 价格范围 | 100-200元 | ⚠️ 取第一个值 |

## 注意事项

### 1. 遵守网站规则
- 遵守目标网站的 `robots.txt`
- 遵守网站的使用条款
- 不要过于频繁地请求

### 2. 请求头设置
系统已设置合理的请求头：
```java
.header("User-Agent", "Mozilla/5.0...")
.header("Accept", "text/html...")
.header("Accept-Language", "zh-CN,zh;q=0.9...")
```

### 3. 超时设置
- 默认超时：15秒
- 如果网站响应慢，可以调整超时时间

### 4. 错误处理
- 网络错误：自动跳过，尝试下一个URL
- 解析错误：记录日志，不影响其他策略

### 5. 价格准确性
- 网页爬虫的置信度通常为 0.5-0.8
- 建议人工审核首次提取的价格
- 可以结合多个数据源提高准确性

## 性能优化建议

### 1. 并发请求
可以并行请求多个URL：

```java
List<Mono<String>> requests = searchUrls.stream()
    .map(url -> webClientBuilder.build()
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(String.class))
    .collect(Collectors.toList());

Flux<String> responses = Flux.merge(requests);
```

### 2. 缓存结果
可以缓存HTML内容，避免重复请求：

```java
@Cacheable(value = "priceCache", key = "#spot.id")
public PriceInfo fetchPrice(ScenicSpot spot) {
    // ...
}
```

### 3. 使用Jsoup（可选）
如果需要更精确的HTML解析，可以添加Jsoup依赖：

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>
```

然后使用Jsoup解析HTML：

```java
Document doc = Jsoup.parse(htmlContent);
Elements priceElements = doc.select("span.price, .ticket-price");
// 更精确地提取价格
```

## 故障排查

### 问题1：无法提取价格
**可能原因**：
- 网络连接问题
- 网站结构变化
- 正则表达式不匹配

**解决方案**：
1. 检查网络连接
2. 查看日志中的错误信息
3. 更新正则表达式模式

### 问题2：提取的价格不准确
**可能原因**：
- 网页中有多个价格
- 价格格式特殊

**解决方案**：
1. 检查置信度值
2. 人工审核并手动修正
3. 添加更精确的正则表达式

### 问题3：请求被拒绝
**可能原因**：
- User-Agent被识别为爬虫
- 请求频率过高

**解决方案**：
1. 更新User-Agent
2. 添加请求延迟
3. 使用代理IP

## 扩展开发

### 添加新的数据源

```java
// 在 buildSearchUrls 方法中添加
urls.add("https://www.mafengwo.com/search/q.php?q=" + encodeUrl(spot.getName()));
urls.add("https://www.qunar.com/sight/search.jsp?keyword=" + encodeUrl(spot.getName()));
```

### 添加价格历史记录

```java
// 保存价格历史
PriceHistory history = new PriceHistory();
history.setSpotId(spot.getId());
history.setPrice(priceInfo.getBasePrice());
history.setSource(priceInfo.getSource());
history.setFetchTime(LocalDateTime.now());
priceHistoryRepository.save(history);
```

## 总结

网页爬虫策略通过以下步骤提取价格：

1. ✅ 构建多个搜索URL
2. ✅ 获取HTML内容
3. ✅ 使用正则表达式提取价格
4. ✅ 验证价格合理性
5. ✅ 选择最佳价格
6. ✅ 计算置信度

该策略适合作为AI提取策略的备选方案，可以提高价格获取的成功率。















