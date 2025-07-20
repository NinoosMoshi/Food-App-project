package com.ninos.payment.services;

import com.ninos.email_notification.dtos.NotificationDTO;
import com.ninos.email_notification.services.NotificationService;
import com.ninos.enums.OrderStatus;
import com.ninos.enums.PaymentGateway;
import com.ninos.enums.PaymentStatus;
import com.ninos.exceptions.BadRequestException;
import com.ninos.exceptions.NotFoundException;
import com.ninos.order.entity.Order;
import com.ninos.order.repository.OrderRepository;
import com.ninos.payment.dtos.PaymentDTO;
import com.ninos.payment.entity.Payment;
import com.ninos.payment.repository.PaymentRepository;
import com.ninos.response.Response;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService{

    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final TemplateEngine templateEngine;
    private final ModelMapper modelMapper;

    @Value("${stripe.api.secret.key}")
    private String secretKey;

    @Value("${frontend.base.url}")
    private String frontendBaseUrl;


    @Override
    public Response<?> initializePayment(PaymentDTO paymentDTO) {
        log.info("inside initializePayment");

        Stripe.apiKey = secretKey;

        Long orderId =  paymentDTO.getOrderId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("order not found"));

        if(order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new BadRequestException("order already completed");
        }
        if(paymentDTO.getAmount() == null) {
            throw new BadRequestException("amount is required");
        }
        if(order.getTotalAmount().compareTo(paymentDTO.getAmount()) != 0) {
            throw new BadRequestException("Payment amount does not tally. please contact out customer support agent");
        }

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(paymentDTO.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) // converting to cent in usd
                    .setCurrency("usd")
                    .putMetadata("orderId", String.valueOf(orderId))
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            String uniqueTransactionId = intent.getClientSecret();

            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Payment created successfully")
                    .data(uniqueTransactionId)
                    .build();

        }catch (Exception e) {
            throw new RuntimeException("Error Creating Payment unique transaction id");
        }
    }



    @Override
    public void updatePaymentForOrder(PaymentDTO paymentDTO) {
        log.info("inside updatePaymentForOrder");

        Long orderId =  paymentDTO.getOrderId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("order not found"));

        // Build payment entity to save
        Payment payment = new Payment();
        payment.setPaymentGateway(PaymentGateway.STRIPE);
        payment.setAmount(paymentDTO.getAmount());
        payment.setTransactionId(paymentDTO.getTransactionId());
        payment.setPaymentStatus(paymentDTO.isSuccess() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setOrder(order);

        if(!paymentDTO.isSuccess()) {
            payment.setFailureReason(paymentDTO.getFailureReason());
        }

        paymentRepository.save(payment);

        // Prepare email context
        Context context = new Context(Locale.getDefault());
        context.setVariable("customerName", order.getUser().getName());
        context.setVariable("orderId", order.getId());
        context.setVariable("currentYear", Year.now().getValue());
        context.setVariable("amount", "$" + paymentDTO.getAmount());

        if (paymentDTO.isSuccess()) {
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);


            log.info("PAYMENT IS SUCCESSFUL ABOUT TO SEND EMAIL");

            // Add success-specific variables
            context.setVariable("transactionId", paymentDTO.getTransactionId());
            context.setVariable("paymentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")));
            context.setVariable("frontendBaseUrl", this.frontendBaseUrl);

            String emailBody = templateEngine.process("payment-success", context);

            log.info("HAVE GOTTEN TEMPLATE");

            notificationService.sendEmail(NotificationDTO.builder()
                    .recipient(order.getUser().getEmail())
                    .subject("Payment Successful - Order #" + order.getId())
                    .body(emailBody)
                    .isHtml(true)
                    .build());
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);


            log.info("PAYMENT IS FAILED ABOUT TO SEND EMAIL");
            // Add failure-specific variables
            context.setVariable("failureReason", paymentDTO.getFailureReason());

            String emailBody = templateEngine.process("payment-failed", context);

            notificationService.sendEmail(NotificationDTO.builder()
                    .recipient(order.getUser().getEmail())
                    .subject("Payment Failed - Order #" + order.getId())
                    .body(emailBody)
                    .isHtml(true)
                    .build());
        }

    }

    @Override
    public Response<List<PaymentDTO>> getAllPayments() {
        log.info("inside getAllPayments");

        List<Payment> paymentList = paymentRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<PaymentDTO> paymentDTOS = modelMapper.map(paymentList, new  TypeToken<List<PaymentDTO>>() {}.getType());

        paymentDTOS.forEach(item -> {
            item.setOrder(null);
            item.setUser(null);
        });

        return Response.<List<PaymentDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("payments retrieved successfully by id")
                .data(paymentDTOS)
                .build();
    }



    @Override
    public Response<PaymentDTO> getPaymentById(Long paymentId) {
        log.info("inside getPaymentById");

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("payment not found"));
        PaymentDTO paymentDTO = modelMapper.map(payment, PaymentDTO.class);

        paymentDTO.getUser().setRoles(null);
        paymentDTO.getOrder().setUser(null);
        paymentDTO.getOrder().getOrderItems().forEach(item -> {
            item.getMenu().setReviews(null);
        });

        return Response.<PaymentDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("payment retrieved successfully by id")
                .data(paymentDTO)
                .build();
    }
}
