package com.tollfinder.routing;

import com.tollfinder.application.FinderException;
import com.tollfinder.domain.DrivingRoute;
import com.tollfinder.domain.GeoPoint;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GoogleRoutesClient {
    private final GoogleHttpClient http;
    private final PolylineDecoder decoder;

    public GoogleRoutesClient(GoogleHttpClient http, PolylineDecoder decoder) {
        this.http = http;
        this.decoder = decoder;
    }

    public DrivingRoute compute(GeoPoint source, GeoPoint destination) {
        var body = http.postRoute(Map.of(
                "origin", waypoint(source), "destination", waypoint(destination), "travelMode", "DRIVE",
                "routingPreference", "TRAFFIC_UNAWARE", "computeAlternativeRoutes", false,
                "polylineQuality", "HIGH_QUALITY", "polylineEncoding", "ENCODED_POLYLINE",
                "routeModifiers", Map.of("avoidTolls", false), "units", "METRIC"));
        var routes = body.path("routes");
        if (!routes.isArray() && !routes.isMissingNode()) throw GoogleHttpClient.invalidResponse();
        if (routes.isMissingNode() || routes.isEmpty()) {
            throw new FinderException(FinderException.Reason.NO_ROUTE, "No driving route found between the pincodes");
        }
        var route = routes.get(0);
        var distance = route.path("distanceMeters");
        var encoded = route.path("polyline").path("encodedPolyline");
        if (!distance.isIntegralNumber() || !distance.canConvertToLong() || !encoded.isTextual()) {
            throw GoogleHttpClient.invalidResponse();
        }
        try {
            return new DrivingRoute(decoder.decode(encoded.textValue()), distance.longValue());
        } catch (IllegalArgumentException exception) {
            throw GoogleHttpClient.invalidResponse();
        }
    }

    private Map<String, ?> waypoint(GeoPoint point) {
        return Map.of("location", Map.of("latLng", Map.of("latitude", point.latitude(), "longitude", point.longitude())));
    }
}
