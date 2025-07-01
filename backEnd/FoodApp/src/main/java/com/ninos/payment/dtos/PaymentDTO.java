package com.ninos.payment.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ninos.auth_users.dtos.UserDTO;
import com.ninos.enums.PaymentGateway;
import com.ninos.enums.PaymentStatus;
import com.ninos.order.dtos.OrderDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentDTO {

    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private String transactionId;
    private PaymentGateway paymentGateway;
    private String failureReasons;
    private LocalDateTime paymentDate;

    private OrderDTO order;
    private UserDTO user;

}
