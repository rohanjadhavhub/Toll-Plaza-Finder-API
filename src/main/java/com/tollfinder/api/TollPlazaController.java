package com.tollfinder.api;

import com.tollfinder.application.FindTollPlazasService;
import com.tollfinder.domain.Pincode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/toll-plazas")
@Tag(name = "Toll plazas")
public class TollPlazaController {
    private final FindTollPlazasService service;

    public TollPlazaController(FindTollPlazasService service) { this.service = service; }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @Operation(summary = "Find toll plazas on a driving route between Indian PIN codes",
            description = "Returns plazas in travel order. All distances use kilometers. "
                    + "No matches returns 200 with an empty tollPlazas array. "
                    + "Default corridor is 50 meters; parallel roads and opposite carriageways may match.")
    @ApiResponse(responseCode = "200", description = "Route and ordered plazas, possibly empty",
            content = @Content(schema = @Schema(implementation = TollPlazaResponse.class), examples = {
                    @ExampleObject(name = "With plazas", value = """
                            {"route":{"sourcePincode":"110001","destinationPincode":"560001","distanceInKm":2100},
                             "tollPlazas":[{"name":"Toll Plaza 1","latitude":28.7041,"longitude":77.1025,"distanceFromSource":200}]}
                            """),
                    @ExampleObject(name = "No plazas", value = """
                            {"route":{"sourcePincode":"110001","destinationPincode":"560001","distanceInKm":2100},"tollPlazas":[]}
                            """) }))
    @ApiResponse(responseCode = "400", description = "Missing, invalid, unresolved, or identical PIN codes",
            content = @Content(schema = @Schema(implementation = ApiError.class), examples = {
                    @ExampleObject(name = "Invalid PIN", value = "{\"error\":\"Invalid source or destination pincode\"}"),
                    @ExampleObject(name = "Same PIN", value = "{\"error\":\"Source and destination pincodes cannot be the same\"}") }))
    @ApiResponse(responseCode = "422", description = "No driving route available", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "415", description = "Request body must use application/json", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "502", description = "Invalid response from routing provider", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "503", description = "Routing provider or database unavailable", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "504", description = "Routing provider timeout", content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<TollPlazaResponse> find(@Valid @RequestBody TollPlazaRequest request) {
        var source = new Pincode(request.sourcePincode());
        var destination = new Pincode(request.destinationPincode());
        if (source.equals(destination)) {
            throw new IllegalArgumentException("Source and destination pincodes cannot be the same");
        }
        return ResponseEntity.ok().header("Cache-Control", "no-store").header("X-Route-Provider", "Google Maps")
                .body(TollPlazaResponse.from(request, service.find(source, destination)));
    }
}
