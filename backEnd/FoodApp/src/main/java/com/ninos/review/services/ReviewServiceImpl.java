package com.ninos.review.services;

import com.ninos.auth_users.entity.User;
import com.ninos.auth_users.services.UserService;
import com.ninos.enums.OrderStatus;
import com.ninos.exceptions.BadRequestException;
import com.ninos.exceptions.NotFoundException;
import com.ninos.menu.entity.Menu;
import com.ninos.menu.repository.MenuRepository;
import com.ninos.order.entity.Order;
import com.ninos.order.repository.OrderItemRepository;
import com.ninos.order.repository.OrderRepository;
import com.ninos.response.Response;
import com.ninos.review.dtos.ReviewDTO;
import com.ninos.review.entity.Review;
import com.ninos.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements  ReviewService {

    private final ReviewRepository reviewRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ModelMapper modelMapper;
    private final UserService  userService;



    @Transactional
    @Override
    public Response<ReviewDTO> createReview(ReviewDTO reviewDTO) {
        log.info("inside createReview");

        // Get current User
        User user = userService.getCurrentLoggedInUser();

        // Validate required fields
        if(reviewDTO.getOrderId() == null || reviewDTO.getMenuId() == null) {
            throw new BadRequestException("Order ID and Menu ID cannot be null");
        }

        // Validate menu item exists
        Menu menu = menuRepository.findById(reviewDTO.getMenuId())
                .orElseThrow(() -> new NotFoundException("Menu not found"));


        // Validate order exists
        Order order = orderRepository.findById(reviewDTO.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));


        // Make sure the order belongs to you
        if(!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You are not the owner of this order");
        }

        // Validate order status is DELEVERED
        if(order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("You are not delivered to this order");
        }

        // Validate that menu item was part of this order
        boolean itemInOrder = orderItemRepository.existsByOrderIdAndMenuId(reviewDTO.getOrderId(), reviewDTO.getMenuId());

        if(!itemInOrder) {
            throw new BadRequestException("You are not the owner of this order");
        }

        // Check if user already wrote a review for the item
        if(reviewRepository.existsByUserIdAndMenuIdAndOrderId(
                user.getId(),
                reviewDTO.getMenuId(),
                reviewDTO.getOrderId()))
        {
            throw new BadRequestException("You are already reviewed this order");
        }


        // Create and save review
        Review review = Review.builder()
                .user(user)
                .menu(menu)
                .orderId(reviewDTO.getOrderId())
                .rating(reviewDTO.getRating())
                .comment(reviewDTO.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);

        // Return response with review data
        ReviewDTO responseDTO = modelMapper.map(savedReview, ReviewDTO.class);
        responseDTO.setUserName(user.getName());
        responseDTO.setMenuName(menu.getName());

        return Response.<ReviewDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Review added successfully")
                .data(responseDTO)
                .build();
    }

    @Override
    public Response<List<ReviewDTO>> getReviewsForMenu(Long menuId) {
        log.info("inside getReviewsForMenu");

        List<Review> reviews = reviewRepository.findByMenuIdOrderByIdDesc(menuId);

        List<ReviewDTO> reviewDTOS = reviews.stream()
                .map(review -> modelMapper.map(review, ReviewDTO.class))
                .toList();

        return Response.<List<ReviewDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Reviews retrieved successfully")
                .data(reviewDTOS)
                .build();
    }


    @Override
    public Response<Double> getAverageRating(Long menuId) {
        log.info("inside getAverageRating");

        Double averageRating = reviewRepository.calculateAverageRatingByMenuId(menuId);

        return Response.<Double>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Average rating retrieved successfully")
                .data(averageRating != null ? averageRating : 0.0)
                .build();
    }
}
