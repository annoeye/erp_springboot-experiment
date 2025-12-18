package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Order;
import com.anno.ERP_SpringBoot_Experiment.model.entity.OrderItem;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Tìm tất cả items của một order
     */
    List<OrderItem> findByOrder(Order order);

    /**
     * Tìm tất cả items của một order theo order ID
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") UUID orderId);

    /**
     * Tìm tất cả orders có chứa product
     */
    List<OrderItem> findByProduct(Product product);

    /**
     * Tìm tất cả orders có chứa product theo product ID
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId")
    List<OrderItem> findByProductId(@Param("productId") UUID productId);

    /**
     * Thống kê sản phẩm bán chạy nhất
     */
    @Query("SELECT oi.product, SUM(oi.quantity) as totalQuantity " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE o.status = 'COMPLETED' " +
           "GROUP BY oi.product " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findBestSellingProducts();

    /**
     * Tính tổng số lượng đã bán của một product
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE oi.product.id = :productId AND o.status = 'COMPLETED'")
    Long sumQuantitySoldByProductId(@Param("productId") UUID productId);

    /**
     * Tính tổng doanh thu của một product
     */
    @Query("SELECT SUM(oi.subtotal) FROM OrderItem oi " +
           "JOIN oi.order o " +
           "WHERE oi.product.id = :productId AND o.status = 'COMPLETED'")
    Double sumRevenueByProductId(@Param("productId") UUID productId);
}
