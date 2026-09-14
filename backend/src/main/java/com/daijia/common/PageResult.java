package com.daijia.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> records = Collections.emptyList();
    private long total;
    private long current;
    private long size;

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records == null ? Collections.emptyList() : records);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        return result;
    }

    public static <T> PageResult<T> empty(long current, long size) {
        return of(Collections.emptyList(), 0L, current, size);
    }
}
