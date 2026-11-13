package com.tollfinder.spatial;

import com.tollfinder.domain.DrivingRoute;
import java.util.LinkedHashSet;
import java.util.Set;

public record GridCell(int longitude, int latitude) {
    public static Set<GridCell> covering(DrivingRoute route, double corridorMeters) {
        Set<GridCell> cells = new LinkedHashSet<>();
        for (int i = 1; i < route.points().size(); i++) {
            var start = route.points().get(i - 1);
            var end = route.points().get(i);
            double maxLatitude = Math.max(Math.abs(start.latitude()), Math.abs(end.latitude()));
            if (maxLatitude > 85 || Math.abs(start.longitude() - end.longitude()) > 180) {
                throw new IllegalArgumentException("Unsupported route extent");
            }
            // Conservative margins keep candidates near cell boundaries in the search.
            double latitudeMargin = corridorMeters / 100000.0 + 0.00001;
            double longitudeMargin = latitudeMargin / Math.cos(Math.toRadians(maxLatitude + latitudeMargin));
            int minX = Math.max(-180, (int) Math.floor(Math.min(start.longitude(), end.longitude()) - longitudeMargin));
            int maxX = Math.min(179, (int) Math.floor(Math.max(start.longitude(), end.longitude()) + longitudeMargin));
            int minY = Math.max(-90, (int) Math.floor(Math.min(start.latitude(), end.latitude()) - latitudeMargin));
            int maxY = Math.min(89, (int) Math.floor(Math.max(start.latitude(), end.latitude()) + latitudeMargin));
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    cells.add(new GridCell(x, y));
                }
            }
        }
        return Set.copyOf(cells);
    }
}
