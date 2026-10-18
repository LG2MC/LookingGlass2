package io.github.lg2mc.lookingglass2.api.client;

/** Recent render-thread elapsed times, including driver waits; not GPU timer queries. */
public record CameraStatistics(double actualFps, double averageMillis, double maximumMillis, int samples) {}
