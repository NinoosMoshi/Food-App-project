package com.ninos.category.services;

import com.ninos.category.dtos.CategoryDTO;
import com.ninos.category.entity.Category;
import com.ninos.category.repository.CategoryRepository;
import com.ninos.exceptions.NotFoundException;
import com.ninos.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements  CategoryService {

    private final CategoryRepository repository;
    private final ModelMapper modelMapper;
    private final CategoryRepository categoryRepository;


    @Override
    public Response<CategoryDTO> addCategory(CategoryDTO categoryDTO) {

        log.info("Inside addCategory");

        Category category = modelMapper.map(categoryDTO, Category.class);
        categoryRepository.save(category);

        return Response.<CategoryDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Category added successfully")
                .build();
    }

    @Override
    public Response<CategoryDTO> updateCategory(CategoryDTO categoryDTO) {

        log.info("Inside updateCategory");

        Category category = categoryRepository.findById(categoryDTO.getId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if(categoryDTO.getName() != null && !categoryDTO.getName().isEmpty()){
            category.setName(categoryDTO.getName());
        }
        if(categoryDTO.getDescription() != null){
            category.setDescription(categoryDTO.getDescription());
        }

        categoryRepository.save(category);

        return Response.<CategoryDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Category updated successfully")
                .build();
    }

    @Override
    public Response<CategoryDTO> getCategoryById(Long id) {

        log.info("Inside getCategoryById");

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        CategoryDTO categoryDTO = modelMapper.map(category, CategoryDTO.class);

        return Response.<CategoryDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(categoryDTO)
                .build();
    }

    @Override
    public Response<List<CategoryDTO>> getAllCategories() {

        log.info("Inside getAllCategories");

        List<Category> categories = categoryRepository.findAll();

        List<CategoryDTO> categoryDTOS = categories.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();

        return Response.<List<CategoryDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Categories retrieved successfully")
                .data(categoryDTOS)
                .build();
    }


    @Override
    public Response<?> deleteCategoryById(Long id) {

        log.info("Inside deleteCategory()");

        if (!categoryRepository.existsById(id)){
            throw  new NotFoundException("Category Not Found");
        }
        categoryRepository.deleteById(id);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Category deleted successfully")
                .build();
    }
}
