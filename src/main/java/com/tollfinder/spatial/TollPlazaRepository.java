package com.tollfinder.spatial;

import com.tollfinder.domain.DrivingRoute;
import com.tollfinder.domain.TollPlaza;
import com.tollfinder.domain.TollPlazaMatch;
import java.util.Collection;
import java.util.List;

public interface TollPlazaRepository {
    long datasetVersion();
    List<TollPlaza> findCandidates(GridCell cell);
    List<TollPlazaMatch> findMatches(DrivingRoute route, Collection<Long> candidateIds, double corridorMeters);
}
