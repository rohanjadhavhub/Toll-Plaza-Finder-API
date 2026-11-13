package com.tollfinder.routing;

import com.tollfinder.domain.GeoPoint;
import java.util.ArrayList;
import java.util.List;

public class PolylineDecoder {
    public List<GeoPoint> decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) throw new IllegalArgumentException("Empty route polyline");
        var points = new ArrayList<GeoPoint>();
        int[] position = {0};
        long latitude = 0;
        long longitude = 0;
        while (position[0] < encoded.length()) {
            latitude += coordinate(encoded, position);
            longitude += coordinate(encoded, position);
            points.add(new GeoPoint(latitude / 100000.0, longitude / 100000.0));
        }
        if (points.size() < 2) throw new IllegalArgumentException("Incomplete route polyline");
        return List.copyOf(points);
    }

    private long coordinate(String encoded, int[] position) {
        long result = 0;
        for (int shift = 0; shift <= 30; shift += 5) {
            if (position[0] >= encoded.length()) throw new IllegalArgumentException("Truncated route polyline");
            int value = encoded.charAt(position[0]++) - 63;
            if (value < 0 || value > 63) throw new IllegalArgumentException("Invalid route polyline character");
            result |= (long) (value & 31) << shift;
            if (value < 32) return (result & 1) == 0 ? result >> 1 : ~(result >> 1);
        }
        throw new IllegalArgumentException("Route polyline coordinate overflow");
    }
}
