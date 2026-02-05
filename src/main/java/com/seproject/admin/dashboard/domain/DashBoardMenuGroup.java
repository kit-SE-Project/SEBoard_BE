package com.seproject.admin.dashboard.domain;

public enum DashBoardMenuGroup {

    MENU_GROUP("menu"),
    PERSON_GROUP("person"),
    CONTENT_GROUP("content"),
    SETTING_GROUP("setting");

    private final String name;

    DashBoardMenuGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
