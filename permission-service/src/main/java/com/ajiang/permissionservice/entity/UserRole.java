package com.ajiang.permissionservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_roles")
public class UserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer roleId;
}