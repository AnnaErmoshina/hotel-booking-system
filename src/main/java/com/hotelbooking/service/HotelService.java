package com.hotelbooking.service;

import com.hotelbooking.dto.request.CreateHotelRequest;
import com.hotelbooking.dto.response.HotelResponse;
import com.hotelbooking.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HotelService {

    HotelResponse create(CreateHotelRequest request, UserPrincipal currentUser);

    HotelResponse getById(Long id);

    Page<HotelResponse> getByCity(String city, Pageable pageable);

    HotelResponse update(Long id, CreateHotelRequest request, UserPrincipal currentUser);

    void delete(Long id, UserPrincipal currentUser);
}
