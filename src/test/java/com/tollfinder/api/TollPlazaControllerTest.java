package com.tollfinder.api;

import com.tollfinder.application.FindTollPlazasService;
import com.tollfinder.application.FinderException;
import com.tollfinder.domain.DrivingRoute;
import com.tollfinder.domain.GeoPoint;
import com.tollfinder.domain.TollPlazaMatch;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TollPlazaController.class)
@Import(JsonConfiguration.class)
class TollPlazaControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean FindTollPlazasService service;

    @Test
    void returnsScreenshotContractInKilometers() throws Exception {
        when(service.find(any(), any())).thenReturn(new FindTollPlazasService.Result(
                new DrivingRoute(List.of(new GeoPoint(28, 77), new GeoPoint(29, 78)), 2100000),
                List.of(new TollPlazaMatch(1, "Plaza", "Delhi", new GeoPoint(28.7041, 77.1025), 200000))));
        mvc.perform(post("/api/v1/toll-plazas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourcePincode\":\"110001\",\"destinationPincode\":\"560001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.sourcePincode").value("110001"))
                .andExpect(jsonPath("$.route.distanceInKm").value(2100))
                .andExpect(jsonPath("$.tollPlazas[0].distanceFromSource").value(200))
                .andExpect(jsonPath("$.tollPlazas[0].name").value("Plaza"))
                .andExpect(jsonPath("$.tollPlazas[0].state").doesNotExist());
    }

    @Test
    void rejectsInvalidMissingNumericAndIdenticalPincodesBeforeCallingDependencies() throws Exception {
        for (String body : List.of("{}", "{\"sourcePincode\":110001,\"destinationPincode\":\"560001\"}",
                "{\"sourcePincode\":\"000001\",\"destinationPincode\":\"560001\"}")) {
            mvc.perform(post("/api/v1/toll-plazas").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
        }
        mvc.perform(post("/api/v1/toll-plazas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourcePincode\":\"110001\",\"destinationPincode\":\"110001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Source and destination pincodes cannot be the same"));
        verifyNoInteractions(service);
    }

    @Test
    void returnsEmptyListAsSuccess() throws Exception {
        when(service.find(any(), any())).thenReturn(new FindTollPlazasService.Result(
                new DrivingRoute(List.of(new GeoPoint(28, 77), new GeoPoint(29, 78)), 1000), List.of()));
        mvc.perform(post("/api/v1/toll-plazas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourcePincode\":\"110001\",\"destinationPincode\":\"560001\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tollPlazas").isEmpty());
    }

    @Test
    void returnsConsistentDependencyErrorsWithoutInternalDetails() throws Exception {
        var reasons = List.of(FinderException.Reason.INVALID_PINCODE, FinderException.Reason.NO_ROUTE,
                FinderException.Reason.UPSTREAM_FAILURE, FinderException.Reason.UNAVAILABLE, FinderException.Reason.TIMEOUT);
        int[] statuses = {400, 422, 502, 503, 504};
        for (int i = 0; i < reasons.size(); i++) {
            doThrow(new FinderException(reasons.get(i), "Safe error")).when(service).find(any(), any());
            mvc.perform(post("/api/v1/toll-plazas").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sourcePincode\":\"110001\",\"destinationPincode\":\"560001\"}"))
                    .andExpect(status().is(statuses[i])).andExpect(jsonPath("$.error").value("Safe error"))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
    }
}
