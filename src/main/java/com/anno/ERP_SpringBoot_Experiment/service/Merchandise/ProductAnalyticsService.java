//package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;
//
//import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
//import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
//import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
//import com.anno.ERP_SpringBoot_Experiment.repository.OrderItemRepository;
//import com.anno.ERP_SpringBoot_Experiment.repository.ProductRepository;
//import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductAnalyticsDto;
//import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
//import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.List;
//import java.util.UUID;
//
///**
// * Service để tính toán và lấy analytics cho Product
// * Phục vụ cho Dashboard API
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ProductAnalyticsService {
//
//    private final ProductRepository productRepository;
//    private final AttributesRepository attributesRepository;
//    private final OrderItemRepository orderItemRepository;
//    private final Helper merchandiseHelper;
//
//    /**
//     * Lấy analytics tổng hợp cho một sản phẩm
//     */
//    public Response<ProductAnalyticsDto> getProductAnalytics(String productId) {
//        UUID uuid = merchandiseHelper.convertStringToUUID(productId);
//
//        Product product = productRepository.findById(uuid)
//                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại"));
//
//        // Tính toán các chỉ số từ database (real-time)
//        Integer totalSoldQuantity = product.getTotalSoldQuantity() != null
//                ? product.getTotalSoldQuantity()
//                : 0;
//        Double totalRevenue = product.getTotalRevenue() != null
//                ? product.getTotalRevenue()
//                : 0.0;
//        Integer totalOrders = product.getTotalOrders() != null
//                ? product.getTotalOrders()
//                : 0;
//
//        // Tính tồn kho tổng từ tất cả Attributes
//        List<Attributes> attributesList = attributesRepository.findAllByProductAndAuditInfo_DeletedAtIsNull(product);
//        int currentStock = attributesList.stream()
//                .mapToInt(Attributes::getStockQuantity)
//                .sum();
//
//        // Tính sell-through rate
//        double sellThroughRate = (totalSoldQuantity + currentStock) > 0
//                ? (double) totalSoldQuantity / (totalSoldQuantity + currentStock) * 100
//                : 0.0;
//
//        // Xác định stock status
//        String stockStatus = determineStockStatus(currentStock, totalSoldQuantity);
//
//        // Tính tỉ lệ hủy đơn và hoàn trả
//        Integer cancelledOrders = orderItemRepository.countCancelledOrdersByProductId(uuid);
//        Integer returnedOrders = orderItemRepository.countReturnedOrdersByProductId(uuid);
//        int totalAllOrders = totalOrders + (cancelledOrders != null ? cancelledOrders : 0)
//                + (returnedOrders != null ? returnedOrders : 0);
//
//        double cancellationRate = totalAllOrders > 0
//                ? (double) (cancelledOrders != null ? cancelledOrders : 0) / totalAllOrders * 100
//                : 0.0;
//        double returnRate = totalAllOrders > 0
//                ? (double) (returnedOrders != null ? returnedOrders : 0) / totalAllOrders * 100
//                : 0.0;
//
//        // Tính conversion rate (views -> orders)
//        Integer viewCount = product.getViewCount() != null ? product.getViewCount() : 0;
//        double conversionRate = viewCount > 0
//                ? (double) totalOrders / viewCount * 100
//                : 0.0;
//
//        // Tính profit margin từ Attributes (trung bình)
//        double avgProfitMargin = calculateAverageProfitMargin(attributesList);
//
//        // Tính average order value
//        double averageOrderValue = totalOrders > 0
//                ? totalRevenue / totalOrders
//                : 0.0;
//
//        // Tính doanh thu theo thời gian
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime startOfToday = now.with(LocalTime.MIN);
//        LocalDateTime startOfWeek = now.minusDays(7);
//        LocalDateTime startOfMonth = now.withDayOfMonth(1).with(LocalTime.MIN);
//        LocalDateTime startOfLastMonth = now.minusMonths(1).withDayOfMonth(1).with(LocalTime.MIN);
//        LocalDateTime endOfLastMonth = now.withDayOfMonth(1).with(LocalTime.MIN).minusSeconds(1);
//
//        Double revenueToday = orderItemRepository.sumRevenueByProductIdAndPeriod(uuid, startOfToday, now);
//        Double revenueThisWeek = orderItemRepository.sumRevenueByProductIdAndPeriod(uuid, startOfWeek, now);
//        Double revenueThisMonth = orderItemRepository.sumRevenueByProductIdAndPeriod(uuid, startOfMonth, now);
//        Double revenueLastMonth = orderItemRepository.sumRevenueByProductIdAndPeriod(uuid, startOfLastMonth,
//                endOfLastMonth);
//
//        // Tính % thay đổi so với tháng trước
//        double revenueChangePercent = (revenueLastMonth != null && revenueLastMonth > 0)
//                ? ((revenueThisMonth != null ? revenueThisMonth : 0) - revenueLastMonth) / revenueLastMonth * 100
//                : 0.0;
//
//        ProductAnalyticsDto dto = ProductAnalyticsDto.builder()
//                // Basic info
//                .productId(product.getId())
//                .productSku(product.getSkuInfo().getSku())
//                .productName(product.getSkuInfo().getName())
//                // Sales metrics
//                .totalSoldQuantity(totalSoldQuantity)
//                .totalRevenue(totalRevenue)
//                .netRevenue(totalRevenue) // TODO: Trừ discount nếu có
//                .totalOrders(totalOrders)
//                .averageOrderValue(averageOrderValue)
//                // Inventory metrics
//                .currentStock(currentStock)
//                .sellThroughRate(sellThroughRate)
//                .stockStatus(stockStatus)
//                // Performance metrics
//                .returnRate(returnRate)
//                .cancellationRate(cancellationRate)
//                .conversionRate(conversionRate)
//                .profitMargin(avgProfitMargin)
//                // Engagement metrics
//                .viewCount(viewCount)
//                .averageRating(product.getAverageRating() != null ? product.getAverageRating() : 0.0)
//                .reviewCount(product.getReviewCount() != null ? product.getReviewCount() : 0)
//                // Time-based metrics
//                .revenueToday(revenueToday != null ? revenueToday : 0.0)
//                .revenueThisWeek(revenueThisWeek != null ? revenueThisWeek : 0.0)
//                .revenueThisMonth(revenueThisMonth != null ? revenueThisMonth : 0.0)
//                .revenueChangePercent(revenueChangePercent)
//                .build();
//
//        log.info("Retrieved analytics for product: {}", product.getSkuInfo().getSku());
//        return Response.ok(dto);
//    }
//
//    /**
//     * Tăng view count cho product
//     */
//    public void incrementViewCount(String productId) {
//        UUID uuid = merchandiseHelper.convertStringToUUID(productId);
//        Product product = productRepository.findById(uuid)
//                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại"));
//
//        product.setViewCount((product.getViewCount() != null ? product.getViewCount() : 0) + 1);
//        productRepository.save(product);
//        log.debug("Incremented view count for product: {}", productId);
//    }
//
//    /**
//     * Xác định trạng thái tồn kho
//     */
//    private String determineStockStatus(int currentStock, int soldQuantity) {
//        if (currentStock == 0) {
//            return "OUT_OF_STOCK";
//        }
//        // Low stock nếu tồn kho < 20% so với số đã bán
//        if (soldQuantity > 0 && currentStock < soldQuantity * 0.2) {
//            return "LOW_STOCK";
//        }
//        // Low stock nếu tồn kho < 10 đơn vị
//        if (currentStock < 10) {
//            return "LOW_STOCK";
//        }
//        return "IN_STOCK";
//    }
//
//    /**
//     * Tính biên lợi nhuận trung bình từ các Attributes
//     */
//    private double calculateAverageProfitMargin(List<Attributes> attributesList) {
//        if (attributesList.isEmpty()) {
//            return 0.0;
//        }
//
//        double totalMargin = 0.0;
//        int count = 0;
//
//        for (Attributes attr : attributesList) {
//            Double costPrice = attr.getCostPrice();
//            double salePrice = attr.getSalePrice();
//
//            if (costPrice != null && costPrice > 0 && salePrice > 0) {
//                double margin = (salePrice - costPrice) / salePrice * 100;
//                totalMargin += margin;
//                count++;
//            }
//        }
//
//        return count > 0 ? totalMargin / count : 0.0;
//    }
//}
