package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Order;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    /**
     * Tìm order theo order number
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Kiểm tra order number đã tồn tại chưa
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Tìm tất cả orders của một customer
     */
    Page<Order> findByCustomer(User customer, Pageable pageable);

    /**
     * Tìm orders theo customer ID
     */
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId")
    Page<Order> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Tìm orders theo status
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Tìm orders theo customer và status
     */
    Page<Order> findByCustomerAndStatus(User customer, OrderStatus status, Pageable pageable);

    /**
     * Tìm orders trong khoảng thời gian
     */
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Page<Order> findByOrderDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Tìm orders theo customer trong khoảng thời gian
     */
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId AND o.orderDate BETWEEN :startDate AND :endDate")
    Page<Order> findByCustomerIdAndOrderDateBetween(
            @Param("customerId") UUID customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Đếm số orders theo status
     */
    long countByStatus(OrderStatus status);

    /**
     * Đếm số orders của customer
     */
    long countByCustomer(User customer);

    /**
     * Tính tổng doanh thu theo status
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
    Double sumTotalAmountByStatus(@Param("status") OrderStatus status);

    /**
     * Tính tổng doanh thu trong khoảng thời gian
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses")
    Double sumTotalAmountByDateRangeAndStatuses(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statuses") List<OrderStatus> statuses
    );

    /**
     * Tìm orders cần xử lý (PENDING, CONFIRMED)
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'CONFIRMED') ORDER BY o.orderDate ASC")
    List<Order> findPendingOrders();

    /**
     * Tìm orders đang giao hàng
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('PROCESSING', 'PACKED', 'SHIPPED') ORDER BY o.orderDate ASC")
    List<Order> findInProgressOrders();

    /**
     * Tìm orders theo booking ID
     */
    Optional<Order> findByBookingId(String bookingId);

    /**
     * Tìm orders theo shopping cart ID
     */
    Optional<Order> findByShoppingCartId(String shoppingCartId);

    /**
     * Tìm top customers theo tổng giá trị đơn hàng
     */
    @Query("SELECT o.customer, SUM(o.totalAmount) as total FROM Order o " +
           "WHERE o.status = 'COMPLETED' " +
           "GROUP BY o.customer " +
           "ORDER BY total DESC")
    Page<Object[]> findTopCustomersByTotalAmount(Pageable pageable);

    /**
     * Thống kê orders theo ngày
     */
    @Query("SELECT CAST(o.orderDate AS date) as orderDate, COUNT(o) as count, SUM(o.totalAmount) as total " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "GROUP BY CAST(o.orderDate AS date) " +
           "ORDER BY CAST(o.orderDate AS date) DESC")
    List<Object[]> getOrderStatisticsByDate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Tìm orders theo payment status
     */
    @Query("SELECT o FROM Order o WHERE o.paymentInfo.paymentStatus = :paymentStatus")
    Page<Order> findByPaymentStatus(
            @Param("paymentStatus") com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentStatus paymentStatus,
            Pageable pageable
    );
}
