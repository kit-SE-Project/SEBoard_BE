package com.seproject.board.bulletin.domain.model;

import com.seproject.board.menu.domain.model.Menu;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "main_page_menu")
public class MainPageMenu {

    @Id @GeneratedValue
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    public MainPageMenu(Menu menu) {
        this.menu = menu;
    }

}
