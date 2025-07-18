package com.ninos.order.services;

import com.ninos.auth_users.entity.User;
import com.ninos.auth_users.services.UserService;
import com.ninos.cart.entity.Cart;
import com.ninos.cart.entity.CartItem;
import com.ninos.cart.repository.CartRepository;
import com.ninos.cart.services.CartService;
import com.ninos.email_notification.dtos.NotificationDTO;
import com.ninos.email_notification.services.NotificationService;
import com.ninos.enums.OrderStatus;
import com.ninos.enums.PaymentStatus;
import com.ninos.exceptions.BadRequestException;
import com.ninos.exceptions.NotFoundException;
import com.ninos.menu.dtos.MenuDTO;
import com.ninos.order.dtos.OrderDTO;
import com.ninos.order.dtos.OrderItemDTO;
import com.ninos.order.entity.Order;
import com.ninos.order.entity.OrderItem;
import com.ninos.order.repository.OrderItemRepository;
import com.ninos.order.repository.OrderRepository;
import com.ninos.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements  OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final TemplateEngine templateEngine;

    @Value("${base.payment.link}")
    private String basePaymentLink;


    @Transactional
    @Override
    public Response<?> placeOrderFromCart() {
        log.info("inside placeOrderFromCart");

        User customer = userService.getCurrentLoggedInUser();

        String deliveryAddress = customer.getAddress();

        if (deliveryAddress == null) {
            throw new NotFoundException("Delivery Address Not present for the user");
        }
        Cart cart = cartRepository.findByUser_Id(customer.getId())
                .orElseThrow(() -> new NotFoundException("Cart Not present for the user"));

        List<CartItem> cartItems = cart.getCartItems();

        if (cartItems == null || cartItems.isEmpty()) throw new BadRequestException("Cart is empty");

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .menu(cartItem.getMenu())
                    .quantity(cartItem.getQuantity())
                    .pricePerUnit(cartItem.getPricePerUnit())
                    .subtotal(cartItem.getSubtotal())
                    .build();
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(cartItem.getSubtotal());
        }

        Order order = Order.builder()
                .user(customer)
                .orderItems(orderItems)
                .orderDate(LocalDateTime.now())
                .totalAmount(totalAmount)
                .orderStatus(OrderStatus.INITIALIZED)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        orderItems.forEach(orderItem -> orderItem.setOrder(savedOrder));
        orderItemRepository.saveAll(orderItems);

        // clear the user's cart after the order is placed
        cartService.clearShoppingCart();

        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);

        // send email notifications
        sendOrderConfirmationEmail(customer, orderDTO);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Your order has been received successfully, We've sent a secure payment link to your email. Please proceed for payment to confirm your order.")
                .build();
    }


    @Override
    public Response<OrderDTO> getOrderById(Long id) {
        log.info("inside getOrderById");

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("order id not found"));

        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);

        return Response.<OrderDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .data(orderDTO)
                .build();
    }

    @Override
    public Response<Page<OrderDTO>> getAllOrders(OrderStatus orderStatus, int page, int size) {
        log.info("inside getAllOrders");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Order> orderPage;

        if(orderStatus != null){
            orderPage = orderRepository.findByOrderStatus(orderStatus, pageable);
        }else{
            orderPage = orderRepository.findAll(pageable);
        }

        Page<OrderDTO> orderDTOPage = orderPage.map(order -> {
           OrderDTO dto = modelMapper.map(order, OrderDTO.class);
           dto.getOrderItems().forEach(orderItemDTO -> orderItemDTO.getMenu().setReviews(null));
           return dto;
        });

        return Response.<Page<OrderDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .data(orderDTOPage)
                .build();
    }

    @Override
    public Response<List<OrderDTO>> getOrdersOfUser() {
        log.info("inside getOrdersOfUser");

        User customer = userService.getCurrentLoggedInUser();
        List<Order> orders = orderRepository.findByUserOrderByOrderDateDesc(customer);

        List<OrderDTO> orderDTOS = orders.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .toList();

        orderDTOS.forEach(orderItem -> {
            orderItem.setUser(null);
            orderItem.getOrderItems().forEach(item -> item.getMenu().setReviews(null));
        });

        return Response.<List<OrderDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Orders for user retrieved successfully")
                .data(orderDTOS)
                .build();
    }

    @Override
    public Response<OrderItemDTO> getOrderItemById(Long orderItemId) {
        log.info("inside getOrderItemById");

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new NotFoundException("order item not found"));

        OrderItemDTO orderItemDTO = modelMapper.map(orderItem, OrderItemDTO.class);

        orderItemDTO.setMenu(modelMapper.map(orderItem.getMenu(), MenuDTO.class));

        return Response.<OrderItemDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("OrderItem retrieved successfully")
                .data(orderItemDTO)
                .build();
    }

    @Override
    public Response<OrderDTO> updateOrderStatus(OrderDTO orderDTO) {
        log.info("inside updateOrderStatus");

        Order order = orderRepository.findById(orderDTO.getId())
                .orElseThrow(() -> new NotFoundException("order id not found"));

        OrderStatus orderStatus = orderDTO.getOrderStatus();
        order.setOrderStatus(orderStatus);

        orderRepository.save(order);

        return Response.<OrderDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order status updated successfully")
                .build();
    }

    @Override
    public Response<Long> countUniqueCustomers() {
        log.info("inside countUniqueCustomers");

        long uniqueCustomerCount = orderRepository.countDistinctUsers();

        return Response.<Long>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Unique customer count retrieved successfully")
                .data(uniqueCustomerCount)
                .build();
    }


    private void sendOrderConfirmationEmail(User customer, OrderDTO orderDTO) {
        String subject = "Your Order Confirmation - Order #" + orderDTO.getId();

        // create a Thymeleaf context and set variables.
        Context context = new Context(Locale.getDefault());

        context.setVariable("customerName", customer.getName());
        context.setVariable("orderId", String.valueOf(orderDTO.getId()));
        context.setVariable("orderDate", orderDTO.getOrderDate().toString());
        context.setVariable("totalAmount", orderDTO.getTotalAmount().toString());

        // Format delivery address
        String deliveryAddress = orderDTO.getUser().getAddress();
        context.setVariable("deliveryAddress", deliveryAddress);

        context.setVariable("currentYear", java.time.Year.now());

        // build the order items HTML using StringBuilder
        StringBuilder orderItemsHtml = new StringBuilder();

        for (OrderItemDTO item : orderDTO.getOrderItems()) {
            orderItemsHtml.append("<div class=\"order-item\">")
                    .append("<p>").append(item.getMenu().getName()).append(" x ").append(item.getQuantity()).append("</p>")
                    .append("<p> $ ").append(item.getSubtotal()).append("</p>")
                    .append("</div>");
        }


            context.setVariable("orderItemsHtml", orderItemsHtml.toString());
            context.setVariable("totalItems", orderDTO.getOrderItems().size());

            String paymentLink = basePaymentLink + orderDTO.getId() + "&amount=" + orderDTO.getTotalAmount(); // replace "yourdomain.com"
            context.setVariable("paymentLink", paymentLink);

            // process the thymeleaf template go generate the HTML email body
            String emailBody = templateEngine.process("order-confirmation", context); // "order-confirmation" is the template name

            notificationService.sendEmail(NotificationDTO.builder()
                            .recipient(customer.getEmail())
                            .subject(subject)
                            .body(emailBody)
                            .isHtml(true)
                            .build());


    }


}
