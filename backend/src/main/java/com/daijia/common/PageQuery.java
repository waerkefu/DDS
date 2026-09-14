package com.daijia.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询基础参数
 */
@Data
public class PageQuery implements Serializable {

    private static final long MAX_PAGE_SIZE = 100L;

    /** 页码，从 1 开始 */
    private Long pageNum = 1L;
    /** 每页条数 */
    private Long pageSize = 10L;

    public long safePageNum() {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    public long safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
