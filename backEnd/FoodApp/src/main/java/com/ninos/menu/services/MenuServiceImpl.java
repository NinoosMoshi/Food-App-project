package com.ninos.menu.services;

import com.ninos.aws.AWSS3Service;
import com.ninos.category.entity.Category;
import com.ninos.category.repository.CategoryRepository;
import com.ninos.exceptions.BadRequestException;
import com.ninos.exceptions.NotFoundException;
import com.ninos.menu.dtos.MenuDTO;
import com.ninos.menu.entity.Menu;
import com.ninos.menu.repository.MenuRepository;
import com.ninos.response.Response;
import com.ninos.review.dtos.ReviewDTO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final AWSS3Service awss3Service;

    @Override
    public Response<MenuDTO> createMenu(MenuDTO menuDTO) {
        log.info("Inside createMenu");

        Category category = categoryRepository.findById(menuDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        String imageUrl = null;

        MultipartFile imageFile = menuDTO.getImageFile();

        if(imageFile == null || imageFile.isEmpty()) {
            throw new BadRequestException("Menu image is required");
        }

        String imageName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
        URL s3Url = awss3Service.uploadFile("menus/" + imageName, imageFile);
        imageUrl = s3Url.toString();

        Menu menu = Menu.builder()
                .name(menuDTO.getName())
                .description(menuDTO.getDescription())
                .price(menuDTO.getPrice())
                .imageUrl(imageUrl)
                .category(category)
                .build();

        Menu savedMenu = menuRepository.save(menu);

        MenuDTO menuDTO1 = modelMapper.map(savedMenu, MenuDTO.class);

        return Response.<MenuDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu saved successfully")
                .data(menuDTO1)
                .build();
    }

    @Override
    public Response<MenuDTO> updateMenu(MenuDTO menuDTO) {
        log.info("Inside updateMenu");

        Menu existingMenu = menuRepository.findById(menuDTO.getId())
                .orElseThrow(() -> new NotFoundException("Menu not found"));

        Category category = categoryRepository.findById(menuDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        String imageUrl = existingMenu.getImageUrl();
        MultipartFile imageFile = menuDTO.getImageFile();

        // Check if a new imageFile was provided
        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete the old image from S3 if it exists
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String keyName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                awss3Service.deleteFile("menus/" + keyName);

                log.info("Deleted old menu image from s3");
            }
            //upload new image
            String imageName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
            URL newImageUrl = awss3Service.uploadFile("menus/" + imageName, imageFile);
            imageUrl = newImageUrl.toString();
        }

        if(menuDTO.getName() != null && !menuDTO.getName().isBlank()) existingMenu.setName(menuDTO.getName());
        if(menuDTO.getDescription() != null && !menuDTO.getDescription().isBlank()) existingMenu.setDescription(menuDTO.getDescription());
        if(menuDTO.getPrice() != null) existingMenu.setPrice(menuDTO.getPrice());

        existingMenu.setImageUrl(imageUrl);
        existingMenu.setCategory(category);

        Menu updatedMenu = menuRepository.save(existingMenu);
        MenuDTO menuDTO1 = modelMapper.map(updatedMenu, MenuDTO.class);

        return Response.<MenuDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu updated successfully")
                .data(menuDTO1)
                .build();
    }

    @Override
    public Response<MenuDTO> getMenuById(Long id) {
        log.info("Inside getMenuById");

        Menu existingMenu = menuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu not found"));

        MenuDTO menuDTO = modelMapper.map(existingMenu, MenuDTO.class);

        // Sort the reviews in descending order
        if(menuDTO.getReviews() != null){
            menuDTO.getReviews().sort(Comparator.comparing(ReviewDTO::getId).reversed()); // get the id of review by DESC
        }

        return Response.<MenuDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu retrieved successfully")
                .data(menuDTO)
                .build();
    }


    @Override
    public Response<?> deleteMenuById(Long id) {
        log.info("Inside deleteMenuById");

        Menu existingMenu = menuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu not found"));

        // delete the image from s3 if it exists
        String imageUrl = existingMenu.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String keyName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            awss3Service.deleteFile("menus/" + keyName);
            log.info("Deleted image from s3: menus/" + keyName);
        }

        menuRepository.deleteById(id);

        return Response.<MenuDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu deleted successfully")
                .build();
    }

    @Override
    public Response<List<MenuDTO>> getMenus(Long categoryId, String search) {
        log.info("Inside getMenus");

        Specification<Menu> spec = buildSpecification(categoryId, search);
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        List<Menu> menuList = menuRepository.findAll(spec, sort);
        List<MenuDTO> menuDTOS = menuList.stream()
                .map(item -> modelMapper.map(item, MenuDTO.class)).toList();

        return Response.<List<MenuDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menus retrieved successfully")
                .data(menuDTOS)
                .build();
    }

    private Specification<Menu> buildSpecification(Long categoryId, String search) {
        return (root, query, cb) -> {

            // // Create a list to store all filtering conditions
            List<Predicate> predicates = new ArrayList<>();

            // If categoryId is provided, add a filter condition for category
            if (categoryId != null) {
                // Add condition: menu.category.id == categoryId
                predicates.add(cb.equal(
                        root.get("category").get("id"), // navigate to menu.category.id
                        categoryId                      // match with the given categoryId
                ));
            }

            // If a search keyword is provided and not blank, filter by name or description
            if (search != null && !search.isBlank()) {
                // Prepare the search term with wildcards and lowercase for case-insensitive matching
                String searchTerm = "%" + search.toLowerCase() + "%";

                // Add condition: (LOWER(menu.name) LIKE %searchTerm% OR LOWER(menu.description) LIKE %searchTerm%)
                predicates.add(cb.or(
                        cb.like(
                                cb.lower(root.get("name")), // Convert name to lowercase
                                searchTerm                  // apply LIKE condition (Match against search term)
                        ),
                        cb.like(
                                cb.lower(root.get("description")), // Convert description to lowercase
                                searchTerm                        // apply LIKE condition (Match against search term)
                        )
                ));
            }

            // Combine all the conditions using AND operator
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
