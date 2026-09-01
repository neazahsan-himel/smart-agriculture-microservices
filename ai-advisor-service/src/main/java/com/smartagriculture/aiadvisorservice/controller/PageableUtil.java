package com.smartagriculture.aiadvisorservice.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class PageableUtil {

    private static final int MAX_PAGE_SIZE = 100;

    private PageableUtil() {}

    static Pageable build(int page, int size, String sortBy, String sortDir) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return PageRequest.of(page, safeSize, sort);
    }
}
