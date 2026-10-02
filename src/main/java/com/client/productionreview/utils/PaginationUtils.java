package com.client.productionreview.utils;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    private PaginationUtils() {
    }

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SORT_DIRECTION = "ASC";

    public static Pageable createPageable (Integer page, Integer size, String sortBy , String sortDirection) {
        int pageNumber = (page == null || page < 0) ? DEFAULT_PAGE : page;
        int pageSize = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Sort.Direction direction = sortDirection == null ? Sort.Direction.fromString(DEFAULT_SORT_DIRECTION) : Sort.Direction.fromString(sortDirection);
        Sort sort = sortBy == null ? Sort.unsorted() : Sort.by(direction, sortBy);

        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
