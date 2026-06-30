package com.wakapix.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "用户信息")
    private UserInfo user;

    @Data
    @Builder
    @Schema(description = "用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID", example = "1")
        private Long id;
        @Schema(description = "用户名", example = "zhangsan")
        private String username;
        @Schema(description = "昵称", example = "张三")
        private String nickname;
        @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
        private String avatar;
        @Schema(description = "手机号", example = "13800138000")
        private String phone;
        @Schema(description = "邮箱", example = "zhangsan@example.com")
        private String email;
    }
}