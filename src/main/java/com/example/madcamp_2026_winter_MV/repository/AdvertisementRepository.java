package com.example.madcamp_2026_winter_MV.repository;

import com.example.madcamp_2026_winter_MV.entity.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    // 순서대로 모든 광고 가져오기
    List<Advertisement> findAllByOrderByDisplayOrderAsc();
}