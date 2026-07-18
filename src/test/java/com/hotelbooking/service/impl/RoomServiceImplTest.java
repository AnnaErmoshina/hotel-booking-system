package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateRoomRequest;
import com.hotelbooking.dto.response.RoomResponse;
import com.hotelbooking.entity.*;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.RoomRepository;
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
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private User owner;
    private RoomType roomType;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).role(Role.HOTEL_MANAGER).build();
        Hotel hotel = Hotel.builder().id(1L).owner(owner).build();
        roomType = RoomType.builder().id(1L).hotel(hotel).basePrice(BigDecimal.TEN).build();
    }

    @Test
    void create_savesRoom_whenCallerIsOwner() {
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
        CreateRoomRequest request = new CreateRoomRequest("101", 1);

        RoomResponse response = roomService.create(1L, request, new UserPrincipal(owner));

        assertThat(response.roomNumber()).isEqualTo("101");
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void create_throwsForbidden_whenCallerIsNotOwnerOrAdmin() {
        User otherManager = User.builder().id(2L).role(Role.HOTEL_MANAGER).build();
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
        CreateRoomRequest request = new CreateRoomRequest("101", 1);

        assertThatThrownBy(() -> roomService.create(1L, request, new UserPrincipal(otherManager)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void create_throwsResourceNotFound_whenRoomTypeMissing() {
        when(roomTypeRepository.findById(99L)).thenReturn(Optional.empty());
        CreateRoomRequest request = new CreateRoomRequest("101", 1);

        assertThatThrownBy(() -> roomService.create(99L, request, new UserPrincipal(owner)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByRoomType_returnsMappedRooms() {
        Room room = Room.builder().id(1L).roomType(roomType).roomNumber("101").build();
        when(roomRepository.findByRoomTypeId(1L)).thenReturn(List.of(room));

        List<RoomResponse> result = roomService.getByRoomType(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).roomNumber()).isEqualTo("101");
    }
}
