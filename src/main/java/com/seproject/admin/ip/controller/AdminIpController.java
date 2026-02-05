package com.seproject.admin.ip.controller;

import com.seproject.account.Ip.domain.Ip;
import com.seproject.account.Ip.application.IpService;
import com.seproject.admin.account.controller.condition.AccountCondition;
import com.seproject.admin.ip.application.AdminIpAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.seproject.admin.ip.controller.dto.IpDTO.*;

@Tag(name = "아이피 관리 API", description = "관리자 시스템이 갖는 금지 아이피 관리 API")
@AllArgsConstructor
@RequestMapping(value = "/admin/ip")
@RestController
public class AdminIpController {

    private final AdminIpAppService adminIpAppService;

    @Operation(summary = "아이피 목록 조회", description = "아이피의 목록을 조회한다.")
    @GetMapping
    public ResponseEntity<List<IpResponse>> retrieveAllBannedIp(String ipType) {

        IpCondition condition = new IpCondition(ipType);
        List<IpResponse> response = adminIpAppService.findAll(condition);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "아이피 추가", description = "접근을 금지할 아이피를 추가한다.")
    @PostMapping
    public ResponseEntity<Void> createBannedIp(@RequestBody CreateIpRequest request) {
        adminIpAppService.createIp(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "아이피 삭제", description = "접근이 금지된 아이피를 삭제(접근 허용)한다.")
    @DeleteMapping
    public ResponseEntity<Void> deleteBannedIp(@RequestBody DeleteIpRequest request) {
        adminIpAppService.delete(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
