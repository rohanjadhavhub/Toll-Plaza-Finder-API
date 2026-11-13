import com.tollfinder.cache.PlazaCandidateCache;
import com.tollfinder.domain.*;
import com.tollfinder.routing.PolylineDecoder;
import com.tollfinder.spatial.GridCell;
import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Dependency-free checks for environments where Maven cannot download dependencies.
public class CoreSmokeTest {
    public static void main(String[] args) {
        var decoder = new PolylineDecoder();
        var points = decoder.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
        require(points.equals(List.of(new GeoPoint(38.5, -120.2), new GeoPoint(40.7, -120.95),
                new GeoPoint(43.252, -126.453))), "Google polyline decoding");
        for (String encoded : new String[]{"", "_", "~~~~~~~", "?", "\u0000"}) {
            expectInvalid(() -> decoder.decode(encoded));
        }
        expectInvalid(() -> new Pincode("000001"));
        expectInvalid(() -> new GeoPoint(Double.NaN, 77));
        var route = new DrivingRoute(List.of(new GeoPoint(28.9999, 77.9999), new GeoPoint(28.9999, 79.2)), 120000);
        var cells = GridCell.covering(route, 50);
        require(cells.containsAll(List.of(new GridCell(77, 28), new GridCell(78, 28), new GridCell(79, 29))),
                "Corridor candidate coverage");
        require(cells.equals(GridCell.covering(new DrivingRoute(route.points().reversed(), 120000), 50)),
                "Reverse route cell reuse");
        require(route.toWkt().startsWith("LINESTRING(77.9999 28.9999"), "Longitude-first WKT");
        var clock = new MutableClock();
        var cache = new PlazaCandidateCache(1, Duration.ofSeconds(10), clock);
        var count = new AtomicInteger();
        var cell = new GridCell(77, 28);
        cache.get(1, cell, () -> { count.incrementAndGet(); return List.of(); });
        cache.get(1, cell, () -> { throw new AssertionError("Cache was not reused"); });
        cache.get(2, cell, () -> { count.incrementAndGet(); return List.of(); });
        clock.now = clock.now.plusSeconds(11);
        cache.get(2, cell, () -> { count.incrementAndGet(); return List.of(); });
        cache.get(2, new GridCell(78, 28), List::of);
        cache.get(2, cell, () -> { count.incrementAndGet(); return List.of(); });
        require(count.get() == 4, "Cache versioning, expiry, and eviction");
        System.out.println("PASS: polyline decoding, invalid inputs, cell coverage, reverse reuse, WKT, cache versioning/expiry/eviction");
    }

    private static void expectInvalid(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid input was accepted");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
