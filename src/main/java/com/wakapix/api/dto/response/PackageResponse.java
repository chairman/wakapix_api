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
public class PackageResponse {

    private Long id;

    private String name;

    private String description;

    private String packageType;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer monthlyQuota;

    private Integer validDays;

    private Integer priority;

    private Integer status;

    private LocalDateTime createdAt;
}
