package com.seproject.board.menu.service;

import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.domain.repository.MenuRepository;
import com.seproject.board.post.domain.repository.PostRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MenuService {
    private final PostRepository postRepository;
    private final MenuRepository menuRepository;

    public List<Menu> findByIds(List<Long> ids) {
        return menuRepository.findAllById(ids);
    }
    public Menu findById(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_MENU, null));
    }

    public Map<Menu, List<Menu>> findAllMenuTree() {
        List<Menu> groupMenus = menuRepository.findByDepthWithSuperMenu(1);
        return groupMenus.stream()
                .collect(Collectors.groupingBy(Menu::getSuperMenu));
    }

    public List<Menu> findSubMenu(Long superMenuId) {
        return menuRepository.findBySuperMenu(superMenuId);
    }

    public List<Menu> findByDepth(int depth) {
        return menuRepository.findByDepth(depth);
    }

    public Map<Long, List<Menu>> findSubMenu(List<Long> superMenuIds) {
        List<Menu> subMenus = menuRepository.findBySuperMenu(superMenuIds);

        return subMenus.stream()
                .collect(Collectors.groupingBy(
                        menu -> menu.getSuperMenu().getMenuId(),
                        HashMap::new,
                        Collectors.toCollection(ArrayList::new)
                ));
    }


    public boolean hasPost(Long categoryId) {
        return postRepository.existsByCategoryId(categoryId);
    }

    public boolean hasSubCategory(Long categoryId){
        return menuRepository.existsSubMenuById(categoryId);
    }
}
