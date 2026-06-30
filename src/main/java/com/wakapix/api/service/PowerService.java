package com.wakapix.api.service;

import com.wakapix.api.dto.request.CreateOrderRequest;
import com.wakapix.api.dto.request.PurchasePowerRequest;
import com.wakapix.api.dto.response.OrderResponse;
import com.wakapix.api.dto.response.PackageResponse;
import com.wakapix.api.dto.response.UserPowerResponse;

import java.util.List;

public interface PowerService {

    List<PackageResponse> getPackages(String packageType);

    OrderResponse createPackageOrder(Long userId, CreateOrderRequest request);

    OrderResponse createPowerOrder(Long userId, PurchasePowerRequest request);

    OrderResponse getOrderById(Long orderId);

    List<OrderResponse> getUserOrders(Long userId, String orderType, String status);

    boolean deductPower(Long userId, Integer amount);

    UserPowerResponse getUserPower(Long userId);

    String getPaymentUrl(Long orderId, String paymentMethod);
}
