package com.tollfinder.api;

import com.tollfinder.application.FindTollPlazasService;
import com.tollfinder.routing.RouteProvider;
import com.tollfinder.spatial.TollPlazaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
@AutoConfigureMockMvc
class OpenApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean FindTollPlazasService service;
    @MockitoBean RouteProvider routes;
    @MockitoBean TollPlazaRepository repository;
    @MockitoBean TransactionTemplate readTransaction;

    @Test
    void publishesThePostContractSchemasAndErrorResponses() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/toll-plazas'].post").exists())
                .andExpect(jsonPath("$.components.schemas.TollPlazaRequest.properties.sourcePincode.pattern").value("[1-9][0-9]{5}"))
                .andExpect(jsonPath("$.components.schemas.TollPlazaResponse.properties.route").exists())
                .andExpect(jsonPath("$.paths['/api/v1/toll-plazas'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/toll-plazas'].post.responses['504']").exists());
    }

    @Test
    void servesInteractiveSwaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
