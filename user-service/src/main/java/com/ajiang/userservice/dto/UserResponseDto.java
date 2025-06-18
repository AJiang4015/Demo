package com.ajiang.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ajiang.userservice.entity.User;

import java.time.LocalDateTime;

/**
 * 用户信息响应数据传输对象
 * 用于返回用户信息，不包含敏感信息如密码
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long userId;

    private String username;

    private String email;


    private String phone;


    private String roleCode;

    private LocalDateTime gmtCreate;

    /**
     * 从User实体转换为UserResponseDTO
     *
     * @param user     用户实体
     * @param roleCode 角色代码
     * @return UserResponseDTO
     */
    public static UserResponseDto fromUser(User user, String roleCode) {
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleCode(roleCode)
                .gmtCreate(user.getGmtCreate())
                .build();
    }
}