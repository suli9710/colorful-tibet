# 智能价格获取服务使用指南

## 功能概述

智能价格获取服务提供了多种策略来自动获取景点门票价格，包括：

1. **AI提取策略**：使用AI从网页内容中智能提取价格信息
2. **网页爬虫策略**：通过网页爬虫提取价格数据
3. **第三方API策略**：集成携程、去哪儿等第三方API（需配置）

## API接口

### 1. 获取价格信息（不更新数据库）

```http
GET /api/prices/fetch/{spotId}
```

**说明**：仅查询价格信息，不更新数据库中的价格。

**响应示例**：
```json
{
  "success": true,
  "priceInfo": {
    "basePrice": 200.00,
    "peakSeasonPrice": 250.00,
    "offSeasonPrice": 150.00,
    "source": "AI提取",
    "fetchTime": "2025-01-15T10:30:00",
    "confidence": 0.85
  },
  "currentPrice": 200.00
}
```

### 2. 更新单个景点价格

```http
POST /api/prices/update/{spotId}?force=false
```

**参数**：
- `spotId`：景点ID
- `force`：是否强制更新（即使已有价格也更新），默认 `false`

**权限**：需要管理员权限（`ADMIN`角色）

**响应示例**：
```json
{
  "success": true,
  "message": "价格更新成功",
  "priceInfo": {
    "basePrice": 200.00,
    "source": "AI提取",
    "confidence": 0.85
  }
}
```

### 3. 批量更新所有景点价格

```http
POST /api/prices/batch-update?force=false
```

**参数**：
- `force`：是否强制更新，默认 `false`

**权限**：需要管理员权限（`ADMIN`角色）

**响应示例**：
```json
{
  "successCount": 15,
  "failCount": 2,
  "skipCount": 3,
  "totalCount": 20
}
```

## 配置说明

### AI提取策略配置

在 `application.yml` 中已配置豆包API：

```yaml
doubao:
  api:
    url: https://ark.cn-beijing.volces.com/api/v3/chat/completions
    key: your-api-key
    model: your-model-name
```

### 第三方API配置（可选）

如需使用携程、去哪儿等第三方API，可以在 `PriceFetchService.ThirdPartyApiStrategy` 中实现：

```java
// 在 ThirdPartyApiStrategy.fetch() 方法中添加
String apiUrl = "https://openapi.ctrip.com/scenic/price?name=" + spot.getName();
String apiKey = "your-api-key";
// 调用API并解析响应
```

## 使用建议

1. **首次使用**：建议先使用 `GET /api/prices/fetch/{spotId}` 测试价格获取功能
2. **批量更新**：使用 `POST /api/prices/batch-update` 定期更新所有景点价格
3. **定时任务**：可以配置Spring定时任务，定期自动更新价格：

```java
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
public void scheduledPriceUpdate() {
    priceUpdateService.batchUpdatePrices(false);
}
```

## 策略优先级

系统会按以下顺序尝试获取价格：

1. **AI提取策略**（优先级最高）
2. **网页爬虫策略**
3. **第三方API策略**

如果某个策略失败，会自动尝试下一个策略。

## 注意事项

1. **网页爬虫**：请遵守目标网站的 `robots.txt` 和使用条款
2. **API限制**：注意第三方API的调用频率限制
3. **价格准确性**：建议人工审核自动获取的价格，特别是首次获取的价格
4. **法律合规**：确保价格获取方式符合相关法律法规

## 扩展开发

### 添加新的价格获取策略

1. 实现 `PriceFetchStrategy` 接口
2. 在 `PriceFetchService.fetchPrice()` 方法中添加新策略到策略列表

示例：

```java
static class CustomStrategy implements PriceFetchStrategy {
    @Override
    public PriceInfo fetch(ScenicSpot spot) {
        // 实现自定义价格获取逻辑
        return new PriceInfo(price, "自定义来源");
    }
}
```

## 故障排查

1. **无法获取价格**：
   - 检查网络连接
   - 检查AI API配置是否正确
   - 查看日志中的错误信息

2. **价格不准确**：
   - 检查置信度（confidence）值
   - 人工审核并手动修正
   - 考虑调整AI提示词

3. **权限错误**：
   - 确保用户具有 `ADMIN` 角色
   - 检查JWT token是否有效
















