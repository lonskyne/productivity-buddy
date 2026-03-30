package org.example.productivitybuddy.services;

import org.example.productivitybuddy.model.AnalyticsSnapshot;

import java.time.LocalTime;

public interface SnapshotListener {
    void onSnapshot(AnalyticsSnapshot snapshot, LocalTime time);
}