package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateAmenityRequest;
import com.hotelbooking.dto.response.AmenityResponse;
import com.hotelbooking.entity.*;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.AmenityAlreadyExistsException;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.AmenityRepository;
import com.hotelbooking.repository.RoomTypeRepository;
import com.hotelbooking.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmenityServiceImplTest {

    @Mock
    private AmenityRepository amenityRepository;
    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private AmenityServiceImpl amenityService;

    private User owner;
    private RoomType roomType;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).role(Role.HOTEL_MANAGER).build();
        Hotel hotel = Hotel.builder().id(1L).owner(owner).build();
        roomType = RoomType.builder().id(1L).hotel(hotel).amenities(new HashSet<>()).build();
    }

    @Test
    void create_savesAmenity_whenNameIsUnique() {
        when(amenityRepository.findByNameIgnoreCase("WiFi")).thenReturn(Optional.empty());
        CreateAmenityRequest request = new CreateAmenityRequest("WiFi");

        AmenityResponse response = amenityService.create(request);

        assertThat(response.name()).isEqualTo("WiFi");
        verify(amenityRepository).save(any(Amenity.class));
    }

    @Test
    void create_throwsAlreadyExists_whenNameTaken() {
        when(amenityRepository.findByNameIgnoreCase("WiFi"))
                .thenReturn(Optional.of(Amenity.builder().id(1L).name("WiFi").build()));
        CreateAmenityRequest request = new CreateAmenityRequest("WiFi");

        assertThatThrownBy(() -> amenityService.create(request))
                .isInstanceOf(AmenityAlreadyExistsException.class);
    }

    @Test
    void getAll_returnsMappedAmenities() {
        when(amenityRepository.findAll()).thenReturn(List.of(Amenity.builder().id(1L).name("Pool").build()));

        List<AmenityResponse> result = amenityService.getAll();

        assertThat(result).extracting(AmenityResponse::name).containsExactly("Pool");
    }

    @Test
    void getByRoomType_throwsResourceNotFound_whenRoomTypeMissing() {
        when(roomTypeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> amenityService.getByRoomType(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addToRoomType_addsAmenity_whenCallerIsOwner() {
        Amenity amenity = Amenity.builder().id(2L).name("Breakfast").build();
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
        when(amenityRepository.findById(2L)).thenReturn(Optional.of(amenity));

        AmenityResponse response = amenityService.addToRoomType(1L, 2L, new UserPrincipal(owner));

        assertThat(response.name()).isEqualTo("Breakfast");
        assertThat(roomType.getAmenities()).contains(amenity);
    }

    @Test
    void addToRoomType_throwsForbidden_whenCallerIsNotOwnerOrAdmin() {
        User otherManager = User.builder().id(2L).role(Role.HOTEL_MANAGER).build();
        Amenity amenity = Amenity.builder().id(2L).name("Breakfast").build();
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
        when(amenityRepository.findById(2L)).thenReturn(Optional.of(amenity));

        assertThatThrownBy(() -> amenityService.addToRoomType(1L, 2L, new UserPrincipal(otherManager)))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
