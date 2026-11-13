package com.tollfinder.api;

import com.tollfinder.application.FindTollPlazasService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record TollPlazaResponse(RouteSummary route, List<Plaza> tollPlazas) {
    public record RouteSummary(String sourcePincode, String destinationPincode,
                               @Schema(description = "Google driving route distance in kilometers") BigDecimal distanceInKm) {}

    public record Plaza(String name, double latitude, double longitude,
                        @Schema(description = "Estimated kilometers along the route from the resolved source location")
                        BigDecimal distanceFromSource) {}

    public static TollPlazaResponse from(TollPlazaRequest request, FindTollPlazasService.Result result) {
        return new TollPlazaResponse(new RouteSummary(request.sourcePincode(), request.destinationPincode(),
                kilometers(result.route().distanceMeters())), result.plazas().stream()
                .map(plaza -> new Plaza(plaza.name(), plaza.location().latitude(), plaza.location().longitude(),
                        kilometers(plaza.distanceFromSourceMeters()))).toList());
    }

    private static BigDecimal kilometers(double meters) {
        return BigDecimal.valueOf(meters).movePointLeft(3).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
    }
}
