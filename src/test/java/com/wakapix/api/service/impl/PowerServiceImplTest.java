package com.wakapix.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PowerServiceImplTest {

    @Mock
    private PackageMapper packageMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserPackageMapper userPackageMapper;

    @InjectMocks
    private PowerServiceImpl powerService;

    private User testUser;
    private Package testPackage;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .availablePower(1000)
                .status(1)
                .build();

        testPackage = Package.builder()
                .id(1L)
                .name("基础版")
                .price(new BigDecimal("69.00"))
                .monthlyQuota(3500)
                .validDays(30)
                .status(1)
                .build();
    }

    @Test
    @DisplayName("获取套餐列表 - 成功")
    void getPackages_Success() {
        List<Package> packages = Arrays.asList(testPackage);
        when(packageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(packages);

        List<PackageResponse> result = powerService.getPackages(null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("基础版", result.get(0).getName());
        assertEquals(new BigDecimal("69.00"), result.get(0).getPrice());
    }

    @Test
    @DisplayName("获取套餐列表 - 按类型筛选")
    void getPackages_WithTypeFilter() {
        List<Package> packages = Arrays.asList(testPackage);
        when(packageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(packages);

        List<PackageResponse> result = powerService.getPackages("PERSONAL");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(packageMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("创建套餐订单 - 成功")
    void createPackageOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPackageId(1L);
        request.setPaymentMethod("WECHAT");

        when(packageMapper.selectById(1L)).thenReturn(testPackage);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);

        OrderResponse result = powerService.createPackageOrder(1L, request);

        assertNotNull(result);
        assertEquals("PACKAGE", result.getOrderType());
        assertEquals("WECHAT", result.getPaymentMethod());
        assertEquals("PENDING", result.getStatus());
        assertEquals(3500, result.getCalculationPower());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(1L, savedOrder.getUserId());
        assertEquals("基础版", savedOrder.getPackageName());
    }

    @Test
    @DisplayName("创建套餐订单 - 套餐不存在")
    void createPackageOrder_PackageNotFound() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPackageId(999L);
        request.setPaymentMethod("WECHAT");

        when(packageMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.createPackageOrder(1L, request));

        assertEquals("套餐不存在或已下架", exception.getMessage());
    }

    @Test
    @DisplayName("创建套餐订单 - 套餐已下架")
    void createPackageOrder_PackageDisabled() {
        testPackage.setStatus(0);
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPackageId(1L);
        request.setPaymentMethod("WECHAT");

        when(packageMapper.selectById(1L)).thenReturn(testPackage);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.createPackageOrder(1L, request));

        assertEquals("套餐不存在或已下架", exception.getMessage());
    }

    @Test
    @DisplayName("创建算力订单 - 成功")
    void createPowerOrder_Success() {
        PurchasePowerRequest request = new PurchasePowerRequest();
        request.setPowerAmount(1000);
        request.setPaymentMethod("ALIPAY");

        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);

        OrderResponse result = powerService.createPowerOrder(1L, request);

        assertNotNull(result);
        assertEquals("POWER", result.getOrderType());
        assertEquals("ALIPAY", result.getPaymentMethod());
        assertEquals(1000, result.getCalculationPower());
        assertEquals(new BigDecimal("60.00"), result.getPayAmount());
    }

    @Test
    @DisplayName("创建算力订单 - 算力数量为0")
    void createPowerOrder_InvalidAmount() {
        PurchasePowerRequest request = new PurchasePowerRequest();
        request.setPowerAmount(0);
        request.setPaymentMethod("WECHAT");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.createPowerOrder(1L, request));

        assertEquals("算力数量必须大于0", exception.getMessage());
    }

    @Test
    @DisplayName("创建算力订单 - 用户不存在")
    void createPowerOrder_UserNotFound() {
        PurchasePowerRequest request = new PurchasePowerRequest();
        request.setPowerAmount(1000);
        request.setPaymentMethod("WECHAT");

        when(userMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.createPowerOrder(1L, request));

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取用户算力信息 - 成功")
    void getUserPower_Success() {
        UserPackage userPackage = UserPackage.builder()
                .packageName("基础版")
                .remainingQuota(3000)
                .endTime(LocalDateTime.now().plusDays(30))
                .build();

        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userPackageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userPackage);

        UserPowerResponse result = powerService.getUserPower(1L);

        assertNotNull(result);
        assertEquals(1000, result.getAvailablePower());
        assertEquals("基础版", result.getCurrentPackageName());
        assertEquals(3000, result.getRemainingQuota());
    }

    @Test
    @DisplayName("获取用户算力信息 - 用户不存在")
    void getUserPower_UserNotFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.getUserPower(1L));

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("扣除算力 - 成功")
    void deductPower_Success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        boolean result = powerService.deductPower(1L, 500);

        assertTrue(result);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(500, userCaptor.getValue().getAvailablePower());
    }

    @Test
    @DisplayName("扣除算力 - 算力不足")
    void deductPower_InsufficientPower() {
        when(userMapper.selectById(1L)).thenReturn(testUser);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.deductPower(1L, 2000));

        assertEquals("算力不足", exception.getMessage());
    }

    @Test
    @DisplayName("扣除算力 - 用户不存在")
    void deductPower_UserNotFound() {
        when(userMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.deductPower(1L, 100));

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取微信支付链接 - 成功")
    void getPaymentUrl_Wechat_Success() {
        Order order = Order.builder()
                .id(1L)
                .orderNo("ORD123456")
                .payAmount(new BigDecimal("69.00"))
                .packageName("基础版")
                .build();

        when(orderMapper.selectById(1L)).thenReturn(order);

        String result = powerService.getPaymentUrl(1L, "WECHAT");

        assertNotNull(result);
        assertTrue(result.contains("weixin.qq.com"));
        assertTrue(result.contains("ORD123456"));
    }

    @Test
    @DisplayName("获取支付宝支付链接 - 成功")
    void getPaymentUrl_Alipay_Success() {
        Order order = Order.builder()
                .id(1L)
                .orderNo("ORD123456")
                .payAmount(new BigDecimal("69.00"))
                .packageName("基础版")
                .build();

        when(orderMapper.selectById(1L)).thenReturn(order);

        String result = powerService.getPaymentUrl(1L, "ALIPAY");

        assertNotNull(result);
        assertTrue(result.contains("alipay.com"));
    }

    @Test
    @DisplayName("获取支付链接 - 不支持的支付方式")
    void getPaymentUrl_UnsupportedMethod() {
        Order order = Order.builder()
                .id(1L)
                .orderNo("ORD123456")
                .build();

        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.getPaymentUrl(1L, "PAYPAL"));

        assertEquals("不支持的支付方式", exception.getMessage());
    }

    @Test
    @DisplayName("获取支付链接 - 订单不存在")
    void getPaymentUrl_OrderNotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.getPaymentUrl(999L, "WECHAT"));

        assertEquals("订单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取订单详情 - 成功")
    void getOrderById_Success() {
        Order order = Order.builder()
                .id(1L)
                .orderNo("ORD123456")
                .userId(1L)
                .orderType("PACKAGE")
                .status("PENDING")
                .build();

        when(orderMapper.selectById(1L)).thenReturn(order);

        OrderResponse result = powerService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORD123456", result.getOrderNo());
    }

    @Test
    @DisplayName("获取订单详情 - 订单不存在")
    void getOrderById_NotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.getOrderById(999L));

        assertEquals("订单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取用户订单列表 - 成功")
    void getUserOrders_Success() {
        List<Order> orders = Arrays.asList(
                Order.builder().id(1L).orderNo("ORD001").status("PAID").build(),
                Order.builder().id(2L).orderNo("ORD002").status("PENDING").build()
        );

        when(orderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(orders);

        List<OrderResponse> result = powerService.getUserOrders(1L, null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("处理支付回调 - 首次支付")
    void processPaymentCallback_FirstPayment() {
        Order order = Order.builder()
                .id(1L)
                .orderNo("ORD123456")
                .userId(1L)
                .packageId(1L)
                .orderType("PACKAGE")
                .calculationPower(3500)
                .status("PENDING")
                .build();

        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(packageMapper.selectById(1L)).thenReturn(testPackage);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        powerService.processPaymentCallback("ORD123456", "WECHAT", "TXN123456");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).updateById(orderCaptor.capture());
        Order updatedOrder = orderCaptor.getValue();
        assertEquals("PAID", updatedOrder.getStatus());
        assertEquals("TXN123456", updatedOrder.getTransactionId());
        assertNotNull(updatedOrder.getPayTime());
    }

    @Test
    @DisplayName("处理支付回调 - 重复回调")
    void processPaymentCallback_AlreadyPaid() {
        Order order = Order.builder()
                .id(1L)
                .orderNo("ORD123456")
                .userId(1L)
                .orderType("PACKAGE")
                .calculationPower(3500)
                .status("PAID")
                .build();

        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        powerService.processPaymentCallback("ORD123456", "WECHAT", "TXN123456");

        verify(userMapper, never()).updateById(any(User.class));
        verify(userPackageMapper, never()).insert(any(UserPackage.class));
    }

    @Test
    @DisplayName("处理支付回调 - 订单不存在")
    void processPaymentCallback_OrderNotFound() {
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                powerService.processPaymentCallback("INVALID", "WECHAT", "TXN123456"));

        assertEquals("订单不存在", exception.getMessage());
    }
}
