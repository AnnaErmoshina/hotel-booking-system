package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateAmenityRequest;
import com.hotelbooking.dto.response.AmenityResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.AmenityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmenityControllerTest {

    @Mock
    private AmenityService amenityService;

    @Test
    void getAll_returns200() {
        AmenityController controller = new AmenityController(amenityService);
        when(amenityService.getAll()).thenReturn(List.of(new AmenityResponse(1L, "WiFi")));

        var response = controller.getAll();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void create_returns201() {
        AmenityController controller = new AmenityController(amenityService);
        CreateAmenityRequest request = new CreateAmenityRequest("Pool");
        AmenityResponse amenity = new AmenityResponse(1L, "Pool");
        when(amenityService.create(request)).thenReturn(amenity);

        var response = controller.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(amenity);
    }

    @Test
    void getByRoomType_returns200() {
        AmenityController controller = new AmenityController(amenityService);
        when(amenityService.getByRoomType(1L)).thenReturn(List.of());

        var response = controller.getByRoomType(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void addToRoomType_returns200() {
        AmenityController controller = new AmenityController(amenityService);
        UserPrincipal principal = new UserPrincipal(User.builder().id(1L).role(Role.HOTEL_MANAGER).build());
        AmenityResponse amenity = new AmenityResponse(2L, "Breakfast");
        when(amenityService.addToRoomType(1L, 2L, principal)).thenReturn(amenity);

        var response = controller.addToRoomType(1L, 2L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(amenity);
    }
}
