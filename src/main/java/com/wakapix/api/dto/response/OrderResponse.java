package com.wakapix.api.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;

    private String orderNo;

    private Long userId;

    private Long packageId;

    private String packageName;

    private String orderType;

    private String paymentMethod;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private Integer calculationPower;

    private String status;

    private LocalDateTime payTime;

    private String transactionId;

    private LocalDateTime createdAt;
}
