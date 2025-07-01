package com.ninos.cart.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ninos.cart.entity.Cart;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartItemDTO {

    private Long id;

    private Cart cart;


    private MenuDTO menu;

    private int quantity;

    private BigDecimal pricePerUnit;

    private BigDecimal subtotal;

}
