package com.ajiang.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("users_0")
public class User {

    // 用户ID - 分片键（雪花算法）
    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    private Long userId;

    private String username;

    private String password;

    private String email;

    private String phone;

    private LocalDateTime gmtCreate;
}