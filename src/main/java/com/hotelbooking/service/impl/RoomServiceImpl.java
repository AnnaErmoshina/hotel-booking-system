package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateRoomRequest;
import com.hotelbooking.dto.response.RoomResponse;
import com.hotelbooking.entity.Room;
import com.hotelbooking.entity.RoomType;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.RoomRepository;
import com.hotelbooking.repository.RoomTypeRepository;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    @Transactional
    public RoomResponse create(Long roomTypeId, CreateRoomRequest request, UserPrincipal currentUser) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found with id: " + roomTypeId));

        assertOwnerOrAdmin(roomType, currentUser.getUser());

        Room room = Room.builder()
                .roomType(roomType)
                .roomNumber(request.roomNumber())
                .floor(request.floor())
                .build();

        roomRepository.save(room);
        return toResponse(room);
    }

    @Override
    public List<RoomResponse> getByRoomType(Long roomTypeId) {
        return roomRepository.findByRoomTypeId(roomTypeId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void assertOwnerOrAdmin(RoomType roomType, User currentUser) {
        boolean isOwner = roomType.getHotel().getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("You do not have permission to modify rooms of this hotel");
        }
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getRoomType().getId(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getStatus().name()
        );
    }
}
