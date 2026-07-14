package com.hotelbooking.service;

import com.hotelbooking.dto.request.CreateRoomTypeRequest;
import com.hotelbooking.dto.response.RoomTypeResponse;
import com.hotelbooking.security.UserPrincipal;

import java.util.List;

public interface RoomTypeService {

    RoomTypeResponse create(Long hotelId, CreateRoomTypeRequest request, UserPrincipal currentUser);

    List<RoomTypeResponse> getByHotel(Long hotelId);
}
