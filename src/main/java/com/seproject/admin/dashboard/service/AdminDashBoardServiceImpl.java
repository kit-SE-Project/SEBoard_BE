package com.seproject.admin.dashboard.service;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.domain.DashBoardMenuAuthorization;
import com.seproject.admin.dashboard.domain.repository.DashBoardMenuRepository;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.NoSuchResourceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminDashBoardServiceImpl {


    private final DashBoardMenuRepository dashBoardMenuRepository;

    private List<DashBoardMenu> all; // cache


    public AdminDashBoardServiceImpl(DashBoardMenuRepository dashBoardMenuRepository) {
        this.dashBoardMenuRepository = dashBoardMenuRepository;
        all = dashBoardMenuRepository.findAll();
    }

    public Optional<DashBoardMenu> findDashBoardMenu(Long id) {
        return dashBoardMenuRepository.findById(id);
    }

    public DashBoardMenu findDashBoardMenuByUrl(String url){
        return dashBoardMenuRepository.findByUrl(url)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_DASHBOARDMENU));
    }

    public List<DashBoardMenu> findDashBoardMenu(List<Long> ids) {
        List<DashBoardMenu> allById = dashBoardMenuRepository.findAllById(ids);
        return allById;
    }

    public List<DashBoardMenu> findDashBoardMenuWithRole(List<Long> ids) {
        List<DashBoardMenu> dashBoardMenusWithRole = dashBoardMenuRepository.findDashBoardMenusWithRole(ids);

        List<DashBoardMenu> res = dashBoardMenusWithRole.stream().filter(menu -> !menu.getUrl().equals("/admin/adminMenu"))
                .collect(Collectors.toList());

        return res;
    }

    public List<Long> findAuthorizeDashBoardMenuIds(Account account) {
        List<Role> roles = account.getRoles();
        List<Long> ids = new ArrayList<>();

        for (DashBoardMenu dashBoardMenu : all) {
            if(dashBoardMenu.authorize(roles)) {
                ids.add(dashBoardMenu.getId());
            }
        }

        return ids;
    }

    @Transactional
    public void update(SelectOption selectOption, DashBoardMenu dashBoardMenu, List<Role> roles) {

        List<DashBoardMenuAuthorization> collect = roles.stream()
                .map((role) -> new DashBoardMenuAuthorization(dashBoardMenu, role))
                .collect(Collectors.toList());

        dashBoardMenu.update(selectOption, collect);

        all = dashBoardMenuRepository.findAll();
    }

}
