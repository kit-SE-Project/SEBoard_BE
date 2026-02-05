package com.seproject.board.bulletin.controller;

import com.seproject.board.bulletin.application.BannerAppService;
import com.seproject.board.bulletin.controller.dto.BannerDTO;
import com.seproject.board.bulletin.persistence.BannerQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static com.seproject.board.bulletin.controller.dto.BannerDTO.*;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
public class BannerController {
    private final BannerAppService bannerAppService;

    @GetMapping
    public ResponseEntity<List<BannerResponse>> retrieveActiveBanner(){
        List<BannerResponse> activeBanners = bannerAppService.findActiveBanners();
        return new ResponseEntity<>(activeBanners, HttpStatus.OK);
    }
}
