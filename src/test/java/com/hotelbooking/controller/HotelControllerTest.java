package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateHotelRequest;
import com.hotelbooking.dto.response.HotelResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.HotelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelControllerTest {

    @Mock
    private HotelService hotelService;

    private HotelController controller;
    private final UserPrincipal principal = new UserPrincipal(User.builder().id(1L).role(Role.HOTEL_MANAGER).build());

    @Test
    void getByCity_delegatesToService() {
        controller = new HotelController(hotelService);
        Page<HotelResponse> page = Page.empty();
        when(hotelService.getByCity("Paris", PageRequest.of(0, 10))).thenReturn(page);

        var response = controller.getByCity("Paris", PageRequest.of(0, 10));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(page);
    }

    @Test
    void getById_delegatesToService() {
        controller = new HotelController(hotelService);
        HotelResponse hotel = new HotelResponse(1L, "Grand", "desc", "addr", "Paris", "France", 1L);
        when(hotelService.getById(1L)).thenReturn(hotel);

        var response = controller.getById(1L);

        assertThat(response.getBody()).isEqualTo(hotel);
    }

    @Test
    void create_returns201() {
        controller = new HotelController(hotelService);
        CreateHotelRequest request = new CreateHotelRequest("Grand", "desc", "addr", "Paris", "France");
        HotelResponse hotel = new HotelResponse(1L, "Grand", "desc", "addr", "Paris", "France", 1L);
        when(hotelService.create(request, principal)).thenReturn(hotel);

        var response = controller.create(request, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(hotel);
    }

    @Test
    void update_delegatesToService() {
        controller = new HotelController(hotelService);
        CreateHotelRequest request = new CreateHotelRequest("New", "desc", "addr", "Lyon", "France");
        HotelResponse hotel = new HotelResponse(1L, "New", "desc", "addr", "Lyon", "France", 1L);
        when(hotelService.update(1L, request, principal)).thenReturn(hotel);

        var response = controller.update(1L, request, principal);

        assertThat(response.getBody()).isEqualTo(hotel);
    }

    @Test
    void delete_returns204() {
        controller = new HotelController(hotelService);

        var response = controller.delete(1L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(hotelService).delete(1L, principal);
    }
}
