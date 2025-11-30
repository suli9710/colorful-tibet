package com.tibet.tourism.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 价格定时更新服务
 * 定期自动从网络爬取并更新景点价格
 */
@Service
public class PriceScheduledService {

    private static final Logger logger = LoggerFactory.getLogger(PriceScheduledService.class);

    @Autowired
    private PriceUpdateService priceUpdateService;

    @Value("${price.update.enabled:true}")
    private boolean priceUpdateEnabled;

    @Value("${price.update.force:false}")
    private boolean forceUpdate;

    /**
     * 每天凌晨2点自动更新所有景点价格
     * Cron表达式: 秒 分 时 日 月 周
     * 0 0 2 * * ? 表示每天凌晨2点执行
     */
    @Scheduled(cron = "${price.update.cron:0 0 2 * * ?}")
    public void scheduledPriceUpdate() {
        if (!priceUpdateEnabled) {
            logger.info("价格定时更新已禁用，跳过本次更新");
            return;
        }

        logger.info("开始执行定时价格更新任务...");
        try {
            PriceUpdateService.BatchUpdateResult result = priceUpdateService.batchUpdatePrices(forceUpdate);
            
            logger.info("价格更新任务完成 - 成功: {}, 失败: {}, 跳过: {}, 总计: {}", 
                result.getSuccessCount(), 
                result.getFailCount(), 
                result.getSkipCount(), 
                result.getTotalCount());
        } catch (Exception e) {
            logger.error("价格更新任务执行失败", e);
        }
    }

    /**
     * 每周一凌晨3点强制更新所有景点价格（包括已有价格的景点）
     * 用于确保价格数据的准确性
     */
    @Scheduled(cron = "${price.update.weekly.cron:0 0 3 ? * MON}")
    public void weeklyForcePriceUpdate() {
        if (!priceUpdateEnabled) {
            logger.info("价格定时更新已禁用，跳过本次强制更新");
            return;
        }

        logger.info("开始执行每周强制价格更新任务...");
        try {
            PriceUpdateService.BatchUpdateResult result = priceUpdateService.batchUpdatePrices(true);
            
            logger.info("每周强制价格更新任务完成 - 成功: {}, 失败: {}, 跳过: {}, 总计: {}", 
                result.getSuccessCount(), 
                result.getFailCount(), 
                result.getSkipCount(), 
                result.getTotalCount());
        } catch (Exception e) {
            logger.error("每周强制价格更新任务执行失败", e);
        }
    }

    /**
     * 每小时更新一次没有价格的景点（仅更新价格为0或null的景点）
     * 用于快速填充缺失的价格数据
     */
    @Scheduled(cron = "${price.update.hourly.cron:0 0 * * * ?}")
    public void hourlyPriceUpdateForMissing() {
        if (!priceUpdateEnabled) {
            return;
        }

        logger.debug("开始执行每小时价格更新任务（仅更新缺失价格的景点）...");
        try {
            // 只更新没有价格的景点（force=false，且只更新价格为0或null的）
            PriceUpdateService.BatchUpdateResult result = priceUpdateService.batchUpdatePrices(false);
            
            if (result.getSuccessCount() > 0) {
                logger.info("每小时价格更新 - 成功更新 {} 个景点的价格", result.getSuccessCount());
            }
        } catch (Exception e) {
            logger.error("每小时价格更新任务执行失败", e);
        }
    }
}













