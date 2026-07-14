package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateRoomTypeRequest;
import com.hotelbooking.dto.response.RoomTypeResponse;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.RoomType;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomTypeRepository;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;

    @Override
    @Transactional
    public RoomTypeResponse create(Long hotelId, CreateRoomTypeRequest request, UserPrincipal currentUser) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + hotelId));

        assertOwnerOrAdmin(hotel, currentUser.getUser());

        RoomType roomType = RoomType.builder()
                .hotel(hotel)
                .name(request.name())
                .description(request.description())
                .basePrice(request.basePrice())
                .capacity(request.capacity())
                .build();

        roomTypeRepository.save(roomType);
        return toResponse(roomType);
    }

    @Override
    public List<RoomTypeResponse> getByHotel(Long hotelId) {
        return roomTypeRepository.findByHotelId(hotelId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void assertOwnerOrAdmin(Hotel hotel, User currentUser) {
        boolean isOwner = hotel.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("You do not have permission to modify this hotel");
        }
    }

    private RoomTypeResponse toResponse(RoomType roomType) {
        return new RoomTypeResponse(
                roomType.getId(),
                roomType.getHotel().getId(),
                roomType.getName(),
                roomType.getDescription(),
                roomType.getBasePrice(),
                roomType.getCapacity()
        );
    }
}
