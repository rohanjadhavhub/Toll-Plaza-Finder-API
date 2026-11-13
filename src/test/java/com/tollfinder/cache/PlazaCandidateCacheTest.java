package com.tollfinder.cache;

import com.tollfinder.domain.GeoPoint;
import com.tollfinder.domain.TollPlaza;
import com.tollfinder.spatial.GridCell;
import java.time.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PlazaCandidateCacheTest {
    private final GridCell cell = new GridCell(77, 28);

    @Test
    void reusesCandidatesButInvalidatesWhenDatasetVersionChanges() {
        var cache = new PlazaCandidateCache(2, Duration.ofMinutes(15), Clock.systemUTC());
        var calls = new AtomicInteger();
        var plaza = new TollPlaza(1, "Plaza", "Delhi", new GeoPoint(28.5, 77.5));
        cache.get(1, cell, () -> { calls.incrementAndGet(); return List.of(plaza); });
        assertThat(cache.get(1, cell, () -> { throw new AssertionError(); })).containsExactly(plaza);
        cache.get(2, cell, () -> { calls.incrementAndGet(); return List.of(); });
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void evictsLeastRecentlyUsedCellsAndExpiresEntries() {
        var clock = new MutableClock();
        var cache = new PlazaCandidateCache(1, Duration.ofSeconds(10), clock);
        cache.get(1, cell, List::of);
        cache.get(1, new GridCell(78, 28), List::of);
        var calls = new AtomicInteger();
        cache.get(1, cell, () -> { calls.incrementAndGet(); return List.of(); });
        clock.now = clock.now.plusSeconds(11);
        cache.get(1, cell, () -> { calls.incrementAndGet(); return List.of(); });
        assertThat(calls.get()).isEqualTo(2);
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
