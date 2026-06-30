package com.wakapix.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "账号登录请求")
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}