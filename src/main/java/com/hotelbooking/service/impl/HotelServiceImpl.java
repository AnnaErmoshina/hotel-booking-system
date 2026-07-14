package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateHotelRequest;
import com.hotelbooking.dto.response.HotelResponse;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;

    @Override
    @Transactional
    public HotelResponse create(CreateHotelRequest request, UserPrincipal currentUser) {
        Hotel hotel = Hotel.builder()
                .name(request.name())
                .description(request.description())
                .address(request.address())
                .city(request.city())
                .country(request.country())
                .owner(currentUser.getUser())
                .build();

        hotelRepository.save(hotel);
        return toResponse(hotel);
    }

    @Override
    public HotelResponse getById(Long id) {
        return toResponse(findHotelOrThrow(id));
    }

    @Override
    public Page<HotelResponse> getByCity(String city, Pageable pageable) {
        return hotelRepository.findByCityIgnoreCase(city, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public HotelResponse update(Long id, CreateHotelRequest request, UserPrincipal currentUser) {
        Hotel hotel = findHotelOrThrow(id);
        assertOwnerOrAdmin(hotel, currentUser.getUser());

        hotel.setName(request.name());
        hotel.setDescription(request.description());
        hotel.setAddress(request.address());
        hotel.setCity(request.city());
        hotel.setCountry(request.country());

        hotelRepository.save(hotel);
        return toResponse(hotel);
    }

    @Override
    @Transactional
    public void delete(Long id, UserPrincipal currentUser) {
        Hotel hotel = findHotelOrThrow(id);
        assertOwnerOrAdmin(hotel, currentUser.getUser());
        hotelRepository.delete(hotel);
    }

    private Hotel findHotelOrThrow(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
    }

    private void assertOwnerOrAdmin(Hotel hotel, User currentUser) {
        boolean isOwner = hotel.getOwner().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("You do not have permission to modify this hotel");
        }
    }

    private HotelResponse toResponse(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getDescription(),
                hotel.getAddress(),
                hotel.getCity(),
                hotel.getCountry(),
                hotel.getOwner().getId()
        );
    }
}
