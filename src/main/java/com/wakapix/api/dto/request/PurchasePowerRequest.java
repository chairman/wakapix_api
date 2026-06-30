package com.wakapix.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PurchasePowerRequest {

    @NotNull(message = "算力数量不能为空")
    @Min(value = 1, message = "算力数量必须大于0")
    private Integer powerAmount;

    @NotNull(message = "支付方式不能为空")
    private String paymentMethod;
}
