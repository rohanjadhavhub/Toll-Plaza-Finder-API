package com.tollfinder.domain;

import java.util.List;
import java.util.stream.Collectors;

public record DrivingRoute(List<GeoPoint> points, long distanceMeters) {
    public DrivingRoute {
        points = List.copyOf(points);
        if (points.size() < 2 || points.stream().distinct().count() < 2 || distanceMeters <= 0) {
            throw new IllegalArgumentException("Route must have a positive distance and distinct points");
        }
    }

    public String toWkt() {
        return points.stream().map(point -> point.longitude() + " " + point.latitude())
                .collect(Collectors.joining(",", "LINESTRING(", ")"));
    }
}
