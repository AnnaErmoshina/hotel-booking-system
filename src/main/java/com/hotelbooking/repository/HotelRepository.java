package com.hotelbooking.repository;

import com.hotelbooking.entity.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    Page<Hotel> findByCityIgnoreCase(String city, Pageable pageable);

    List<Hotel> findByOwnerId(Long ownerId);
}
