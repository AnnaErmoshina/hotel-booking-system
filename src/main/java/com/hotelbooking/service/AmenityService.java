package com.hotelbooking.service;

import com.hotelbooking.dto.request.CreateAmenityRequest;
import com.hotelbooking.dto.response.AmenityResponse;
import com.hotelbooking.security.UserPrincipal;

import java.util.List;

public interface AmenityService {

    AmenityResponse create(CreateAmenityRequest request);

    List<AmenityResponse> getAll();

    List<AmenityResponse> getByRoomType(Long roomTypeId);

    AmenityResponse addToRoomType(Long roomTypeId, Long amenityId, UserPrincipal currentUser);
}
