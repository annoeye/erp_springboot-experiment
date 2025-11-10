package com.anno.ERP_SpringBoot_Experiment.service.Merchandise;

import com.anno.ERP_SpringBoot_Experiment.mapper.BookingMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Booking;
import com.anno.ERP_SpringBoot_Experiment.model.enums.BookingStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.BookingRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.BookingDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.BookingRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iBooking;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService implements iBooking {

    private final SecurityUtil securityUtil;
    private final AttributesRepository attributesRepository;
    private final BookingMapper bookingMapper;
    private final BookingRepository bookingRepository;

    @Override
    public Response<BookingDto> createBooking(BookingRequest request) {

        final var booking = new Booking();
        final var audit = new AuditInfo();

        List<String> attributesIds = request.getProducts().stream()
                .map(ProductQuantity::getAttributesId).toList();

        Map<String, Attributes> attributesMap = attributesRepository.findAllById(attributesIds.stream().map(UUID::fromString).toList())
                .stream().collect(Collectors.toMap(a -> a.getId().toString(), Function.identity()));

        if (attributesMap.size() != attributesIds.size()) {
            throw new BusinessException("Một hoặc nhiều thuộc tính sản phẩm không tồn tại.");
        }

        double totalPrice = request.getProducts().stream()
                        .mapToDouble(p -> {
                            Attributes attributes = attributesMap.get(p.getAttributesId());
                            return attributes.getSalePrice() > 0 ? attributes.getSalePrice() : attributes.getPrice() * p.getQuantity();
                        }).sum();

        audit.setCreatedAt(LocalDateTime.now());
        audit.setCreatedBy(securityUtil.getCurrentUsername());

        booking.setName(request.getName());
        booking.setAuditInfo(audit);
        booking.setProducts(request.getProducts());
        booking.setCustomerName(request.getCustomerName());
        booking.setTotalPrice(totalPrice);
        booking.setPhoneNumber(request.getPhoneNumber());
        booking.setNote(request.getNote());
        booking.setAddress(request.getAddress());
        booking.setStatus(BookingStatus.PENDING);

        log.info("Đã tạo đơn hàng mới với ID: {}", booking.getId());
        return Response.ok(
                bookingMapper.toDto(bookingRepository.save(booking))
        );
    }

    // Test Create. Thiếu RUD. Sau đó làm phần thanh toán ngân hàng. Và cuối cùng ghép nó vào FE
}
