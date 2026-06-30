package com.wakapix.api.controller;

import com.wakapix.api.dto.request.CreateOrderRequest;
import com.wakapix.api.dto.request.PurchasePowerRequest;
import com.wakapix.api.dto.response.ApiResponse;
import com.wakapix.api.dto.response.OrderResponse;
import com.wakapix.api.dto.response.PackageResponse;
import com.wakapix.api.dto.response.UserPowerResponse;
import com.wakapix.api.service.PowerService;
import com.wakapix.api.service.impl.PowerServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/power")
@Tag(name = "算力管理", description = "算力购买、套餐购买、订单管理接口")
public class PowerController {

    private final PowerService powerService;

    public PowerController(PowerService powerService) {
        this.powerService = powerService;
    }

    @GetMapping("/packages")
    @Operation(summary = "获取套餐列表", description = "获取所有可用套餐，支持按类型筛选")
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getPackages(
            @Parameter(description = "套餐类型：PERSONAL(个人版)、TEAM(团队版)、REFILL(加油包)")
            @RequestParam(required = false) String packageType) {
        List<PackageResponse> packages = powerService.getPackages(packageType);
        return ResponseEntity.ok(ApiResponse.success(packages));
    }

    @GetMapping("/user/power")
    @Operation(summary = "获取用户算力信息", description = "获取当前用户的可用算力、已用算力、当前套餐等信息")
    public ResponseEntity<ApiResponse<UserPowerResponse>> getUserPower() {
        Long userId = getCurrentUserId();
        UserPowerResponse response = powerService.getUserPower(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/order/package")
    @Operation(summary = "创建套餐订单", description = "购买套餐，创建订单")
    public ResponseEntity<ApiResponse<OrderResponse>> createPackageOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        Long userId = getCurrentUserId();
        OrderResponse order = powerService.createPackageOrder(userId, request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/order/power")
    @Operation(summary = "创建算力订单", description = "直接购买算力，创建订单")
    public ResponseEntity<ApiResponse<OrderResponse>> createPowerOrder(
            @Valid @RequestBody PurchasePowerRequest request) {
        Long userId = getCurrentUserId();
        OrderResponse order = powerService.createPowerOrder(userId, request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取订单详情", description = "根据订单ID获取订单详情")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        OrderResponse order = powerService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/orders")
    @Operation(summary = "获取用户订单列表", description = "获取当前用户的所有订单，支持按类型和状态筛选")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(
            @Parameter(description = "订单类型：PACKAGE(套餐)、POWER(算力)")
            @RequestParam(required = false) String orderType,
            @Parameter(description = "订单状态：PENDING(待支付)、PAID(已支付)、CANCELLED(已取消)")
            @RequestParam(required = false) String status) {
        Long userId = getCurrentUserId();
        List<OrderResponse> orders = powerService.getUserOrders(userId, orderType, status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/payment/{orderId}")
    @Operation(summary = "获取支付链接", description = "根据订单ID和支付方式获取支付链接")
    public ResponseEntity<ApiResponse<String>> getPaymentUrl(
            @Parameter(description = "订单ID") @PathVariable Long orderId,
            @Parameter(description = "支付方式：WECHAT(微信支付)、ALIPAY(支付宝)")
            @RequestParam String paymentMethod) {
        String paymentUrl = powerService.getPaymentUrl(orderId, paymentMethod);
        return ResponseEntity.ok(ApiResponse.success(paymentUrl));
    }

    @PostMapping("/payment/callback")
    @Operation(summary = "支付回调", description = "支付成功后的回调接口")
    public ResponseEntity<ApiResponse<String>> paymentCallback(
            @Parameter(description = "订单号") @RequestParam String orderNo,
            @Parameter(description = "支付方式") @RequestParam String paymentMethod,
            @Parameter(description = "交易号") @RequestParam String transactionId) {
        ((PowerServiceImpl) powerService).processPaymentCallback(orderNo, paymentMethod, transactionId);
        return ResponseEntity.ok(ApiResponse.success("支付成功"));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getName());
    }
}
