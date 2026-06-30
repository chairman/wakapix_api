package com.wakapix.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wakapix.api.config.JwtAuthenticationFilter;
import com.wakapix.api.dto.request.CreateOrderRequest;
import com.wakapix.api.dto.request.PurchasePowerRequest;
import com.wakapix.api.dto.response.OrderResponse;
import com.wakapix.api.dto.response.PackageResponse;
import com.wakapix.api.dto.response.UserPowerResponse;
import com.wakapix.api.service.PowerService;
import com.wakapix.api.util.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PowerController.class)
@AutoConfigureMockMvc(addFilters = false)
class PowerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PowerService powerService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username = "1")
    @DisplayName("获取套餐列表 - 成功")
    void getPackages_Success() throws Exception {
        List<PackageResponse> packages = Arrays.asList(
                PackageResponse.builder()
                        .id(1L)
                        .name("基础版")
                        .price(new BigDecimal("69.00"))
                        .monthlyQuota(3500)
                        .build(),
                PackageResponse.builder()
                        .id(2L)
                        .name("高级版")
                        .price(new BigDecimal("299.00"))
                        .monthlyQuota(21000)
                        .build()
        );

        when(powerService.getPackages(isNull())).thenReturn(packages);

        mockMvc.perform(get("/api/power/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("基础版"))
                .andExpect(jsonPath("$.data[1].name").value("高级版"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("获取套餐列表 - 按类型筛选")
    void getPackages_WithTypeFilter() throws Exception {
        List<PackageResponse> packages = Arrays.asList(
                PackageResponse.builder()
                        .id(1L)
                        .name("基础版")
                        .packageType("PERSONAL")
                        .build()
        );

        when(powerService.getPackages("PERSONAL")).thenReturn(packages);

        mockMvc.perform(get("/api/power/packages")
                        .param("packageType", "PERSONAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].packageType").value("PERSONAL"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("获取用户算力信息 - 成功")
    void getUserPower_Success() throws Exception {
        UserPowerResponse powerResponse = UserPowerResponse.builder()
                .availablePower(1000)
                .usedPower(500)
                .currentPackageName("基础版")
                .remainingQuota(3000)
                .packageEndTime(LocalDateTime.now().plusDays(30))
                .build();

        when(powerService.getUserPower(1L)).thenReturn(powerResponse);

        mockMvc.perform(get("/api/power/user/power"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.availablePower").value(1000))
                .andExpect(jsonPath("$.data.usedPower").value(500))
                .andExpect(jsonPath("$.data.currentPackageName").value("基础版"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("创建套餐订单 - 成功")
    void createPackageOrder_Success() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPackageId(1L);
        request.setPaymentMethod("WECHAT");

        OrderResponse orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNo("ORD123456")
                .orderType("PACKAGE")
                .paymentMethod("WECHAT")
                .totalAmount(new BigDecimal("69.00"))
                .payAmount(new BigDecimal("69.00"))
                .calculationPower(3500)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        when(powerService.createPackageOrder(eq(1L), any(CreateOrderRequest.class)))
                .thenReturn(orderResponse);

        mockMvc.perform(post("/api/power/order/package")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("ORD123456"))
                .andExpect(jsonPath("$.data.orderType").value("PACKAGE"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("创建套餐订单 - 缺少套餐ID")
    void createPackageOrder_MissingPackageId() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPaymentMethod("WECHAT");

        mockMvc.perform(post("/api/power/order/package")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("创建算力订单 - 成功")
    void createPowerOrder_Success() throws Exception {
        PurchasePowerRequest request = new PurchasePowerRequest();
        request.setPowerAmount(5000);
        request.setPaymentMethod("ALIPAY");

        OrderResponse orderResponse = OrderResponse.builder()
                .id(2L)
                .orderNo("ORD789012")
                .orderType("POWER")
                .paymentMethod("ALIPAY")
                .totalAmount(new BigDecimal("300.00"))
                .payAmount(new BigDecimal("300.00"))
                .calculationPower(5000)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        when(powerService.createPowerOrder(eq(1L), any(PurchasePowerRequest.class)))
                .thenReturn(orderResponse);

        mockMvc.perform(post("/api/power/order/power")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("ORD789012"))
                .andExpect(jsonPath("$.data.orderType").value("POWER"))
                .andExpect(jsonPath("$.data.calculationPower").value(5000));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("创建算力订单 - 算力数量为0")
    void createPowerOrder_InvalidAmount() throws Exception {
        PurchasePowerRequest request = new PurchasePowerRequest();
        request.setPowerAmount(0);
        request.setPaymentMethod("WECHAT");

        mockMvc.perform(post("/api/power/order/power")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("获取订单详情 - 成功")
    void getOrderById_Success() throws Exception {
        OrderResponse orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNo("ORD123456")
                .orderType("PACKAGE")
                .status("PAID")
                .payTime(LocalDateTime.now())
                .build();

        when(powerService.getOrderById(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/api/power/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderNo").value("ORD123456"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("获取用户订单列表 - 成功")
    void getUserOrders_Success() throws Exception {
        List<OrderResponse> orders = Arrays.asList(
                OrderResponse.builder()
                        .id(1L)
                        .orderNo("ORD001")
                        .status("PAID")
                        .build(),
                OrderResponse.builder()
                        .id(2L)
                        .orderNo("ORD002")
                        .status("PENDING")
                        .build()
        );

        when(powerService.getUserOrders(eq(1L), isNull(), isNull())).thenReturn(orders);

        mockMvc.perform(get("/api/power/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("获取用户订单列表 - 按状态筛选")
    void getUserOrders_WithStatusFilter() throws Exception {
        List<OrderResponse> orders = Arrays.asList(
                OrderResponse.builder()
                        .id(1L)
                        .orderNo("ORD001")
                        .status("PAID")
                        .build()
        );

        when(powerService.getUserOrders(eq(1L), isNull(), eq("PAID"))).thenReturn(orders);

        mockMvc.perform(get("/api/power/orders")
                        .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].status").value("PAID"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("获取微信支付链接 - 成功")
    void getPaymentUrl_Wechat_Success() throws Exception {
        String paymentUrl = "https://pay.weixin.qq.com/wxpay/pay.action?orderNo=ORD123456";

        when(powerService.getPaymentUrl(1L, "WECHAT")).thenReturn(paymentUrl);

        mockMvc.perform(get("/api/power/payment/1")
                        .param("paymentMethod", "WECHAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(paymentUrl));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("获取支付宝支付链接 - 成功")
    void getPaymentUrl_Alipay_Success() throws Exception {
        String paymentUrl = "https://openapi.alipay.com/gateway.do?method=alipay.trade.page.pay";

        when(powerService.getPaymentUrl(1L, "ALIPAY")).thenReturn(paymentUrl);

        mockMvc.perform(get("/api/power/payment/1")
                        .param("paymentMethod", "ALIPAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(paymentUrl));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("创建套餐订单 - 缺少支付方式")
    void createPackageOrder_MissingPaymentMethod() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setPackageId(1L);

        mockMvc.perform(post("/api/power/order/package")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
