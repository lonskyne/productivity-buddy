package org.example.productivitybuddy.model;

public class CategoryStats {
    private final ProcessCategory category;
    private final long timeMillis;

    public CategoryStats(ProcessCategory category, long timeMillis) {
        this.category = category;
        this.timeMillis = timeMillis;
    }

    public ProcessCategory getCategory() {
        return category;
    }

    public String getTimeFormatted() {
        long totalSeconds = timeMillis / 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        } else
        if (minutes > 0){
            return String.format("%dm %02ds", minutes, seconds);
        }
        else {
            return String.format("%02ds", seconds);
        }
    }
}
