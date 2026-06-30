package com.wakapix.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wakapix.api.dto.request.CreateOrderRequest;
import com.wakapix.api.dto.request.PurchasePowerRequest;
import com.wakapix.api.dto.response.OrderResponse;
import com.wakapix.api.dto.response.PackageResponse;
import com.wakapix.api.dto.response.UserPowerResponse;
import com.wakapix.api.entity.Order;
import com.wakapix.api.entity.Package;
import com.wakapix.api.entity.User;
import com.wakapix.api.entity.UserPackage;
import com.wakapix.api.exception.BusinessException;
import com.wakapix.api.mapper.OrderMapper;
import com.wakapix.api.mapper.PackageMapper;
import com.wakapix.api.mapper.UserMapper;
import com.wakapix.api.mapper.UserPackageMapper;
import com.wakapix.api.service.PowerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PowerServiceImpl implements PowerService {

    private final PackageMapper packageMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final UserPackageMapper userPackageMapper;

    private static final BigDecimal POWER_PRICE = new BigDecimal("0.06");

    public PowerServiceImpl(PackageMapper packageMapper,
                            OrderMapper orderMapper,
                            UserMapper userMapper,
                            UserPackageMapper userPackageMapper) {
        this.packageMapper = packageMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.userPackageMapper = userPackageMapper;
    }

    @Override
    public List<PackageResponse> getPackages(String packageType) {
        LambdaQueryWrapper<Package> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Package::getStatus, 1);
        if (packageType != null && !packageType.isEmpty()) {
            wrapper.eq(Package::getPackageType, packageType);
        }
        wrapper.orderByAsc(Package::getPriority);

        return packageMapper.selectList(wrapper).stream()
                .map(this::convertToPackageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse createPackageOrder(Long userId, CreateOrderRequest request) {
        Package pkg = packageMapper.selectById(request.getPackageId());
        if (pkg == null || pkg.getStatus() != 1) {
            throw new BusinessException(400, "套餐不存在或已下架");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        String orderNo = generateOrderNo();

        Order order = Order.builder()
                .orderNo(orderNo)
                .userId(userId)
                .packageId(pkg.getId())
                .packageName(pkg.getName())
                .orderType("PACKAGE")
                .paymentMethod(request.getPaymentMethod())
                .totalAmount(pkg.getPrice())
                .payAmount(pkg.getPrice())
                .calculationPower(pkg.getMonthlyQuota())
                .status("PENDING")
                .build();

        orderMapper.insert(order);

        return convertToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse createPowerOrder(Long userId, PurchasePowerRequest request) {
        if (request.getPowerAmount() <= 0) {
            throw new BusinessException(400, "算力数量必须大于0");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        BigDecimal totalAmount = POWER_PRICE.multiply(new BigDecimal(request.getPowerAmount()));

        String orderNo = generateOrderNo();

        Order order = Order.builder()
                .orderNo(orderNo)
                .userId(userId)
                .orderType("POWER")
                .paymentMethod(request.getPaymentMethod())
                .totalAmount(totalAmount)
                .payAmount(totalAmount)
                .calculationPower(request.getPowerAmount())
                .status("PENDING")
                .build();

        orderMapper.insert(order);

        return convertToOrderResponse(order);
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }
        return convertToOrderResponse(order);
    }

    @Override
    public List<OrderResponse> getUserOrders(Long userId, String orderType, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (orderType != null && !orderType.isEmpty()) {
            wrapper.eq(Order::getOrderType, orderType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        return orderMapper.selectList(wrapper).stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean deductPower(Long userId, Integer amount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        if (user.getAvailablePower() == null || user.getAvailablePower() < amount) {
            throw new BusinessException(400, "算力不足");
        }

        user.setAvailablePower(user.getAvailablePower() - amount);
        userMapper.updateById(user);

        return true;
    }

    @Override
    public UserPowerResponse getUserPower(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        LambdaQueryWrapper<UserPackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPackage::getUserId, userId);
        wrapper.eq(UserPackage::getEndTime, null).or().gt(UserPackage::getEndTime, LocalDateTime.now());
        wrapper.orderByDesc(UserPackage::getCreatedAt);

        UserPackage userPackage = userPackageMapper.selectOne(wrapper);

        UserPowerResponse.UserPowerResponseBuilder builder = UserPowerResponse.builder()
                .availablePower(user.getAvailablePower() != null ? user.getAvailablePower() : 0);

        if (userPackage != null) {
            builder.currentPackageName(userPackage.getPackageName())
                    .remainingQuota(userPackage.getRemainingQuota())
                    .packageEndTime(userPackage.getEndTime());
        }

        return builder.build();
    }

    @Override
    public String getPaymentUrl(Long orderId, String paymentMethod) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }

        if ("WECHAT".equalsIgnoreCase(paymentMethod)) {
            return generateWechatPaymentUrl(order);
        } else if ("ALIPAY".equalsIgnoreCase(paymentMethod)) {
            return generateAlipayPaymentUrl(order);
        } else {
            throw new BusinessException(400, "不支持的支付方式");
        }
    }

    public void processPaymentCallback(String orderNo, String paymentMethod, String transactionId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        Order order = orderMapper.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(400, "订单不存在");
        }

        if ("PAID".equals(order.getStatus())) {
            return;
        }

        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        order.setTransactionId(transactionId);
        orderMapper.updateById(order);

        addPowerToUser(order.getUserId(), order.getCalculationPower());

        if ("PACKAGE".equals(order.getOrderType())) {
            createUserPackage(order);
        }
    }

    private void addPowerToUser(Long userId, Integer powerAmount) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setAvailablePower((user.getAvailablePower() != null ? user.getAvailablePower() : 0) + powerAmount);
            userMapper.updateById(user);
        }
    }

    private void createUserPackage(Order order) {
        Package pkg = packageMapper.selectById(order.getPackageId());
        if (pkg != null) {
            LocalDateTime endTime = pkg.getValidDays() != null && pkg.getValidDays() > 0
                    ? LocalDateTime.now().plusDays(pkg.getValidDays())
                    : null;

            UserPackage userPackage = UserPackage.builder()
                    .userId(order.getUserId())
                    .packageId(pkg.getId())
                    .packageName(pkg.getName())
                    .remainingQuota(pkg.getMonthlyQuota())
                    .totalQuota(pkg.getMonthlyQuota())
                    .startTime(LocalDateTime.now())
                    .endTime(endTime)
                    .isAutoRenew(0)
                    .build();

            userPackageMapper.insert(userPackage);
        }
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateWechatPaymentUrl(Order order) {
        return "https://pay.weixin.qq.com/wxpay/pay.action?orderNo=" + order.getOrderNo()
                + "&amount=" + order.getPayAmount()
                + "&description=" + order.getPackageName();
    }

    private String generateAlipayPaymentUrl(Order order) {
        return "https://openapi.alipay.com/gateway.do?method=alipay.trade.page.pay&orderNo=" + order.getOrderNo()
                + "&amount=" + order.getPayAmount()
                + "&description=" + order.getPackageName();
    }

    private PackageResponse convertToPackageResponse(Package pkg) {
        return PackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .packageType(pkg.getPackageType())
                .price(pkg.getPrice())
                .originalPrice(pkg.getOriginalPrice())
                .monthlyQuota(pkg.getMonthlyQuota())
                .validDays(pkg.getValidDays())
                .priority(pkg.getPriority())
                .status(pkg.getStatus())
                .createdAt(pkg.getCreatedAt())
                .build();
    }

    private OrderResponse convertToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .packageId(order.getPackageId())
                .packageName(order.getPackageName())
                .orderType(order.getOrderType())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .payAmount(order.getPayAmount())
                .calculationPower(order.getCalculationPower())
                .status(order.getStatus())
                .payTime(order.getPayTime())
                .transactionId(order.getTransactionId())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
