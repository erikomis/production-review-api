package com.client.productionreview.utils;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

class PaginationUtilsTest {

    @Test
    void defaults_whenParamsMissing() {
        var pageable = PaginationUtils.createPageable(null, null, null, null);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertTrue(pageable.getSort().isUnsorted());
    }

    @Test
    void invalidValues_fallBackToDefaults() {
        var pageable = PaginationUtils.createPageable(-1, 0, null, null);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
    }

    @Test
    void pageSize_isCapped() {
        assertEquals(100, PaginationUtils.createPageable(0, 1_000_000, null, null).getPageSize());
    }

    @Test
    void sortByProperty_withDirection() {
        var pageable = PaginationUtils.createPageable(2, 5, "name", "DESC");

        assertEquals(2, pageable.getPageNumber());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("name").getDirection());
    }

    @Test
    void sortByProperty_defaultsToAsc() {
        var pageable = PaginationUtils.createPageable(0, 5, "name", null);

        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("name").getDirection());
    }
}
