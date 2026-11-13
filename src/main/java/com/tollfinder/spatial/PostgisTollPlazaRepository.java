package com.tollfinder.spatial;

import com.tollfinder.domain.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostgisTollPlazaRepository implements TollPlazaRepository {
    private final JdbcClient jdbc;

    public PostgisTollPlazaRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public long datasetVersion() {
        return jdbc.sql("SELECT version FROM toll_dataset_metadata WHERE singleton = true")
                .query(Long.class).single();
    }

    @Override
    public List<TollPlaza> findCandidates(GridCell cell) {
        return jdbc.sql("""
                SELECT id, name, state, ST_Y(location) AS latitude, ST_X(location) AS longitude
                FROM toll_plaza
                WHERE location && ST_MakeEnvelope(:west, :south, :east, :north, 4326)
                ORDER BY id
                """).param("west", cell.longitude()).param("south", cell.latitude())
                .param("east", cell.longitude() + 1).param("north", cell.latitude() + 1)
                .query((rs, row) -> new TollPlaza(rs.getLong("id"), rs.getString("name"), rs.getString("state"), point(rs)))
                .list();
    }

    @Override
    public List<TollPlazaMatch> findMatches(DrivingRoute route, Collection<Long> candidateIds, double corridorMeters) {
        if (candidateIds.isEmpty()) return List.of();
        // Locate on each segment, then measure that segment's prefix as geography.
        // Multiplying a fraction in longitude/latitude degrees by a length in meters is incorrect.
        return jdbc.sql("""
                WITH route AS (
                    SELECT ST_GeomFromText(:route, 4326) AS line
                ), segments AS (
                    SELECT part.path[1] AS position, part.geom AS line
                    FROM route CROSS JOIN LATERAL ST_DumpSegments(route.line) AS part
                    WHERE ST_Length(part.geom::geography) > 0
                ), measured AS (
                    SELECT position, line,
                        COALESCE(SUM(ST_Length(line::geography)) OVER (
                            ORDER BY position ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS prefix
                    FROM segments
                ), candidates AS (
                    SELECT p.* FROM toll_plaza p CROSS JOIN route r
                    WHERE p.id IN (:ids)
                      AND ST_DWithin(p.location::geography, r.line::geography, :corridor)
                )
                SELECT p.id, p.name, p.state, ST_Y(p.location) AS latitude, ST_X(p.location) AS longitude,
                       nearest.distance_from_source
                FROM candidates p
                CROSS JOIN LATERAL (
                    SELECT m.prefix + ST_Length(ST_LineSubstring(m.line, 0,
                        ST_LineLocatePoint(m.line, p.location))::geography) AS distance_from_source
                    FROM measured m
                    ORDER BY ST_Distance(m.line::geography, p.location::geography), m.position
                    LIMIT 1
                ) nearest
                ORDER BY nearest.distance_from_source, p.id
                """).param("route", route.toWkt()).param("ids", candidateIds).param("corridor", corridorMeters)
                .query((rs, row) -> new TollPlazaMatch(rs.getLong("id"), rs.getString("name"), rs.getString("state"),
                        point(rs), rs.getDouble("distance_from_source"))).list();
    }

    private GeoPoint point(ResultSet rs) throws SQLException {
        return new GeoPoint(rs.getDouble("latitude"), rs.getDouble("longitude"));
    }
}
