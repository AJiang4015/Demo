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
@TableName("roles")
public class Role {

    /**
     * 角色ID
     * 1:超级管理员 2:普通用户 3:管理员
     */
    @TableId(value = "role_id", type = IdType.INPUT)
    private Integer roleId;

    private String roleCode;

}