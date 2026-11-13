package com.tollfinder.cache;

import com.tollfinder.domain.TollPlaza;
import com.tollfinder.spatial.GridCell;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

public final class PlazaCandidateCache {
    private record Key(long version, GridCell cell) {}
    private record Entry(List<TollPlaza> plazas, Instant expiresAt) {}

    private final LinkedHashMap<Key, Entry> entries = new LinkedHashMap<>(16, 0.75f, true);
    private final int maximumSize;
    private final Duration ttl;
    private final Clock clock;

    public PlazaCandidateCache(int maximumSize, Duration ttl, Clock clock) {
        if (maximumSize < 1 || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("Cache size and lifetime must be positive");
        }
        this.maximumSize = maximumSize;
        this.ttl = ttl;
        this.clock = clock;
    }

    public List<TollPlaza> get(long version, GridCell cell, Supplier<List<TollPlaza>> loader) {
        var key = new Key(version, cell);
        synchronized (entries) {
            entries.entrySet().removeIf(item -> !clock.instant().isBefore(item.getValue().expiresAt()));
            var entry = entries.get(key);
            if (entry != null) return entry.plazas();
        }
        var plazas = List.copyOf(loader.get());
        synchronized (entries) {
            entries.put(key, new Entry(plazas, clock.instant().plus(ttl)));
            while (entries.size() > maximumSize) entries.remove(entries.keySet().iterator().next());
        }
        return plazas;
    }
}
