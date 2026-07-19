package com.anno.ERP_SpringBoot_Experiment.web.rest;

import com.anno.ERP_SpringBoot_Experiment.service.dto.ShoppingCartDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CartItemRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/cart")
public interface ShoppingCartController {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    Response<ShoppingCartDto> getCart();

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.OK)
    Response<ShoppingCartDto> addToCart(@Valid @RequestBody List<CartItemRequest> items);

    @DeleteMapping("/remove")
    @ResponseStatus(HttpStatus.OK)
    Response<ShoppingCartDto> removeFromCart(@RequestBody List<String> skus);

    @DeleteMapping("/clear")
    @ResponseStatus(HttpStatus.OK)
    Response<ShoppingCartDto> clearCart();
}
