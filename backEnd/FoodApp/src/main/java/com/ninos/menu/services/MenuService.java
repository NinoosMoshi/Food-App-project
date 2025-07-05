package com.ninos.menu.services;

import com.ninos.menu.dtos.MenuDTO;
import com.ninos.response.Response;

import java.util.List;

public interface MenuService {

    Response<MenuDTO> createMenu(MenuDTO menuDTO);
    Response<MenuDTO> updateMenu(MenuDTO menuDTO);
    Response<MenuDTO> getMenuById(Long id);
    Response<?> deleteMenuById(Long id);
    Response<List<MenuDTO>> getMenus(Long categoryId, String search);

}
