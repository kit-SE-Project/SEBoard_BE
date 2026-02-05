package com.seproject.admin.dashboard.controller;

import com.seproject.admin.dashboard.application.AdminDashBoardMenuAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.seproject.admin.dashboard.controller.dto.DashBoardDTO.*;


@Tag(name = "관리자 대시보드 관리 API", description = "관리자 메뉴 접근 관리 관리 API")
@AllArgsConstructor
@RequestMapping(value = "/admin/dashboard")
@RestController
public class AdminDashBoardController {

    private final AdminDashBoardMenuAppService adminDashBoardMenuAppService;

    @GetMapping
    @Operation(summary = "대시보드 메뉴 조회", description = "권한에 따른 대시 보드 메뉴를 조회한다.")
    public ResponseEntity<DashBoardMenuResponse> retrieveDashBoardMenus() {
        return new ResponseEntity<>(adminDashBoardMenuAppService.findDashBoardMenus(),HttpStatus.OK);
    }

    @GetMapping("/setting")
    @Operation(summary = "대시보드에 설정된 접근 권한을 조회" ,description = "대시 보드에 설정된 권한을 조회한다.")
    public ResponseEntity<DashBoardMenuAuthorizationResponse> retrieveSettings() {
        return new ResponseEntity<>(adminDashBoardMenuAppService.findDashBoardMenuOptions(),HttpStatus.OK);
    }

    @Operation(summary = "대시보드 메뉴 접근 권한 수정", description = "대시보드 url을 통해서 전달한 권한으로 접근 권한을 새로 설정한다.")
    @PutMapping("/setting")
    public ResponseEntity<Void> updateSettings(@RequestBody DashBoardUpdateRequest request) {
        adminDashBoardMenuAppService.update(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
