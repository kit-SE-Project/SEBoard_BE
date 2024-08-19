package com.seproject.board.bulletin.service;

import com.seproject.board.bulletin.domain.model.MainPageMenu;
import com.seproject.board.bulletin.domain.repository.MainPageMenuRepository;
import com.seproject.board.bulletin.persistence.MainPageQueryRepository;
import com.seproject.board.menu.domain.model.InternalSiteMenu;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class MainPageService {

    private final MainPageMenuRepository mainPageMenuRepository;
    private final MainPageQueryRepository mainPageQueryRepository;

    @Transactional
    public Long createMainPageMenu(Menu menu) {
        if(!(menu instanceof InternalSiteMenu)) {
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_MAIN_PAGE_MENU,null);
        }

        MainPageMenu mainPageMenu = new MainPageMenu(menu);
        mainPageMenuRepository.save(mainPageMenu);
        return mainPageMenu.getId();
    }

    @Transactional
    public void createMainPageMenu(List<Menu> menus) {

        for (Menu menu : menus) {
            if(!(menu instanceof InternalSiteMenu)) {
                throw new CustomIllegalArgumentException(ErrorCode.INVALID_MAIN_PAGE_MENU,null);
            }
        }

        List<MainPageMenu> mainPageMenus = menus
                .stream()
                .map(MainPageMenu::new)
                .collect(Collectors.toList());

        mainPageMenuRepository.saveAll(mainPageMenus);
    }

    @Transactional
    public void deleteMainPageMenu(Long id) {
        MainPageMenu mainPageMenu = findById(id);
        mainPageMenuRepository.delete(mainPageMenu);
    }

    @Transactional
    public void deleteMainPageMenus(List<MainPageMenu> mainPageMenus) {
        mainPageMenuRepository.deleteAllInBatch(mainPageMenus);
    }

    public MainPageMenu findById(Long id){
        MainPageMenu mainPageMenu = mainPageMenuRepository.findById(id)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_MAIN_PAGE_MENU, null));
        return mainPageMenu;
    }

    public List<MainPageMenu> findAll(){
        List<MainPageMenu> all = mainPageMenuRepository.findAll();
        return all;
    }

    public List<MainPageMenu> findAllWithMenu() {
        return mainPageQueryRepository.findAllWithMenu();
    }

    public List<InternalSiteMenu> findAllMainPageableMenu() {
        List<InternalSiteMenu> mainPageableMenu = mainPageQueryRepository.findMainPageableMenu();
        return mainPageableMenu;
    }

}
