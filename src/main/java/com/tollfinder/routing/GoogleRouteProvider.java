package com.tollfinder.routing;

import com.tollfinder.domain.DrivingRoute;
import com.tollfinder.domain.Pincode;
import org.springframework.stereotype.Component;

@Component
public class GoogleRouteProvider implements RouteProvider {
    private final GoogleGeocodingClient geocoding;
    private final GoogleRoutesClient routes;

    public GoogleRouteProvider(GoogleGeocodingClient geocoding, GoogleRoutesClient routes) {
        this.geocoding = geocoding;
        this.routes = routes;
    }

    @Override
    public DrivingRoute findRoute(Pincode source, Pincode destination) {
        return routes.compute(geocoding.resolve(source), geocoding.resolve(destination));
    }
}
