package com.tibet.tourism.common.security.antibot;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BehaviorAnalysisService {

    private final AntibotProperties properties;

    public BehaviorAnalysisService(AntibotProperties properties) {
        this.properties = properties;
    }

    public BehaviorMetrics analyze(BehaviorData data) {
        AntibotProperties.Behavior cfg = properties.getBehavior();
        if (!properties.isEnabled() || !cfg.isEnabled()) {
            return new BehaviorMetrics(0, 0, 0, 0, 0, 0);
        }
        if (data == null) {
            return new BehaviorMetrics(0, 0, 0, 0, 0, 50);
        }

        int risk = 0;
        double speedMean = 0, speedStdDev = 0, straightness = 0, intervalStdDev = 0, keyIntStdDev = 0;

        List<BehaviorData.MousePoint> points = data.getMousePoints();
        if (points != null && points.size() >= 3) {
            double[] speeds = new double[points.size() - 1];
            double[] intervals = new double[points.size() - 1];
            double pathLength = 0;

            for (int i = 1; i < points.size(); i++) {
                BehaviorData.MousePoint prev = points.get(i - 1);
                BehaviorData.MousePoint curr = points.get(i);
                double dx = curr.x() - prev.x();
                double dy = curr.y() - prev.y();
                double dist = Math.sqrt(dx * dx + dy * dy);
                double dt = Math.max(curr.t() - prev.t(), 1);
                speeds[i - 1] = dist / dt;
                intervals[i - 1] = dt;
                pathLength += dist;
            }

            speedMean = mean(speeds);
            speedStdDev = stdDev(speeds, speedMean);
            intervalStdDev = stdDev(intervals, mean(intervals));

            BehaviorData.MousePoint first = points.get(0);
            BehaviorData.MousePoint last = points.get(points.size() - 1);
            double directDistance = Math.sqrt(
                    Math.pow(last.x() - first.x(), 2) + Math.pow(last.y() - first.y(), 2));
            straightness = pathLength > 0 ? directDistance / pathLength : 0;

            if (speedStdDev < cfg.getMinSpeedStdDev()) risk += 30;
            if (straightness > cfg.getMaxStraightnessRatio()) risk += 25;
            if (intervalStdDev < cfg.getMinIntervalStdDev()) risk += 25;
        } else {
            risk += 50;
        }

        List<Long> keyIntervals = data.getKeyIntervals();
        if (keyIntervals != null && keyIntervals.size() >= 3) {
            double[] ki = keyIntervals.stream().mapToDouble(Long::doubleValue).toArray();
            keyIntStdDev = stdDev(ki, mean(ki));
            if (keyIntStdDev < cfg.getMinIntervalStdDev()) risk += 20;
        }

        return new BehaviorMetrics(speedMean, speedStdDev, straightness, intervalStdDev, keyIntStdDev,
                Math.min(risk, 100));
    }

    public record BehaviorMetrics(
            double mouseSpeedMean,
            double mouseSpeedStdDev,
            double straightnessRatio,
            double mouseIntervalStdDev,
            double keyIntervalStdDev,
            int risk
    ) {}

    private static double mean(double[] values) {
        if (values.length == 0) return 0;
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private static double stdDev(double[] values, double mean) {
        if (values.length < 2) return 0;
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / (values.length - 1));
    }
}
