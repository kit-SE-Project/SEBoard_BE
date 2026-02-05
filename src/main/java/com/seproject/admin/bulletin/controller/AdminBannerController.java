package com.seproject.admin.bulletin.controller;

import com.seproject.admin.bulletin.application.AdminBannerAppService;
import com.seproject.admin.bulletin.controller.dto.BannerDTO;
import com.seproject.admin.bulletin.controller.dto.BannerDTO.BannerResponse;
import com.seproject.admin.bulletin.controller.dto.BannerDTO.CreateBannerRequest;
import com.seproject.board.common.controller.dto.MessageResponse;
import com.seproject.board.common.controller.dto.MessageResponse.CreateAndUpdateMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.seproject.admin.bulletin.controller.dto.BannerDTO.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/banners")
public class AdminBannerController {
    private final AdminBannerAppService adminBannerAppService;

    @GetMapping
    public ResponseEntity<List<BannerResponse>> retrieveBanner(@RequestParam(required = false) Boolean isActive){

        List<BannerResponse> res = adminBannerAppService.retrieveBanner(isActive);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<CreateAndUpdateMessage> createBanner(@RequestBody CreateBannerRequest request){
        Long bannerId = adminBannerAppService.createBanner(request);
        return ResponseEntity.ok(CreateAndUpdateMessage.of(bannerId, "배너 생성 성공"));
    }

    @PutMapping("/{bannerId}")
    public ResponseEntity<CreateAndUpdateMessage> updateBanner(@PathVariable Long bannerId, @RequestBody UpdateBannerRequest request){
        adminBannerAppService.updateBanner(bannerId, request);
        return ResponseEntity.ok(CreateAndUpdateMessage.of(bannerId, "배너 수정 성공"));
    }

    @DeleteMapping("/{bannerId}")
    public ResponseEntity<MessageResponse> deleteBanner(@PathVariable Long bannerId){
        adminBannerAppService.deleteBanner(bannerId);
        return ResponseEntity.ok(MessageResponse.of("배너 삭제 성공"));
    }
}
