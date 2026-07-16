package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateAmenityRequest;
import com.hotelbooking.dto.response.AmenityResponse;
import com.hotelbooking.entity.Amenity;
import com.hotelbooking.entity.RoomType;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.AmenityAlreadyExistsException;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.AmenityRepository;
import com.hotelbooking.repository.RoomTypeRepository;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    @Transactional
    public AmenityResponse create(CreateAmenityRequest request) {
        amenityRepository.findByNameIgnoreCase(request.name()).ifPresent(existing -> {
            throw new AmenityAlreadyExistsException(
                    "Amenity already exists with name: " + request.name());
        });

        Amenity amenity = Amenity.builder()
                .name(request.name())
                .build();

        amenityRepository.save(amenity);
        return toResponse(amenity);
    }

    @Override
    public List<AmenityResponse> getAll() {
        return amenityRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<AmenityResponse> getByRoomType(Long roomTypeId) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found with id: " + roomTypeId));

        return roomType.getAmenities().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AmenityResponse addToRoomType(Long roomTypeId, Long amenityId, UserPrincipal currentUser) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found with id: " + roomTypeId));

        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found with id: " + amenityId));

        assertOwnerOrAdmin(roomType, currentUser.getUser());

        roomType.getAmenities().add(amenity);
        roomTypeRepository.save(roomType);

        return toResponse(amenity);
    }

    private void assertOwnerOrAdmin(RoomType roomType, User currentUser) {
        boolean isOwner = roomType.getHotel().getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("You do not have permission to modify this room type");
        }
    }

    private AmenityResponse toResponse(Amenity amenity) {
        return new AmenityResponse(amenity.getId(), amenity.getName());
    }
}
