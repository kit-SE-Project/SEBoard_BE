package com.seproject.admin.role.controller;

import com.seproject.admin.role.application.AdminRoleAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.seproject.admin.role.controller.dto.RoleDTO.*;

@Tag(name = "권한 관리 API", description = "관리자 시스템의 권한 관리 API")
@AllArgsConstructor
@RequestMapping(value = "/admin/roles")
@RestController
public class  AdminRoleController {

    private final AdminRoleAppService roleAppService;


    @Operation(summary = "권한 목록 조회", description = "등록된 권한 목록들을 조회한다.")
    @GetMapping
    public ResponseEntity<List<RoleResponse>> retrieveAllRole() {
        List<RoleResponse> response = roleAppService.findAll();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "권한 생성", description = "새로운 권한을 추가한다.")
    @PostMapping
    public ResponseEntity<Void> createRole(@RequestBody CreateRoleRequest createRoleRequest) {
        roleAppService.createRole(createRoleRequest);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "권한 수정", description = "권한 이름이나 설명을 수정한다.")
    @PutMapping("/{roleId}")
    public ResponseEntity<Void> updateRole(
            @RequestBody UpdateRoleRequest updateRoleRequest,
            @PathVariable Long roleId) {
        roleAppService.updateRole(roleId,updateRoleRequest);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "권한 삭제", description = "권한을 삭제한다.")
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        roleAppService.deleteRole(roleId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
