package org.example.productivitybuddy.model;

import org.example.productivitybuddy.util.TimeFormatter;

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
        return TimeFormatter.formatTime(timeMillis);
    }
}
