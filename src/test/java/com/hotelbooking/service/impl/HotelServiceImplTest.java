package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateHotelRequest;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceImplTest {

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelServiceImpl hotelService;

    private User owner;
    private Hotel hotel;
    private CreateHotelRequest updateRequest;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).role(Role.HOTEL_MANAGER).build();
        hotel = Hotel.builder().id(1L).owner(owner).name("Old Name").city("Paris").country("France").address("1 Rue").build();
        updateRequest = new CreateHotelRequest("New Name", "desc", "2 Rue", "Lyon", "France");
    }

    @Test
    void update_succeeds_whenCallerIsOwner() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

        var response = hotelService.update(1L, updateRequest, new UserPrincipal(owner));

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.city()).isEqualTo("Lyon");
    }

    @Test
    void update_succeeds_whenCallerIsAdmin_evenIfNotOwner() {
        User admin = User.builder().id(99L).role(Role.ADMIN).build();
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

        var response = hotelService.update(1L, updateRequest, new UserPrincipal(admin));

        assertThat(response.name()).isEqualTo("New Name");
    }

    @Test
    void update_throwsForbidden_whenCallerIsNeitherOwnerNorAdmin() {
        User otherManager = User.builder().id(2L).role(Role.HOTEL_MANAGER).build();
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

        assertThatThrownBy(() -> hotelService.update(1L, updateRequest, new UserPrincipal(otherManager)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void delete_throwsResourceNotFound_whenHotelDoesNotExist() {
        when(hotelRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.delete(404L, new UserPrincipal(owner)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(hotelRepository, never()).delete(any());
    }

    @Test
    void delete_removesHotel_whenCallerIsOwner() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

        hotelService.delete(1L, new UserPrincipal(owner));

        verify(hotelRepository).delete(hotel);
    }
}
