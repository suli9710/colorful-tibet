package com.tibet.tourism.security.antibot;

import java.util.List;

public class BehaviorData {

    private List<MousePoint> mousePoints;
    private List<ClickEvent> clicks;
    private List<Long> keyIntervals;

    public record MousePoint(double x, double y, long t) {}
    public record ClickEvent(double x, double y, long t) {}

    public List<MousePoint> getMousePoints() { return mousePoints; }
    public void setMousePoints(List<MousePoint> mousePoints) {
        this.mousePoints = mousePoints != null && mousePoints.size() > 200
                ? mousePoints.subList(mousePoints.size() - 200, mousePoints.size())
                : mousePoints;
    }
    public List<ClickEvent> getClicks() { return clicks; }
    public void setClicks(List<ClickEvent> clicks) {
        this.clicks = clicks != null && clicks.size() > 50
                ? clicks.subList(clicks.size() - 50, clicks.size())
                : clicks;
    }
    public List<Long> getKeyIntervals() { return keyIntervals; }
    public void setKeyIntervals(List<Long> keyIntervals) {
        this.keyIntervals = keyIntervals != null && keyIntervals.size() > 200
                ? keyIntervals.subList(keyIntervals.size() - 200, keyIntervals.size())
                : keyIntervals;
    }
}
