package pl.edu.salonmanager.salon_manager.controller.mvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.edu.salonmanager.salon_manager.config.SalonProperties;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.response.ServiceOfferDto;
import pl.edu.salonmanager.salon_manager.service.ServiceOfferService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SalonProperties salonProperties;

    @MockBean
    private ServiceOfferService serviceOfferService;

    @BeforeEach
    void setUp() {
        when(salonProperties.getName()).thenReturn("Beauty Salon");
        when(salonProperties.getAddress()).thenReturn("Test Street 123");
    }

    @Test
    void shouldDisplayHomepage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/index"))
                .andExpect(model().attributeExists("salon"))
                .andExpect(model().attribute("salon", is(salonProperties)));
    }

    @Test
    void shouldDisplayServicesPage() throws Exception {
        // Given
        List<ServiceOfferDto> services = Arrays.asList(
                new ServiceOfferDto(1L, "Haircut", new BigDecimal("50.00"), 30),
                new ServiceOfferDto(2L, "Manicure", new BigDecimal("40.00"), 45)
        );
        when(serviceOfferService.getAllServices()).thenReturn(services);

        // When & Then
        mockMvc.perform(get("/services"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/services"))
                .andExpect(model().attributeExists("salon"))
                .andExpect(model().attributeExists("services"))
                .andExpect(model().attribute("services", hasSize(2)))
                .andExpect(model().attribute("services", everyItem(
                        instanceOf(ServiceOfferDto.class))))
                .andExpect(model().attribute("services", hasItem(
                        allOf(
                                hasProperty("id", is(1L)),
                                hasProperty("name", is("Haircut")),
                                hasProperty("price", is(new BigDecimal("50.00"))),
                                hasProperty("durationMinutes", is(30))
                        ))))
                .andExpect(model().attribute("services", hasItem(
                        allOf(
                                hasProperty("id", is(2L)),
                                hasProperty("name", is("Manicure"))
                        ))));
    }

    @Test
    void shouldDisplayEmptyServicesListWhenNoServicesExist() throws Exception {
        // Given
        when(serviceOfferService.getAllServices()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/services"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/services"))
                .andExpect(model().attribute("services", hasSize(0)));
    }
}
