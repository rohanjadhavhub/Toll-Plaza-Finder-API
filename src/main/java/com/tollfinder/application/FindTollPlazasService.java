package com.tollfinder.application;

import com.tollfinder.cache.PlazaCandidateCache;
import com.tollfinder.config.FinderProperties;
import com.tollfinder.domain.*;
import com.tollfinder.routing.RouteProvider;
import com.tollfinder.spatial.GridCell;
import com.tollfinder.spatial.TollPlazaRepository;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class FindTollPlazasService {
    public record Result(DrivingRoute route, List<TollPlazaMatch> plazas) {
        public Result { plazas = List.copyOf(plazas); }
    }

    private final RouteProvider routes;
    private final TollPlazaRepository plazas;
    private final PlazaCandidateCache cache;
    private final FinderProperties properties;
    private final TransactionTemplate readTransaction;

    public FindTollPlazasService(RouteProvider routes, TollPlazaRepository plazas, PlazaCandidateCache cache,
                                FinderProperties properties, TransactionTemplate readTransaction) {
        this.routes = routes;
        this.plazas = plazas;
        this.cache = cache;
        this.properties = properties;
        this.readTransaction = readTransaction;
    }

    public Result find(Pincode source, Pincode destination) {
        if (source.equals(destination)) {
            throw new IllegalArgumentException("Source and destination pincodes cannot be the same");
        }
        var route = routes.findRoute(source, destination);
        // One snapshot prevents a concurrent dataset replacement from mixing versions.
        return readTransaction.execute(status -> {
            long version = plazas.datasetVersion();
            var candidateIds = new LinkedHashSet<Long>();
            for (var cell : GridCell.covering(route, properties.corridorMeters())) {
                cache.get(version, cell, () -> plazas.findCandidates(cell))
                        .forEach(plaza -> candidateIds.add(plaza.id()));
            }
            return new Result(route, plazas.findMatches(route, candidateIds, properties.corridorMeters()));
        });
    }
}
