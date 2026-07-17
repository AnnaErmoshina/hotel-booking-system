package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateRoomTypeRequest;
import com.hotelbooking.dto.response.RoomTypeResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.RoomTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomTypeControllerTest {

    @Mock
    private RoomTypeService roomTypeService;

    @Test
    void getByHotel_returns200() {
        RoomTypeController controller = new RoomTypeController(roomTypeService);
        when(roomTypeService.getByHotel(1L)).thenReturn(List.of());

        var response = controller.getByHotel(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void create_returns201() {
        RoomTypeController controller = new RoomTypeController(roomTypeService);
        UserPrincipal principal = new UserPrincipal(User.builder().id(1L).role(Role.HOTEL_MANAGER).build());
        CreateRoomTypeRequest request = new CreateRoomTypeRequest("Deluxe", "desc", BigDecimal.TEN, 2);
        RoomTypeResponse roomType = new RoomTypeResponse(1L, 1L, "Deluxe", "desc", BigDecimal.TEN, 2);
        when(roomTypeService.create(1L, request, principal)).thenReturn(roomType);

        var response = controller.create(1L, request, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(roomType);
    }
}
