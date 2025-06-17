package com.ajiang.userservice.mapper;

import com.ajiang.userservice.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问层接口
 * 用于操作用户数据
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}