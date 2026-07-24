package com.seproject.global.data_setup;

import com.seproject.board.bulletin.domain.model.MainPageMenu;
import com.seproject.board.bulletin.domain.repository.MainPageMenuRepository;
import com.seproject.board.menu.domain.model.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MainPageMenuSetup {

    @Autowired
    MainPageMenuRepository mainPageMenuRepository;


    public MainPageMenu createMainPageMenu(Menu menu) {
        MainPageMenu mainPageMenu = new MainPageMenu(menu);
        mainPageMenuRepository.save(mainPageMenu);
        return mainPageMenu;
    }
}
