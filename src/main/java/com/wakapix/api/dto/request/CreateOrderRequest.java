package com.wakapix.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull(message = "套餐ID不能为空")
    private Long packageId;

    @NotNull(message = "支付方式不能为空")
    private String paymentMethod;
}
