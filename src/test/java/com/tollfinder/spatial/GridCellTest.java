package com.tollfinder.spatial;

import com.tollfinder.domain.DrivingRoute;
import com.tollfinder.domain.GeoPoint;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GridCellTest {
    @Test
    void coversSegmentInteriorsAndCorridorAcrossCellBoundaries() {
        var route = new DrivingRoute(List.of(new GeoPoint(28.9999, 77.9999), new GeoPoint(28.9999, 79.2)), 120000);
        assertThat(GridCell.covering(route, 50)).contains(new GridCell(77, 28), new GridCell(78, 28),
                new GridCell(79, 28), new GridCell(77, 29), new GridCell(78, 29), new GridCell(79, 29));
    }

    @Test
    void reversingTheRouteReusesTheSameCells() {
        var a = new GeoPoint(28.5, 77.2);
        var b = new GeoPoint(29.2, 78.5);
        assertThat(GridCell.covering(new DrivingRoute(List.of(a, b), 100000), 50))
                .isEqualTo(GridCell.covering(new DrivingRoute(List.of(b, a), 100000), 50));
    }
}
