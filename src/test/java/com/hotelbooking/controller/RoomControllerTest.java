package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateRoomRequest;
import com.hotelbooking.dto.response.RoomResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.RoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock
    private RoomService roomService;

    @Test
    void getByRoomType_returns200() {
        RoomController controller = new RoomController(roomService);
        when(roomService.getByRoomType(1L)).thenReturn(List.of());

        var response = controller.getByRoomType(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void create_returns201() {
        RoomController controller = new RoomController(roomService);
        UserPrincipal principal = new UserPrincipal(User.builder().id(1L).role(Role.HOTEL_MANAGER).build());
        CreateRoomRequest request = new CreateRoomRequest("101", 1);
        RoomResponse room = new RoomResponse(1L, 1L, "101", 1, "AVAILABLE");
        when(roomService.create(1L, request, principal)).thenReturn(room);

        var response = controller.create(1L, request, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(room);
    }
}
