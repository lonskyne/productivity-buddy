package org.example.productivitybuddy.util;

public class TimeFormatter {
    public static String formatTime(long timeMillis) {
        long totalSeconds = timeMillis / 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %2dm %2ds", hours, minutes, seconds);
        } else
        if (minutes > 0){
            return String.format("%dm %2ds", minutes, seconds);
        }
        else {
            return String.format("%2ds", seconds);
        }
    }
}
