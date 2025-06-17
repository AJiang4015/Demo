package com.ajiang.permissionservice.mapper;

import com.ajiang.permissionservice.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

}
