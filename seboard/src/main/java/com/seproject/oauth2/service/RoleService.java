package com.seproject.oauth2.service;

import com.seproject.oauth2.model.Role;
import com.seproject.oauth2.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.seproject.oauth2.controller.dto.RoleDTO.*;

@RequiredArgsConstructor
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RetrieveAllRoleResponse findAll(int page, int perPage) {
        Pageable pageable = PageRequest.of(page,perPage);

        Page<Role> all = roleRepository.findAll(pageable);
        List<Role> roles = all.toList();
        int total = all.getTotalPages();
        int nowPage = all.getNumber();

        return RetrieveAllRoleResponse.toDTO(roles,total,nowPage,perPage);
    }

}
