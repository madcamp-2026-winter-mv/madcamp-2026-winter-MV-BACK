package com.example.madcamp_2026_winter_MV.controller;

import com.example.madcamp_2026_winter_MV.entity.Advertisement;
import com.example.madcamp_2026_winter_MV.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdvertisementRepository adRepository;

    // 1. 광고 목록 조회 (사용자용)
    @GetMapping
    public ResponseEntity<List<Advertisement>> getAds() {
        return ResponseEntity.ok(adRepository.findAllByOrderByDisplayOrderAsc());
    }

    //  2. 광고 등록 (관리자용)
    @PostMapping
    public ResponseEntity<Advertisement> createAd(@RequestBody Advertisement ad) {
        return ResponseEntity.ok(adRepository.save(ad));
    }

    //  3. 광고 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAd(@PathVariable Long id) {
        adRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}