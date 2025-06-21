package com.ajiang.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {
    // 数据列表
    private List<T> items;
    // 总记录数
    private long counts;
    // 当前页码
    private long page;
    // 每页记录数
    private long pageSize;

    /**
     * 创建空的分页结果
     *
     * @param page     当前页码
     * @param pageSize 每页记录数
     * @param <T>      数据类型
     * @return 空的分页结果
     */
    public static <T> PageResult<T> empty(Long page, Long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setItems(new ArrayList<>());
        result.setCounts(0L);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }
}
