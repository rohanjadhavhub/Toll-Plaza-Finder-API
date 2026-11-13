package com.tollfinder.routing;

import com.tollfinder.domain.GeoPoint;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PolylineDecoderTest {
    @Test
    void decodesGoogleReferenceExampleWithCorrectCoordinateOrder() {
        assertThat(new PolylineDecoder().decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@"))
                .containsExactly(new GeoPoint(38.5, -120.2), new GeoPoint(40.7, -120.95),
                        new GeoPoint(43.252, -126.453));
    }

    @Test
    void rejectsEmptyTruncatedAndOverflowingPolylines() {
        for (String value : new String[]{"", "_", "~~~~~~~", "?", "\u0000"}) {
            assertThatThrownBy(() -> new PolylineDecoder().decode(value)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
