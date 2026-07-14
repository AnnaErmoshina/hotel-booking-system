package com.hotelbooking.service;

import com.hotelbooking.dto.request.CreateRoomRequest;
import com.hotelbooking.dto.response.RoomResponse;
import com.hotelbooking.security.UserPrincipal;

import java.util.List;

public interface RoomService {

    RoomResponse create(Long roomTypeId, CreateRoomRequest request, UserPrincipal currentUser);

    List<RoomResponse> getByRoomType(Long roomTypeId);
}
