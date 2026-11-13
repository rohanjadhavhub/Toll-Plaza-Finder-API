package com.tollfinder.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TollPlazaRequest(
        @NotNull @Pattern(regexp = "[1-9][0-9]{5}")
        @Schema(type = "string", pattern = "[1-9][0-9]{5}", example = "110001", requiredMode = Schema.RequiredMode.REQUIRED)
        String sourcePincode,
        @NotNull @Pattern(regexp = "[1-9][0-9]{5}")
        @Schema(type = "string", pattern = "[1-9][0-9]{5}", example = "560001", requiredMode = Schema.RequiredMode.REQUIRED)
        String destinationPincode) {}
