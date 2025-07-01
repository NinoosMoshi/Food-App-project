package com.ninos.order.repository;

import com.ninos.auth_users.entity.User;
import com.ninos.enums.OrderStatus;
import com.ninos.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    List<Order> findByUserOrderByOrderDateDesc(User user);

    @Query("SELECT COUNT(DISTINCT o.user.id) FROM Order o")
    long countDistinctUsers();

//    It counts the number of unique users who have placed at least one order.
//    Order o → Refers to the Order entity with alias o.
//    o.user.id → Refers to the ID of the user associated with that order.
//    DISTINCT o.user.id → Ensures that each user is only counted once, even if they have multiple orders.
//    COUNT(...) → Counts the number of unique user IDs

//    🧠 Example Scenario:
//    Let's say the Order table has:
//
//    Order ID	User ID
//        1	      101
//        2	      102
//        3	      101
//        4	      103
//        5	      102
//
//    Then:
//
//    User 101: 2 orders
//    User 102: 2 orders
//    User 103: 1 order
//
//    countDistinctUsers() would return 3, because there are 3 unique users who placed orders.

}
