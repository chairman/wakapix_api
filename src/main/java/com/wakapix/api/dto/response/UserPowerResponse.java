package com.wakapix.api.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPowerResponse {

    private Integer availablePower;

    private Integer usedPower;

    private String currentPackageName;

    private Integer remainingQuota;

    private LocalDateTime packageEndTime;
}
