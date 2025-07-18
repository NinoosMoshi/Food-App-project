package com.ninos.review.services;

import com.ninos.response.Response;
import com.ninos.review.dtos.ReviewDTO;

import java.util.List;

public interface ReviewService {

    Response<ReviewDTO> createReview(ReviewDTO reviewDTO);
    Response<List<ReviewDTO>> getReviewsForMenu(Long menuId);
    Response<Double> getAverageRating(Long menuId);

}
