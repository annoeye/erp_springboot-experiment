package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.BookingDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.BookingRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;

public interface iBooking {
    Response<BookingDto> createBooking(BookingRequest request);
}
