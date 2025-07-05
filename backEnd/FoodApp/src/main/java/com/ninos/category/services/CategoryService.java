package com.ninos.category.services;

import com.ninos.category.dtos.CategoryDTO;
import com.ninos.category.entity.Category;
import com.ninos.response.Response;

import java.util.List;

public interface CategoryService {

    Response<CategoryDTO> addCategory(CategoryDTO categoryDTO);
    Response<CategoryDTO> updateCategory(CategoryDTO categoryDTO);
    Response<CategoryDTO> getCategoryById(Long id);
    Response<List<CategoryDTO>> getAllCategories();
    Response<?>  deleteCategoryById(Long id);




}
