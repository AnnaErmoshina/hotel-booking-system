package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateRoomTypeRequest;
import com.hotelbooking.dto.response.RoomTypeResponse;
import com.hotelbooking.entity.*;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomTypeRepository;
import com.hotelbooking.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceImplTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;
    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private RoomTypeServiceImpl roomTypeService;

    private User owner;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).role(Role.HOTEL_MANAGER).build();
        hotel = Hotel.builder().id(1L).owner(owner).build();
    }

    @Test
    void create_savesRoomType_whenCallerIsOwner() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        CreateRoomTypeRequest request = new CreateRoomTypeRequest("Deluxe", "desc", BigDecimal.valueOf(150), 2);

        RoomTypeResponse response = roomTypeService.create(1L, request, new UserPrincipal(owner));

        assertThat(response.name()).isEqualTo("Deluxe");
        verify(roomTypeRepository).save(any(RoomType.class));
    }

    @Test
    void create_throwsForbidden_whenCallerIsNotOwnerOrAdmin() {
        User otherManager = User.builder().id(2L).role(Role.HOTEL_MANAGER).build();
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        CreateRoomTypeRequest request = new CreateRoomTypeRequest("Deluxe", "desc", BigDecimal.valueOf(150), 2);

        assertThatThrownBy(() -> roomTypeService.create(1L, request, new UserPrincipal(otherManager)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void create_succeeds_whenCallerIsAdmin() {
        User admin = User.builder().id(99L).role(Role.ADMIN).build();
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        CreateRoomTypeRequest request = new CreateRoomTypeRequest("Suite", null, BigDecimal.ONE, 1);

        RoomTypeResponse response = roomTypeService.create(1L, request, new UserPrincipal(admin));

        assertThat(response.name()).isEqualTo("Suite");
    }

    @Test
    void create_throwsResourceNotFound_whenHotelMissing() {
        when(hotelRepository.findById(404L)).thenReturn(Optional.empty());
        CreateRoomTypeRequest request = new CreateRoomTypeRequest("Deluxe", "desc", BigDecimal.TEN, 2);

        assertThatThrownBy(() -> roomTypeService.create(404L, request, new UserPrincipal(owner)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByHotel_returnsMappedRoomTypes() {
        RoomType rt = RoomType.builder().id(1L).hotel(hotel).name("Standard").basePrice(BigDecimal.TEN).build();
        when(roomTypeRepository.findByHotelId(1L)).thenReturn(List.of(rt));

        List<RoomTypeResponse> result = roomTypeService.getByHotel(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Standard");
    }
}
