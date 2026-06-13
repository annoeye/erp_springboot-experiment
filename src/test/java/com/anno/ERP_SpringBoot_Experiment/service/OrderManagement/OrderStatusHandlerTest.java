package com.anno.ERP_SpringBoot_Experiment.service.OrderManagement;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Order;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests cho OrderStatusHandler — state machine của đơn hàng.
 * Kiểm tra tất cả các chuyển trạng thái hợp lệ và không hợp lệ.
 */
@DisplayName("OrderStatusHandler — State Machine Tests")
class OrderStatusHandlerTest {

    private OrderStatusHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderStatusHandler();
    }

    // ==================== Helper ====================

    /**
     * Tạo một Order với lịch sử trạng thái cho trước.
     * Trạng thái hiện tại = phần tử cuối cùng của history.
     */
    private Order createOrderWithStatus(OrderStatus... statuses) {
        Order order = new Order();
        List<OrderStatus> list = new ArrayList<>();
        for (OrderStatus s : statuses) {
            list.add(s);
        }
        order.setStatus(list);
        return order;
    }

    // ==================== getCurrentStatus ====================

    @Nested
    @DisplayName("getCurrentStatus()")
    class GetCurrentStatusTests {

        @Test
        @DisplayName("Should return PENDING when status list is null")
        void shouldReturnPending_whenStatusListIsNull() {
            Order order = new Order();
            order.setStatus(null);
            assertEquals(OrderStatus.PENDING, handler.getCurrentStatus(order));
        }

        @Test
        @DisplayName("Should return PENDING when status list is empty")
        void shouldReturnPending_whenStatusListIsEmpty() {
            Order order = new Order();
            order.setStatus(new ArrayList<>());
            assertEquals(OrderStatus.PENDING, handler.getCurrentStatus(order));
        }

        @Test
        @DisplayName("Should return last element of status history")
        void shouldReturnLastStatus() {
            Order order = createOrderWithStatus(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);
            assertEquals(OrderStatus.PROCESSING, handler.getCurrentStatus(order));
        }
    }

    // ==================== transitionTo - Valid ====================

    @Nested
    @DisplayName("transitionTo() — Valid transitions")
    class ValidTransitionsTests {

        @ParameterizedTest(name = "{0} → {1}")
        @MethodSource("com.anno.ERP_SpringBoot_Experiment.service.OrderManagement.OrderStatusHandlerTest#validTransitionsProvider")
        @DisplayName("Should allow valid transition")
        void shouldAllowValidTransition(OrderStatus from, OrderStatus to) {
            Order order = createOrderWithStatus(from);
            handler.transitionTo(order, to, "test transition");
            assertEquals(to, handler.getCurrentStatus(order));
            assertEquals(2, order.getStatus().size()); // original + new
        }

        @Test
        @DisplayName("Should append status, not replace")
        void shouldAppendStatus() {
            Order order = createOrderWithStatus(OrderStatus.PENDING);
            handler.transitionTo(order, OrderStatus.CONFIRMED, "confirm");
            handler.transitionTo(order, OrderStatus.PROCESSING, "process");

            assertEquals(OrderStatus.PROCESSING, handler.getCurrentStatus(order));
            assertEquals(3, order.getStatus().size());
            assertEquals(OrderStatus.PENDING, order.getStatus().get(0));
            assertEquals(OrderStatus.CONFIRMED, order.getStatus().get(1));
            assertEquals(OrderStatus.PROCESSING, order.getStatus().get(2));
        }
    }

    // ==================== transitionTo - Invalid ====================

    @Nested
    @DisplayName("transitionTo() — Invalid transitions")
    class InvalidTransitionsTests {

        @ParameterizedTest(name = "{0} → {1}")
        @MethodSource("com.anno.ERP_SpringBoot_Experiment.service.OrderManagement.OrderStatusHandlerTest#invalidTransitionsProvider")
        @DisplayName("Should throw BusinessException for invalid transition")
        void shouldThrowException_forInvalidTransition(OrderStatus from, OrderStatus to) {
            Order order = createOrderWithStatus(from);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> handler.transitionTo(order, to, "invalid"));
            assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
            assertEquals(from, handler.getCurrentStatus(order)); // status unchanged
        }

        @Test
        @DisplayName("Should not allow transition from terminal state")
        void shouldNotAllowTransition_fromTerminalState() {
            List<OrderStatus> terminalStates = List.of(
                    OrderStatus.COMPLETED, OrderStatus.CANCELLED,
                    OrderStatus.FAILED, OrderStatus.REFUNDED
            );

            for (OrderStatus terminal : terminalStates) {
                Order order = createOrderWithStatus(terminal);
                for (OrderStatus any : OrderStatus.values()) {
                    if (any == terminal) continue; // same state not in allowed set
                    assertThrows(BusinessException.class,
                            () -> handler.transitionTo(order, any, "try from terminal"),
                            "Should reject transition from " + terminal + " to " + any);
                }
            }
        }

        @Test
        @DisplayName("Should not allow self-transition")
        void shouldNotAllowSelfTransition() {
            for (OrderStatus status : OrderStatus.values()) {
                Order order = createOrderWithStatus(status);
                assertThrows(BusinessException.class,
                        () -> handler.transitionTo(order, status, "self"),
                        "Should reject self-transition for " + status);
            }
        }
    }

    // ==================== isTerminal ====================

    @Nested
    @DisplayName("isTerminal()")
    class IsTerminalTests {

        @ParameterizedTest
        @MethodSource("com.anno.ERP_SpringBoot_Experiment.service.OrderManagement.OrderStatusHandlerTest#terminalStatesProvider")
        @DisplayName("Should return true for terminal states")
        void shouldReturnTrue_forTerminalStates(OrderStatus status) {
            assertTrue(handler.isTerminal(status));
        }

        @ParameterizedTest
        @MethodSource("com.anno.ERP_SpringBoot_Experiment.service.OrderManagement.OrderStatusHandlerTest#nonTerminalStatesProvider")
        @DisplayName("Should return false for non-terminal states")
        void shouldReturnFalse_forNonTerminalStates(OrderStatus status) {
            assertFalse(handler.isTerminal(status));
        }
    }

    // ==================== isValidTransition ====================

    @Nested
    @DisplayName("isValidTransition()")
    class IsValidTransitionTests {

        @ParameterizedTest(name = "{0} → {1}")
        @MethodSource("com.anno.ERP_SpringBoot_Experiment.service.OrderManagement.OrderStatusHandlerTest#validTransitionsProvider")
        @DisplayName("Should return true for valid transitions")
        void shouldReturnTrue_forValidTransition(OrderStatus from, OrderStatus to) {
            assertTrue(handler.isValidTransition(from, to));
        }

        @ParameterizedTest(name = "{0} → {1}")
        @MethodSource("com.anno.ERP_SpringBoot_Experiment.service.OrderManagement.OrderStatusHandlerTest#invalidTransitionsProvider")
        @DisplayName("Should return false for invalid transitions")
        void shouldReturnFalse_forInvalidTransition(OrderStatus from, OrderStatus to) {
            assertFalse(handler.isValidTransition(from, to));
        }
    }
    // ==================== Full lifecycle test ====================

    @Nested
    @DisplayName("Full lifecycle — Happy path")
    class FullLifecycleTests {

        @Test
        @DisplayName("Should complete full order lifecycle (COD)")
        void shouldCompleteFullLifecycle_COD() {
            // COD tự động: PENDING → CONFIRMED → PROCESSING
            Order order = createOrderWithStatus(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING);

            // PROCESSING → SHIPPING
            handler.transitionTo(order, OrderStatus.SHIPPING, "giao cho tài xế");
            assertEquals(OrderStatus.SHIPPING, handler.getCurrentStatus(order));

            // SHIPPING → DELIVERED
            handler.transitionTo(order, OrderStatus.DELIVERED, "giao thành công");
            assertEquals(OrderStatus.DELIVERED, handler.getCurrentStatus(order));

            // DELIVERED → COMPLETED
            handler.transitionTo(order, OrderStatus.COMPLETED, "hoàn thành");
            assertEquals(OrderStatus.COMPLETED, handler.getCurrentStatus(order));
            assertTrue(handler.isTerminal(OrderStatus.COMPLETED));
        }

        @Test
        @DisplayName("Should complete full order lifecycle (Pickup)")
        void shouldCompleteFullLifecycle_Pickup() {
            Order order = createOrderWithStatus(OrderStatus.PENDING, OrderStatus.CONFIRMED);

            // CONFIRMED → PROCESSING
            handler.transitionTo(order, OrderStatus.PROCESSING, "");
            // PROCESSING → READY_FOR_PICKUP
            handler.transitionTo(order, OrderStatus.READY_FOR_PICKUP, "sẵn sàng");
            // READY_FOR_PICKUP → DELIVERED (pickup)
            handler.transitionTo(order, OrderStatus.DELIVERED, "khách lấy");
            // DELIVERED → COMPLETED
            handler.transitionTo(order, OrderStatus.COMPLETED, "");
            assertEquals(OrderStatus.COMPLETED, handler.getCurrentStatus(order));
        }

        @Test
        @DisplayName("Should complete full return-refund lifecycle")
        void shouldCompleteFullReturnRefundLifecycle() {
            Order order = createOrderWithStatus(OrderStatus.DELIVERED);

            // DELIVERED → RETURNING
            handler.transitionTo(order, OrderStatus.RETURNING, "khách trả hàng");
            assertEquals(OrderStatus.RETURNING, handler.getCurrentStatus(order));

            // RETURNING → RETURNED
            handler.transitionTo(order, OrderStatus.RETURNED, "đã nhận lại hàng");
            assertEquals(OrderStatus.RETURNED, handler.getCurrentStatus(order));

            // RETURNED → REFUNDED
            handler.transitionTo(order, OrderStatus.REFUNDED, "đã hoàn tiền");
            assertEquals(OrderStatus.REFUNDED, handler.getCurrentStatus(order));
            assertTrue(handler.isTerminal(OrderStatus.REFUNDED));
        }

        @Test
        @DisplayName("Should handle cancellation from non-terminal states")
        void shouldHandleCancellationFromValidStates() {
            // Có thể hủy từ: PENDING, WAITING_PAYMENT, CONFIRMED, PROCESSING
            for (OrderStatus from : List.of(OrderStatus.PENDING, OrderStatus.WAITING_PAYMENT,
                    OrderStatus.CONFIRMED, OrderStatus.PROCESSING)) {
                Order order = createOrderWithStatus(from);
                handler.transitionTo(order, OrderStatus.CANCELLED, "khách hủy");
                assertEquals(OrderStatus.CANCELLED, handler.getCurrentStatus(order));
            }
        }

        @Test
        @DisplayName("Should handle delay and recovery")
        void shouldHandleDelayAndRecovery() {
            Order order = createOrderWithStatus(OrderStatus.PROCESSING);

            // PROCESSING → SHIPPING
            handler.transitionTo(order, OrderStatus.SHIPPING, "");
            // SHIPPING → DELAYED
            handler.transitionTo(order, OrderStatus.DELAYED, "kẹt xe");
            assertEquals(OrderStatus.DELAYED, handler.getCurrentStatus(order));
            // DELAYED → SHIPPING (recovery)
            handler.transitionTo(order, OrderStatus.SHIPPING, "đã thông thoáng");
            assertEquals(OrderStatus.SHIPPING, handler.getCurrentStatus(order));
        }
    }

    // ==================== Parameterized Data Providers ====================

    static Stream<Arguments> validTransitionsProvider() {
        return Stream.of(
                // PENDING transitions
                Arguments.of(OrderStatus.PENDING, OrderStatus.CONFIRMED),
                Arguments.of(OrderStatus.PENDING, OrderStatus.WAITING_PAYMENT),
                Arguments.of(OrderStatus.PENDING, OrderStatus.CANCELLED),
                // WAITING_PAYMENT transitions
                Arguments.of(OrderStatus.WAITING_PAYMENT, OrderStatus.CONFIRMED),
                Arguments.of(OrderStatus.WAITING_PAYMENT, OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.WAITING_PAYMENT, OrderStatus.FAILED),
                // CONFIRMED transitions
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
                // PROCESSING transitions
                Arguments.of(OrderStatus.PROCESSING, OrderStatus.SHIPPING),
                Arguments.of(OrderStatus.PROCESSING, OrderStatus.READY_FOR_PICKUP),
                Arguments.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
                // SHIPPING transitions
                Arguments.of(OrderStatus.SHIPPING, OrderStatus.DELIVERED),
                Arguments.of(OrderStatus.SHIPPING, OrderStatus.DELAYED),
                Arguments.of(OrderStatus.SHIPPING, OrderStatus.RETURNING),
                // DELAYED transitions
                Arguments.of(OrderStatus.DELAYED, OrderStatus.SHIPPING),
                Arguments.of(OrderStatus.DELAYED, OrderStatus.RETURNING),
                // READY_FOR_PICKUP transitions
                Arguments.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.DELIVERED),
                Arguments.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.RETURNING),
                // DELIVERED transitions
                Arguments.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED),
                Arguments.of(OrderStatus.DELIVERED, OrderStatus.RETURNING),
                // RETURNING → RETURNED
                Arguments.of(OrderStatus.RETURNING, OrderStatus.RETURNED),
                // RETURNED → REFUNDED
                Arguments.of(OrderStatus.RETURNED, OrderStatus.REFUNDED)
        );
    }

    static Stream<Arguments> invalidTransitionsProvider() {
        return Stream.of(
                // SKIP (can't transition to current state - tested separately)
                // PENDING invalid targets
                Arguments.of(OrderStatus.PENDING, OrderStatus.PROCESSING),
                Arguments.of(OrderStatus.PENDING, OrderStatus.SHIPPING),
                Arguments.of(OrderStatus.PENDING, OrderStatus.DELAYED),
                Arguments.of(OrderStatus.PENDING, OrderStatus.READY_FOR_PICKUP),
                Arguments.of(OrderStatus.PENDING, OrderStatus.DELIVERED),
                Arguments.of(OrderStatus.PENDING, OrderStatus.COMPLETED),
                Arguments.of(OrderStatus.PENDING, OrderStatus.FAILED),
                Arguments.of(OrderStatus.PENDING, OrderStatus.RETURNING),
                Arguments.of(OrderStatus.PENDING, OrderStatus.RETURNED),
                Arguments.of(OrderStatus.PENDING, OrderStatus.REFUNDED),
                // WAITING_PAYMENT invalid targets
                Arguments.of(OrderStatus.WAITING_PAYMENT, OrderStatus.PROCESSING),
                Arguments.of(OrderStatus.WAITING_PAYMENT, OrderStatus.SHIPPING),
                Arguments.of(OrderStatus.WAITING_PAYMENT, OrderStatus.DELIVERED),
                Arguments.of(OrderStatus.WAITING_PAYMENT, OrderStatus.COMPLETED),
                Arguments.of(OrderStatus.WAITING_PAYMENT, OrderStatus.RETURNING),
                // CONFIRMED invalid targets
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPING),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.DELIVERED),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.WAITING_PAYMENT),
                Arguments.of(OrderStatus.CONFIRMED, OrderStatus.FAILED),
                // PROCESSING invalid targets
                Arguments.of(OrderStatus.PROCESSING, OrderStatus.COMPLETED),
                Arguments.of(OrderStatus.PROCESSING, OrderStatus.DELIVERED),
                Arguments.of(OrderStatus.PROCESSING, OrderStatus.DELAYED),
                Arguments.of(OrderStatus.PROCESSING, OrderStatus.WAITING_PAYMENT),
                Arguments.of(OrderStatus.PROCESSING, OrderStatus.FAILED),
                // Terminal can't go anywhere (except same state - not allowed)
                Arguments.of(OrderStatus.COMPLETED, OrderStatus.PENDING),
                Arguments.of(OrderStatus.CANCELLED, OrderStatus.PENDING),
                Arguments.of(OrderStatus.FAILED, OrderStatus.PENDING),
                Arguments.of(OrderStatus.REFUNDED, OrderStatus.PENDING)
        );
    }

    static Stream<Arguments> terminalStatesProvider() {
        return Stream.of(
                Arguments.of(OrderStatus.COMPLETED),
                Arguments.of(OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.FAILED),
                Arguments.of(OrderStatus.REFUNDED)
        );
    }

    static Stream<Arguments> nonTerminalStatesProvider() {
        return Stream.of(
                Arguments.of(OrderStatus.PENDING),
                Arguments.of(OrderStatus.WAITING_PAYMENT),
                Arguments.of(OrderStatus.CONFIRMED),
                Arguments.of(OrderStatus.PROCESSING),
                Arguments.of(OrderStatus.SHIPPING),
                Arguments.of(OrderStatus.DELAYED),
                Arguments.of(OrderStatus.READY_FOR_PICKUP),
                Arguments.of(OrderStatus.DELIVERED),
                Arguments.of(OrderStatus.RETURNING),
                Arguments.of(OrderStatus.RETURNED)
        );
    }
}
